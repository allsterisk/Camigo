package com.camigo.dispatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.config.DispatchProperties;
import com.camigo.dto.DispatchRejectDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryAssignment;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryAssignmentMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.vo.DispatchAssignmentDetailVO;
import com.camigo.vo.DispatchOfferVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** 智能派单的候选生成、限时确认、递补、轮次和并发控制。 */
@Service
public class IntelligentDispatchService {

    private static final int BATCH_SIZE = 100;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String COURIER_ROLE = "COURIER";

    private final DeliveryOrderMapper orderMapper;
    private final DeliveryAssignmentMapper assignmentMapper;
    private final CourierMapper courierMapper;
    private final ParcelMapper parcelMapper;
    private final CourierRoutePlanningService routePlanningService;
    private final DispatchScoringService scoringService;
    private final CourierRejectionCooldownService cooldownService;
    private final RouteVersionService routeVersionService;
    private final ParcelLifecycleService parcelLifecycleService;
    private final DispatchProperties properties;

    public IntelligentDispatchService(DeliveryOrderMapper orderMapper,
                                      DeliveryAssignmentMapper assignmentMapper,
                                      CourierMapper courierMapper,
                                      ParcelMapper parcelMapper,
                                      CourierRoutePlanningService routePlanningService,
                                      DispatchScoringService scoringService,
                                      CourierRejectionCooldownService cooldownService,
                                      RouteVersionService routeVersionService,
                                      ParcelLifecycleService parcelLifecycleService,
                                      DispatchProperties properties) {
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
        this.courierMapper = courierMapper;
        this.parcelMapper = parcelMapper;
        this.routePlanningService = routePlanningService;
        this.scoringService = scoringService;
        this.cooldownService = cooldownService;
        this.routeVersionService = routeVersionService;
        this.parcelLifecycleService = parcelLifecycleService;
        this.properties = properties;
    }

    /** 启动首轮或管理员确认后的下一轮；订单行锁保证同一轮只生成一次。 */
    @Transactional
    public void startDispatch(long orderId) {
        LocalDateTime now = LocalDateTime.now();
        DeliveryOrder order = requireLockedOrder(orderId);
        startDispatchLocked(order, now);
    }

    /** 管理员按包裹启动首轮或人工确认后的下一轮派单。 */
    @Transactional
    public void startDispatchByParcel(long adminUserId, long parcelId) {
        ParcelEntity parcel = parcelMapper.selectByIdForUpdate(parcelId);
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        if (!List.of("SORTING", "WAITING_ASSIGNMENT").contains(parcel.getCurrentStatus())) {
            throw conflict("当前快递不是已到校待配送状态");
        }
        Long orderId = orderMapper.selectLatestIdByParcelId(parcelId);
        if (orderId == null) {
            throw new ResourceNotFoundException("该快递尚无配送任务");
        }
        DeliveryOrder order = requireLockedOrder(orderId);
        if ("SORTING".equals(parcel.getCurrentStatus())) {
            if (!"FAILED".equals(order.getStatus())) {
                throw conflict("当前分拣包裹没有可重新配送的失败任务");
            }
            order = createRetryOrder(order);
            parcelLifecycleService.changeStatus(parcel, "WAITING_ASSIGNMENT", adminUserId,
                    ADMIN_ROLE, "管理员开始重新配送");
        }
        boolean initialRound = "CREATED".equals(order.getStatus());
        boolean awaitingAdmin = "ASSIGNING".equals(order.getStatus())
                && Integer.valueOf(1).equals(order.getManualDispatchRequired());
        if (!initialRound && !awaitingAdmin) {
            throw conflict("当前轮派单尚未结束，无需重复开始配送");
        }
        startDispatchLocked(order, LocalDateTime.now());
    }

    private DeliveryOrder createRetryOrder(DeliveryOrder previous) {
        if (previous.getPickupLocationId() == null || previous.getDestinationLocationId() == null) {
            throw conflict("原配送任务缺少取件站或目的地，不能重新配送");
        }
        DeliveryOrder retry = new DeliveryOrder();
        retry.setOrderNo("DO" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase());
        retry.setParcelId(previous.getParcelId());
        retry.setPickupLocationId(previous.getPickupLocationId());
        retry.setDestinationLocationId(previous.getDestinationLocationId());
        retry.setPriority(previous.getPriority() == null ? "NORMAL" : previous.getPriority());
        retry.setStatus("CREATED");
        retry.setDispatchRound(0);
        retry.setManualDispatchRequired(0);
        if (orderMapper.insert(retry) != 1) {
            throw conflict("重新创建配送任务失败");
        }
        return retry;
    }

    private void startDispatchLocked(DeliveryOrder order, LocalDateTime now) {
        long orderId = order.getId();
        if (order.getAssignedCourierId() != null
                || List.of("ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED", "COMPLETED")
                .contains(order.getStatus())) {
            return;
        }
        if (!List.of("CREATED", "ASSIGNING").contains(order.getStatus())) {
            throw conflict("当前订单状态不能启动智能派单");
        }
        if (order.getPickupLocationId() == null) {
            throw conflict("配送订单缺少取件服务站");
        }

        if ("ASSIGNING".equals(order.getStatus())) {
            Long activeOfferCount = assignmentMapper.selectCount(
                    new LambdaQueryWrapper<DeliveryAssignment>()
                            .eq(DeliveryAssignment::getDeliveryOrderId, orderId)
                            .eq(DeliveryAssignment::getDecision, "OFFERED"));
            if (activeOfferCount != null && activeOfferCount > 0) {
                return;
            }
        }

        int currentRound = order.getDispatchRound() == null ? 0 : order.getDispatchRound();
        int nextRound = currentRound + 1;
        Long existing = assignmentMapper.selectCount(new LambdaQueryWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getDeliveryOrderId, orderId)
                .eq(DeliveryAssignment::getAssignmentRound, nextRound));
        if (existing != null && existing > 0) {
            updateOrderDispatchState(orderId, "ASSIGNING", nextRound, null, false);
            offerNextCandidate(order, nextRound, now);
            return;
        }

        updateOrderDispatchState(orderId, "ASSIGNING", nextRound, null, false);
        order.setStatus("ASSIGNING");
        order.setDispatchRound(nextRound);
        createCandidateSnapshots(order, nextRound, now);
        offerNextCandidate(order, nextRound, now);
    }

    @Transactional(readOnly = true)
    public List<DispatchOfferVO> currentOffers(long userId) {
        return assignmentMapper.selectCurrentOffers(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignmentDetailVO> adminDetails(long orderId) {
        if (orderMapper.selectById(orderId) == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        return assignmentMapper.selectAdminDetails(orderId);
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignmentDetailVO> adminDetailsByParcel(long parcelId) {
        Long orderId = orderMapper.selectLatestIdByParcelId(parcelId);
        if (orderId == null) {
            throw new ResourceNotFoundException("该包裹尚无配送任务");
        }
        return assignmentMapper.selectAdminDetails(orderId);
    }

    @Transactional
    public DeliveryOrder acceptAssignment(long userId, long assignmentId) {
        DeliveryAssignment snapshot = assignmentMapper.selectSnapshotById(assignmentId);
        if (snapshot == null) {
            throw new ResourceNotFoundException("派单邀请不存在");
        }
        DeliveryOrder order = requireLockedOrder(snapshot.getDeliveryOrderId());
        DeliveryAssignment assignment = assignmentMapper.selectByIdForUpdate(assignmentId);
        return acceptLocked(userId, order, assignment);
    }

    /** 兼容旧接单URL，但只允许接受系统当前发给本人的邀请。 */
    @Transactional
    public DeliveryOrder acceptCurrentOffer(long userId, long orderId) {
        DeliveryOrder order = requireLockedOrder(orderId);
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) {
            throw new ResourceNotFoundException("配送员档案不存在");
        }
        DeliveryAssignment assignment = assignmentMapper.selectCurrentOfferForUpdate(
                orderId, courier.getId());
        if (assignment == null) {
            throw conflict("当前订单没有发给该配送员的有效邀请");
        }
        return acceptLocked(userId, order, assignment, courier);
    }

    @Transactional
    public void rejectAssignment(long userId, long assignmentId, DispatchRejectDTO request) {
        validateRejectReason(request);
        DeliveryAssignment snapshot = assignmentMapper.selectSnapshotById(assignmentId);
        if (snapshot == null) {
            throw new ResourceNotFoundException("派单邀请不存在");
        }
        DeliveryOrder order = requireLockedOrder(snapshot.getDeliveryOrderId());
        DeliveryAssignment assignment = assignmentMapper.selectByIdForUpdate(assignmentId);
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null || !courier.getId().equals(assignment.getCourierId())) {
            throw new BusinessException(ErrorCodes.DISPATCH_CONFLICT,
                    "不能响应其他配送员的邀请", HttpStatus.FORBIDDEN);
        }
        LocalDateTime now = LocalDateTime.now();
        requireActiveOffer(assignment, now);
        int updated = assignmentMapper.update(null,
                new LambdaUpdateWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getId, assignmentId)
                        .eq(DeliveryAssignment::getDecision, "OFFERED")
                        .set(DeliveryAssignment::getDecision, "REJECTED")
                        .set(DeliveryAssignment::getResponseTime, now)
                        .set(DeliveryAssignment::getDecidedTime, now)
                        .set(DeliveryAssignment::getRejectReasonCode, request.reasonCode())
                        .set(DeliveryAssignment::getRejectReasonDetail,
                                StringUtils.hasText(request.reasonDetail())
                                        ? request.reasonDetail().trim() : null)
                        .set(DeliveryAssignment::getDecisionReason, "配送员主动拒绝"));
        if (updated != 1) {
            throw conflict("派单邀请已被其他请求处理");
        }
        offerNextCandidate(order, assignment.getAssignmentRound(), now);
        afterCommit(() -> cooldownService.recordRejection(courier.getId(), now));
    }

    public List<Long> expiredOfferIds() {
        return assignmentMapper.selectExpiredOfferIds(LocalDateTime.now(), BATCH_SIZE);
    }

    @Transactional
    public void expireOffer(long assignmentId) {
        DeliveryAssignment snapshot = assignmentMapper.selectSnapshotById(assignmentId);
        if (snapshot == null) {
            return;
        }
        DeliveryOrder order = requireLockedOrder(snapshot.getDeliveryOrderId());
        DeliveryAssignment assignment = assignmentMapper.selectByIdForUpdate(assignmentId);
        LocalDateTime now = LocalDateTime.now();
        if (!"OFFERED".equals(assignment.getDecision())
                || assignment.getOfferExpireTime() == null
                || assignment.getOfferExpireTime().isAfter(now)) {
            return;
        }
        assignmentMapper.update(null, new LambdaUpdateWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getId, assignmentId)
                .eq(DeliveryAssignment::getDecision, "OFFERED")
                .set(DeliveryAssignment::getDecision, "EXPIRED")
                .set(DeliveryAssignment::getDecidedTime, now)
                .set(DeliveryAssignment::getDecisionReason, "3分钟内未确认，邀请超时"));
        offerNextCandidate(order, assignment.getAssignmentRound(), now);
    }

    private DeliveryOrder acceptLocked(long userId, DeliveryOrder order,
                                       DeliveryAssignment assignment) {
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) {
            throw new ResourceNotFoundException("配送员档案不存在");
        }
        return acceptLocked(userId, order, assignment, courier);
    }

    private DeliveryOrder acceptLocked(long userId, DeliveryOrder order,
                                       DeliveryAssignment assignment, Courier courier) {
        LocalDateTime now = LocalDateTime.now();
        if (!courier.getId().equals(assignment.getCourierId())) {
            throw new BusinessException(ErrorCodes.DISPATCH_CONFLICT,
                    "不能响应其他配送员的邀请", HttpStatus.FORBIDDEN);
        }
        requireActiveOffer(assignment, now);
        if (!"ASSIGNING".equals(order.getStatus()) || order.getAssignedCourierId() != null) {
            throw conflict("配送订单已被处理");
        }
        ensureCourierEligible(courier);
        long activeCount = orderMapper.countActiveByCourierId(courier.getId());
        if (activeCount >= 8) {
            throw conflict("配送员活动订单已达到上限");
        }

        RoutePlan acceptedPlan = routePlanningService.planInsertion(courier.getId(),
                courier.getCurrentLocationId(), order, now);
        try {
            int assignmentUpdated = assignmentMapper.update(null,
                    new LambdaUpdateWrapper<DeliveryAssignment>()
                            .eq(DeliveryAssignment::getId, assignment.getId())
                            .eq(DeliveryAssignment::getDecision, "OFFERED")
                            .set(DeliveryAssignment::getDecision, "ACCEPTED")
                            .set(DeliveryAssignment::getResponseTime, now)
                            .set(DeliveryAssignment::getDecidedTime, now)
                            .set(DeliveryAssignment::getDecisionReason,
                                    "综合得分最高且配送员在有效期内接受"));
            if (assignmentUpdated != 1) {
                throw conflict("派单邀请已被其他请求处理");
            }
        } catch (DuplicateKeyException ex) {
            throw conflict("该订单已有配送员接受");
        }

        int orderUpdated = orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, "ASSIGNING")
                .isNull(DeliveryOrder::getAssignedCourierId)
                .set(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "ACCEPTED")
                .set(DeliveryOrder::getAcceptedTime, now)
                .set(DeliveryOrder::getNextDispatchTime, null)
                .set(DeliveryOrder::getManualDispatchRequired, 0));
        if (orderUpdated != 1) {
            throw conflict("配送订单已被其他请求处理");
        }
        order.setAssignedCourierId(courier.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(now);

        assignmentMapper.update(null, new LambdaUpdateWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                .ne(DeliveryAssignment::getId, assignment.getId())
                .eq(DeliveryAssignment::getDecision, "CANDIDATE")
                .set(DeliveryAssignment::getDecision, "SKIPPED")
                .set(DeliveryAssignment::getDecidedTime, now)
                .set(DeliveryAssignment::getDecisionReason, "其他候选人已接受订单"));

        routeVersionService.saveAcceptedPlan(courier.getId(), acceptedPlan);
        ParcelEntity parcel = parcelMapper.selectById(order.getParcelId());
        if (parcel == null) {
            throw new ResourceNotFoundException("关联快递不存在");
        }
        if ("WAITING_ASSIGNMENT".equals(parcel.getCurrentStatus())) {
            parcelLifecycleService.changeStatus(parcel, "DELIVERING", userId,
                    COURIER_ROLE, "配送员接受智能派单 " + order.getOrderNo());
        } else if (!"DELIVERING".equals(parcel.getCurrentStatus())) {
            throw conflict("关联快递当前不能开始配送");
        }

        String targetAvailability = activeCount + 1 >= 8 ? "BUSY" : "AVAILABLE";
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, targetAvailability));
        return order;
    }

    private void createCandidateSnapshots(DeliveryOrder order, int round, LocalDateTime now) {
        for (DispatchCandidate candidate : assignmentMapper.selectEligibleCandidates(order.getId(), now)) {
            if (cooldownService.isCoolingDown(candidate.courierId(), now)) {
                continue;
            }
            RoutePlan plan = routePlanningService.planInsertion(candidate.courierId(),
                    candidate.currentLocationId(), order, now);
            DispatchScore score = scoringService.score(plan.incrementalDistanceMeters(),
                    candidate.activeOrderCount(), candidate.ratingAvg());
            DeliveryAssignment assignment = new DeliveryAssignment();
            assignment.setDeliveryOrderId(order.getId());
            assignment.setCourierId(candidate.courierId());
            assignment.setAssignmentRound(round);
            assignment.setAlgorithmVersion(properties.algorithmVersion());
            assignment.setRouteIncrementMeters(plan.incrementalDistanceMeters());
            assignment.setActiveOrderCount(candidate.activeOrderCount());
            assignment.setDistanceScore(score.distanceScore());
            assignment.setWorkloadScore(score.workloadScore());
            assignment.setServiceScore(score.serviceScore());
            assignment.setTotalScore(score.totalScore());
            assignment.setDecision("CANDIDATE");
            assignment.setDecisionReason("候选评分快照：路线增量、负载、服务三项加权");
            assignmentMapper.insert(assignment);
        }
    }

    private void offerNextCandidate(DeliveryOrder order, int round, LocalDateTime now) {
        Long existingOffer = assignmentMapper.selectCount(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                        .eq(DeliveryAssignment::getDecision, "OFFERED"));
        if (existingOffer != null && existingOffer > 0) {
            return;
        }
        List<DeliveryAssignment> candidates = assignmentMapper.selectList(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                        .eq(DeliveryAssignment::getAssignmentRound, round)
                        .eq(DeliveryAssignment::getDecision, "CANDIDATE")
                        .orderByDesc(DeliveryAssignment::getTotalScore)
                        .orderByAsc(DeliveryAssignment::getCourierId));
        for (DeliveryAssignment candidate : candidates) {
            Courier courier = courierMapper.selectByIdForUpdate(candidate.getCourierId());
            if (!isStillEligible(courier, order.getId(), now)) {
                skipCandidate(candidate.getId(), now, "邀请前状态、容量或冷却条件已变化");
                continue;
            }
            LocalDateTime expireTime = now.plusSeconds(properties.offerSeconds());
            int offered;
            try {
                offered = assignmentMapper.update(null,
                        new LambdaUpdateWrapper<DeliveryAssignment>()
                                .eq(DeliveryAssignment::getId, candidate.getId())
                                .eq(DeliveryAssignment::getDecision, "CANDIDATE")
                                .set(DeliveryAssignment::getDecision, "OFFERED")
                                .set(DeliveryAssignment::getOfferTime, now)
                                .set(DeliveryAssignment::getOfferExpireTime, expireTime)
                                .set(DeliveryAssignment::getDecisionReason,
                                        "当前轮剩余候选中综合得分最高"));
            } catch (DuplicateKeyException ex) {
                return;
            }
            if (offered == 1) {
                updateOrderDispatchState(order.getId(), "ASSIGNING", round, null, false);
                return;
            }
        }
        pauseForAdminRestart(order.getId(), round);
    }

    private boolean isStillEligible(Courier courier, long orderId, LocalDateTime now) {
        if (courier == null || !"ACTIVE".equals(courier.getEmploymentStatus())
                || !"AVAILABLE".equals(courier.getAvailabilityStatus())
                || courier.getCurrentLocationId() == null
                || orderMapper.countActiveByCourierId(courier.getId()) >= 8
                || cooldownService.isCoolingDown(courier.getId(), now)) {
            return false;
        }
        Long activeOfferCount = assignmentMapper.selectCount(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, orderId)
                        .eq(DeliveryAssignment::getCourierId, courier.getId())
                        .eq(DeliveryAssignment::getDecision, "OFFERED")
                        .gt(DeliveryAssignment::getOfferExpireTime, now));
        return activeOfferCount == null || activeOfferCount == 0;
    }

    private void pauseForAdminRestart(long orderId, int round) {
        updateOrderDispatchState(orderId, "ASSIGNING", round, null, true);
    }

    private void updateOrderDispatchState(long orderId, String status, int round,
                                          LocalDateTime nextTime, boolean manualRequired) {
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, orderId)
                .set(DeliveryOrder::getStatus, status)
                .set(DeliveryOrder::getDispatchRound, round)
                .set(DeliveryOrder::getNextDispatchTime, nextTime)
                .set(DeliveryOrder::getManualDispatchRequired, manualRequired ? 1 : 0));
    }

    private void skipCandidate(long assignmentId, LocalDateTime now, String reason) {
        assignmentMapper.update(null, new LambdaUpdateWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getId, assignmentId)
                .eq(DeliveryAssignment::getDecision, "CANDIDATE")
                .set(DeliveryAssignment::getDecision, "SKIPPED")
                .set(DeliveryAssignment::getDecidedTime, now)
                .set(DeliveryAssignment::getDecisionReason, reason));
    }

    private void ensureCourierEligible(Courier courier) {
        if (!"ACTIVE".equals(courier.getEmploymentStatus())
                || !"AVAILABLE".equals(courier.getAvailabilityStatus())
                || courier.getCurrentLocationId() == null) {
            throw conflict("配送员当前不满足接单条件");
        }
    }

    private void requireActiveOffer(DeliveryAssignment assignment, LocalDateTime now) {
        if (assignment == null || !"OFFERED".equals(assignment.getDecision())) {
            throw conflict("派单邀请已被处理");
        }
        if (assignment.getOfferExpireTime() == null
                || !assignment.getOfferExpireTime().isAfter(now)) {
            throw new BusinessException(ErrorCodes.DISPATCH_OFFER_EXPIRED,
                    "派单邀请已过期", HttpStatus.CONFLICT);
        }
    }

    private void validateRejectReason(DispatchRejectDTO request) {
        if ("OTHER".equals(request.reasonCode())
                && !StringUtils.hasText(request.reasonDetail())) {
            throw new BusinessException(ErrorCodes.DISPATCH_CONFLICT,
                    "选择其他原因时必须填写原因详情", HttpStatus.BAD_REQUEST);
        }
    }

    private DeliveryOrder requireLockedOrder(long orderId) {
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        return order;
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCodes.DISPATCH_CONFLICT, message, HttpStatus.CONFLICT);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
