package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.common.ErrorCodes;
import com.camigo.common.PageResult;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.DeliveryOrderService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.vo.DeliveryOrderVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 配送订单服务实现。公共 API 状态与数据库细粒度状态在本层完成映射。
 */
@Service
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private static final String USER_ROLE = "USER";
    private static final String COURIER_ROLE = "COURIER";
    private static final Set<String> WAITING_INTERNAL_STATUSES = Set.of(
            "CREATED", "ASSIGNING", "ASSIGNED");
    private static final Set<String> PROCESSING_INTERNAL_STATUSES = Set.of(
            "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED");
    private static final Set<String> PUBLIC_INTERNAL_STATUSES = Set.of(
            "CREATED", "ASSIGNING", "ASSIGNED", "ACCEPTED",
            "PICKING_UP", "IN_TRANSIT", "ARRIVED", "COMPLETED");
    private static final Set<String> ACTIVE_COURIER_ORDER_STATUSES = Set.of(
            "ASSIGNED", "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED");
    private static final Set<String> PUBLIC_STATUSES = Set.of(
            "WAITING", "PROCESSING", "DELIVERED");
    private static final Map<String, Set<String>> PUBLIC_TO_INTERNAL_STATUSES = Map.of(
            "WAITING", WAITING_INTERNAL_STATUSES,
            "PROCESSING", PROCESSING_INTERNAL_STATUSES,
            "DELIVERED", Set.of("COMPLETED"));

    private final DeliveryOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final CourierMapper courierMapper;
    private final CampusLocationMapper locationMapper;
    private final UserMapper userMapper;
    private final ParcelLifecycleService parcelLifecycleService;

    public DeliveryOrderServiceImpl(DeliveryOrderMapper orderMapper,
                                    ParcelMapper parcelMapper,
                                    CourierMapper courierMapper,
                                    CampusLocationMapper locationMapper,
                                    UserMapper userMapper,
                                    ParcelLifecycleService parcelLifecycleService) {
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.courierMapper = courierMapper;
        this.locationMapper = locationMapper;
        this.userMapper = userMapper;
        this.parcelLifecycleService = parcelLifecycleService;
    }

    @Override
    @Transactional
    public DeliveryOrderVO create(Long userId, DeliveryOrderCreateDTO request) {
        requireRole(userId, USER_ROLE);
        ParcelEntity parcel = parcelMapper.selectOne(new LambdaQueryWrapper<ParcelEntity>()
                .eq(ParcelEntity::getId, request.parcelId())
                .eq(ParcelEntity::getRecipientUserId, userId));
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        if (!Set.of("REGISTERED", "SORTING", "WAITING_ASSIGNMENT")
                .contains(parcel.getCurrentStatus())) {
            throw invalidStatus("当前快递状态不能创建配送任务");
        }

        Long destinationId = request.destinationLocationId() == null
                ? parcel.getDestinationLocationId() : request.destinationLocationId();
        if (destinationId == null) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送目的地不能为空",
                    HttpStatus.BAD_REQUEST);
        }
        CampusLocation location = locationMapper.selectById(destinationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())) {
            throw new ResourceNotFoundException("配送目的地不存在或已停用");
        }

        DeliveryOrder order = new DeliveryOrder();
        order.setOrderNo(generateOrderNo());
        order.setParcelId(parcel.getId());
        order.setDestinationLocationId(destinationId);
        order.setPriority(StringUtils.hasText(request.priority())
                ? request.priority().trim().toUpperCase() : "NORMAL");
        order.setStatus("CREATED");
        order.setPredictedMinutes(request.predictedMinutes());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "该快递已有活动配送任务");
        }
        parcelLifecycleService.changeStatus(parcel, "WAITING_ASSIGNMENT", userId,
                USER_ROLE, "创建配送任务 " + order.getOrderNo());
        return toVo(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeliveryOrderVO> listTasks(Long userId, long page, long pageSize,
                                                  String status) {
        requireRole(userId, COURIER_ROLE);
        Courier courier = requireCourier(userId);
        if (!"ACTIVE".equals(courier.getEmploymentStatus())) {
            throw new ForbiddenException("配送员账号当前不可执行配送任务");
        }

        LambdaQueryWrapper<DeliveryOrder> query = new LambdaQueryWrapper<DeliveryOrder>()
                .in(DeliveryOrder::getStatus, PUBLIC_INTERNAL_STATUSES)
                .and(scope -> scope
                        .isNull(DeliveryOrder::getAssignedCourierId)
                        .in(DeliveryOrder::getStatus, WAITING_INTERNAL_STATUSES)
                        .or()
                        .eq(DeliveryOrder::getAssignedCourierId, courier.getId()))
                .orderByDesc(DeliveryOrder::getCreateTime);
        applyPublicStatusFilter(query, status);

        Page<DeliveryOrder> result = orderMapper.selectPage(new Page<>(page, pageSize), query);
        List<DeliveryOrderVO> records = result.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public DeliveryOrderVO accept(Long userId, Long orderId) {
        requireRole(userId, COURIER_ROLE);
        Courier courier = requireCourierForUpdate(userId);
        ensureCourierCanAccept(courier);
        DeliveryOrder order = requireLockedOrder(orderId);

        if ("ACCEPTED".equals(order.getStatus())
                && courier.getId().equals(order.getAssignedCourierId())) {
            return toVo(order);
        }
        if (!WAITING_INTERNAL_STATUSES.contains(order.getStatus())) {
            throw invalidStatus("当前配送任务不可接单");
        }
        if (order.getAssignedCourierId() != null
                && !courier.getId().equals(order.getAssignedCourierId())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送任务已分配给其他配送员");
        }
        ensureCourierCapacity(courier, orderId);

        LocalDateTime now = LocalDateTime.now();
        String previousStatus = order.getStatus();
        Long previousCourierId = order.getAssignedCourierId();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, previousStatus)
                .eq(previousCourierId != null, DeliveryOrder::getAssignedCourierId, previousCourierId)
                .isNull(previousCourierId == null, DeliveryOrder::getAssignedCourierId)
                .set(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "ACCEPTED")
                .set(DeliveryOrder::getAcceptedTime, now));
        if (updated != 1) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送任务已被其他配送员接取");
        }
        order.setAssignedCourierId(courier.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(now);
        if (!"BUSY".equals(courier.getAvailabilityStatus())) {
            courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                    .eq(Courier::getId, courier.getId())
                    .set(Courier::getAvailabilityStatus, "BUSY"));
            courier.setAvailabilityStatus("BUSY");
        }

        ParcelEntity parcel = requireParcel(order.getParcelId());
        if ("WAITING_ASSIGNMENT".equals(parcel.getCurrentStatus())) {
            parcelLifecycleService.changeStatus(parcel, "DELIVERING", userId,
                    COURIER_ROLE, "配送员接单 " + order.getOrderNo());
        } else if (!"DELIVERING".equals(parcel.getCurrentStatus())) {
            throw invalidStatus("关联快递当前不能开始配送");
        }
        return toVo(order);
    }

    @Override
    @Transactional
    public DeliveryOrderVO updateStatus(Long userId, Long orderId,
                                        DeliveryOrderStatusDTO request) {
        requireRole(userId, COURIER_ROLE);
        Courier courier = requireCourierForUpdate(userId);
        DeliveryOrder order = requireLockedOrder(orderId);
        if (!courier.getId().equals(order.getAssignedCourierId())) {
            throw new ForbiddenException("不能修改其他配送员的任务");
        }

        String targetStatus = request.status().trim().toUpperCase();
        String currentPublicStatus = toPublicStatus(order.getStatus());
        if (targetStatus.equals(currentPublicStatus)) {
            return toVo(order);
        }
        if (!"PROCESSING".equals(currentPublicStatus) || !"DELIVERED".equals(targetStatus)) {
            throw invalidStatus("配送状态不能从 " + currentPublicStatus + " 变更为 " + targetStatus);
        }

        LocalDateTime completedTime = LocalDateTime.now();
        String previousStatus = order.getStatus();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, previousStatus)
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "COMPLETED")
                .set(DeliveryOrder::getCompletedTime, completedTime));
        if (updated != 1) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送状态已被其他请求修改");
        }
        order.setStatus("COMPLETED");
        order.setCompletedTime(completedTime);
        releaseCourierWhenIdle(courier);
        return toVo(order);
    }

    private void applyPublicStatusFilter(LambdaQueryWrapper<DeliveryOrder> query, String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        String normalized = status.trim().toUpperCase();
        if (!PUBLIC_STATUSES.contains(normalized)) {
            throw invalidStatus("不支持的配送状态筛选条件");
        }
        query.in(DeliveryOrder::getStatus, PUBLIC_TO_INTERNAL_STATUSES.get(normalized));
    }

    private void ensureCourierCanAccept(Courier courier) {
        if (!"ACTIVE".equals(courier.getEmploymentStatus())
                || !Set.of("AVAILABLE", "BUSY").contains(courier.getAvailabilityStatus())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送员当前不可接单");
        }
    }

    private void ensureCourierCapacity(Courier courier, Long currentOrderId) {
        Long activeCount = orderMapper.selectCount(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId())
                .ne(DeliveryOrder::getId, currentOrderId)
                .in(DeliveryOrder::getStatus, ACTIVE_COURIER_ORDER_STATUSES));
        int maximum = courier.getMaxActiveOrders() == null ? 1 : courier.getMaxActiveOrders();
        if (activeCount != null && activeCount >= maximum) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送员活动任务已达到上限");
        }
    }

    private void releaseCourierWhenIdle(Courier courier) {
        Long activeCount = orderMapper.selectCount(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId())
                .in(DeliveryOrder::getStatus, ACTIVE_COURIER_ORDER_STATUSES));
        if (activeCount == null || activeCount == 0) {
            courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                    .eq(Courier::getId, courier.getId())
                    .eq(Courier::getAvailabilityStatus, "BUSY")
                    .set(Courier::getAvailabilityStatus, "AVAILABLE"));
            courier.setAvailabilityStatus("AVAILABLE");
        }
    }

    private Courier requireCourier(Long userId) {
        Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getUserId, userId));
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND, "配送员档案不存在",
                    HttpStatus.NOT_FOUND);
        }
        return courier;
    }

    private Courier requireCourierForUpdate(Long userId) {
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND, "配送员档案不存在",
                    HttpStatus.NOT_FOUND);
        }
        return courier;
    }

    private DeliveryOrder requireLockedOrder(Long orderId) {
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        return order;
    }

    private ParcelEntity requireParcel(Long parcelId) {
        ParcelEntity parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        return parcel;
    }

    private void requireRole(Long userId, String role) {
        List<String> roles = userMapper.selectRoleCodes(userId);
        if (roles == null || !roles.contains(role)) {
            throw new ForbiddenException("没有执行该配送操作的权限");
        }
    }

    private String generateOrderNo() {
        return "DO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private BusinessException invalidStatus(String message) {
        return new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID, message,
                HttpStatus.BAD_REQUEST);
    }

    private DeliveryOrderVO toVo(DeliveryOrder order) {
        return new DeliveryOrderVO(order.getId(), order.getOrderNo(), order.getParcelId(),
                order.getDestinationLocationId(), order.getAssignedCourierId(), order.getPriority(),
                toPublicStatus(order.getStatus()), order.getPredictedMinutes(), order.getAcceptedTime(),
                order.getCompletedTime(), order.getCreateTime(), order.getUpdateTime());
    }

    private String toPublicStatus(String internalStatus) {
        if (WAITING_INTERNAL_STATUSES.contains(internalStatus)) {
            return "WAITING";
        }
        if (PROCESSING_INTERNAL_STATUSES.contains(internalStatus)) {
            return "PROCESSING";
        }
        if ("COMPLETED".equals(internalStatus)) {
            return "DELIVERED";
        }
        return internalStatus;
    }
}
