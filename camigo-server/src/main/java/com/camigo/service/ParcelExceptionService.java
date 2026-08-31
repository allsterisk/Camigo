package com.camigo.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.common.ErrorCodes;
import com.camigo.common.PageResult;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.dto.ParcelExceptionReportDTO;
import com.camigo.dto.ParcelExceptionResolutionDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.ParcelExceptionReport;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelExceptionReportMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.vo.ParcelExceptionVO;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 配送异常上报与管理员处理的一致性事务边界。 */
@Service
public class ParcelExceptionService {
    private static final Set<String> REPORTABLE_ORDER_STATUSES =
            Set.of("PICKING_UP", "IN_TRANSIT", "ARRIVED");
    private static final Set<String> REPORT_STATUSES = Set.of("PENDING", "RESOLVED", "RETURNED");

    private final ParcelExceptionReportMapper exceptionMapper;
    private final DeliveryOrderMapper orderMapper;
    private final CourierMapper courierMapper;
    private final ParcelMapper parcelMapper;
    private final RouteStopMapper stopMapper;
    private final ParcelLifecycleService parcelLifecycleService;
    private final CourierRoutePlanningService planningService;
    private final RouteVersionService routeVersionService;

    public ParcelExceptionService(ParcelExceptionReportMapper exceptionMapper,
                                  DeliveryOrderMapper orderMapper,
                                  CourierMapper courierMapper,
                                  ParcelMapper parcelMapper,
                                  RouteStopMapper stopMapper,
                                  ParcelLifecycleService parcelLifecycleService,
                                  CourierRoutePlanningService planningService,
                                  RouteVersionService routeVersionService) {
        this.exceptionMapper = exceptionMapper;
        this.orderMapper = orderMapper;
        this.courierMapper = courierMapper;
        this.parcelMapper = parcelMapper;
        this.stopMapper = stopMapper;
        this.parcelLifecycleService = parcelLifecycleService;
        this.planningService = planningService;
        this.routeVersionService = routeVersionService;
    }

    @Transactional
    public ParcelExceptionVO report(long userId, long orderId, ParcelExceptionReportDTO request) {
        DeliveryOrder order = requireOrderForUpdate(orderId);
        Courier courier = requireCourierForUpdate(userId);
        if (!courier.getId().equals(order.getAssignedCourierId())) {
            throw new ForbiddenException("只能上报本人配送任务的异常");
        }
        if (!REPORTABLE_ORDER_STATUSES.contains(order.getStatus())) {
            throw conflict("当前配送状态不能上报异常");
        }
        if (courier.getCurrentLocationId() == null || courier.getLocationUpdateTime() == null) {
            throw conflict("请先确认当前位置再上报异常");
        }
        ParcelEntity parcel = requireParcelForUpdate(order.getParcelId());
        if (!"DELIVERING".equals(parcel.getCurrentStatus())) {
            throw conflict("当前包裹状态不能上报配送异常");
        }

        LocalDateTime now = LocalDateTime.now();
        ParcelExceptionReport report = new ParcelExceptionReport();
        report.setParcelId(parcel.getId());
        report.setDeliveryOrderId(order.getId());
        report.setCourierId(courier.getId());
        report.setExceptionType(request.exceptionType().trim().toUpperCase(Locale.ROOT));
        report.setDescription(request.description().trim());
        report.setRequiresAdmin(1);
        report.setStatus("PENDING");
        report.setReportedTime(now);
        try {
            exceptionMapper.insert(report);
        } catch (DuplicateKeyException ex) {
            throw conflict("该包裹已有待处理异常，请勿重复上报");
        }

        parcelLifecycleService.changeStatus(parcel, "EXCEPTION", userId,
                "COURIER", "配送员上报异常：" + report.getExceptionType());
        int orderUpdated = orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, order.getStatus())
                .set(DeliveryOrder::getStatus, "FAILED")
                .set(DeliveryOrder::getCompletedTime, now));
        if (orderUpdated != 1) throw conflict("配送订单状态已变化，请刷新后重试");

        stopMapper.skipActiveByOrderId(courier.getId(), order.getId());
        refreshCourierState(courier);
        saveRemainingRoute(courier, now);
        return exceptionMapper.selectDetailById(report.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<ParcelExceptionVO> listAdmin(long page, long pageSize,
                                                    String status, String keyword) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<ParcelExceptionVO> result = exceptionMapper.selectAdminPage(
                new Page<>(page, pageSize), normalizedStatus, normalizedKeyword);
        return new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Transactional
    public ParcelExceptionVO resolve(long adminUserId, long reportId,
                                     ParcelExceptionResolutionDTO request) {
        ParcelExceptionReport snapshot = exceptionMapper.selectById(reportId);
        if (snapshot == null) throw new ResourceNotFoundException("异常记录不存在");
        DeliveryOrder order = requireOrderForUpdate(snapshot.getDeliveryOrderId());
        Courier courier = courierMapper.selectByIdForUpdate(snapshot.getCourierId());
        if (courier == null) throw new ResourceNotFoundException("上报配送员不存在");
        ParcelEntity parcel = requireParcelForUpdate(snapshot.getParcelId());
        ParcelExceptionReport report = exceptionMapper.selectByIdForUpdate(reportId);
        if (report == null) throw new ResourceNotFoundException("异常记录不存在");
        if (!"PENDING".equals(report.getStatus())) throw conflict("该异常已经处理，不能重复操作");
        if (!"FAILED".equals(order.getStatus()) || !"EXCEPTION".equals(parcel.getCurrentStatus())) {
            throw conflict("包裹或配送订单状态与待处理异常不一致");
        }

        String resolution = request.resolutionType().trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        String reportStatus;
        switch (resolution) {
            case "RESUME_DELIVERY" -> {
                resumeDelivery(parcel, order, courier, adminUserId, now);
                reportStatus = "RESOLVED";
            }
            case "RETURN_TO_SORTING" -> {
                parcelLifecycleService.changeStatus(parcel, "SORTING", adminUserId,
                        "ADMIN", "异常处理：包裹已回服务站，恢复分拣");
                moveCourierToPickup(courier, order, now);
                refreshCourierState(courier);
                saveRemainingRoute(courier, now);
                reportStatus = "RESOLVED";
            }
            case "CONFIRM_RETURNED" -> {
                parcelLifecycleService.changeStatus(parcel, "RETURNED", adminUserId,
                        "ADMIN", "异常处理：管理员确认包裹已退回服务站");
                moveCourierToPickup(courier, order, now);
                refreshCourierState(courier);
                saveRemainingRoute(courier, now);
                reportStatus = "RETURNED";
            }
            default -> throw new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID,
                    "不支持的异常处理方式", HttpStatus.BAD_REQUEST);
        }

        report.setStatus(reportStatus);
        report.setHandledByUserId(adminUserId);
        report.setHandledTime(now);
        report.setResolutionType(resolution);
        report.setResolutionNote(request.resolutionNote().trim());
        if (exceptionMapper.updateById(report) != 1) throw conflict("异常记录已被其他管理员处理");
        return exceptionMapper.selectDetailById(reportId);
    }

    private void resumeDelivery(ParcelEntity parcel, DeliveryOrder order, Courier courier,
                                long adminUserId, LocalDateTime now) {
        long activeBefore = orderMapper.countActiveByCourierId(courier.getId());
        int capacity = courier.getMaxActiveOrders() == null ? 8 : courier.getMaxActiveOrders();
        if (activeBefore >= capacity) throw conflict("原配送员活动任务已达到上限，不能恢复配送");
        parcelLifecycleService.changeStatus(parcel, "DELIVERING", adminUserId,
                "ADMIN", "异常已解决，由原配送员继续配送");
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, "FAILED")
                .set(DeliveryOrder::getStatus, "IN_TRANSIT")
                .set(DeliveryOrder::getCompletedTime, null));
        if (updated != 1) throw conflict("配送订单状态已变化，不能恢复配送");
        order.setStatus("IN_TRANSIT");
        order.setCompletedTime(null);
        refreshCourierState(courier);
        RoutePlan plan = planningService.planRestoredDelivery(courier.getId(),
                courier.getCurrentLocationId(), order, now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);
    }

    private void moveCourierToPickup(Courier courier, DeliveryOrder order, LocalDateTime now) {
        int updated = courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getCurrentLocationId, order.getPickupLocationId())
                .set(Courier::getLocationUpdateTime, now));
        if (updated != 1) throw conflict("配送员位置更新失败");
        courier.setCurrentLocationId(order.getPickupLocationId());
        courier.setLocationUpdateTime(now);
    }

    private void refreshCourierState(Courier courier) {
        long activeCount = orderMapper.countActiveByCourierId(courier.getId());
        int capacity = courier.getMaxActiveOrders() == null ? 8 : courier.getMaxActiveOrders();
        String availability = "OFFLINE".equals(courier.getAvailabilityStatus())
                ? "OFFLINE" : activeCount >= capacity ? "BUSY" : "AVAILABLE";
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, availability));
        courier.setAvailabilityStatus(availability);
    }

    private void saveRemainingRoute(Courier courier, LocalDateTime now) {
        if (courier.getCurrentLocationId() == null) return;
        RoutePlan plan = planningService.planRemaining(courier.getId(),
                courier.getCurrentLocationId(), now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);
    }

    private DeliveryOrder requireOrderForUpdate(long orderId) {
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) throw new ResourceNotFoundException("配送任务不存在");
        return order;
    }

    private Courier requireCourierForUpdate(long userId) {
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) throw new ResourceNotFoundException("配送员档案不存在");
        if (!"ACTIVE".equals(courier.getEmploymentStatus())) {
            throw new ForbiddenException("配送员账号当前不可执行配送任务");
        }
        return courier;
    }

    private ParcelEntity requireParcelForUpdate(long parcelId) {
        ParcelEntity parcel = parcelMapper.selectByIdForUpdate(parcelId);
        if (parcel == null) throw new ResourceNotFoundException("快递不存在");
        return parcel;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!REPORT_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID,
                    "异常状态筛选条件不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCodes.DELIVERY_CONFLICT, message, HttpStatus.CONFLICT);
    }
}
