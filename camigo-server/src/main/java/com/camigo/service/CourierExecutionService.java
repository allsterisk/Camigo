package com.camigo.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.RouteStop;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.vo.CampusLocationOptionVO;
import com.camigo.vo.CourierExecutionVO;
import com.camigo.vo.CourierLocationStateVO;
import com.camigo.vo.CourierRouteStepVO;
import com.camigo.vo.CourierRouteVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 配送执行节点、当前位置和剩余路线的一致性边界。 */
@Service
public class CourierExecutionService {

    private final CourierMapper courierMapper;
    private final CampusLocationMapper locationMapper;
    private final DeliveryOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final DeliveryRouteMapper routeMapper;
    private final RouteStopMapper stopMapper;
    private final CourierRoutePlanningService planningService;
    private final RouteVersionService routeVersionService;

    public CourierExecutionService(CourierMapper courierMapper,
                                   CampusLocationMapper locationMapper,
                                   DeliveryOrderMapper orderMapper,
                                   ParcelMapper parcelMapper,
                                   DeliveryRouteMapper routeMapper,
                                   RouteStopMapper stopMapper,
                                   CourierRoutePlanningService planningService,
                                   RouteVersionService routeVersionService) {
        this.courierMapper = courierMapper;
        this.locationMapper = locationMapper;
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.planningService = planningService;
        this.routeVersionService = routeVersionService;
    }

    @Transactional(readOnly = true)
    public List<CampusLocationOptionVO> activeLocations() {
        return locationMapper.selectActiveOptions();
    }

    @Transactional(readOnly = true)
    public CourierLocationStateVO locationState(long userId) {
        Courier courier = requireCourier(userId);
        return toLocationState(courier);
    }

    /** 每次登录后确认位置，再根据当前负载恢复接单状态。 */
    @Transactional
    public CourierLocationStateVO confirmLocation(long userId, long locationId) {
        Courier courier = requireCourierForUpdate(userId);
        ensureEmployed(courier);
        CampusLocation location = requireActiveLocation(locationId);
        LocalDateTime now = LocalDateTime.now();
        long activeCount = orderMapper.countActiveByCourierId(courier.getId());
        String availability = activeCount >= 8 ? "BUSY" : "AVAILABLE";
        int updated = courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getCurrentLocationId, location.getId())
                .set(Courier::getLocationUpdateTime, now)
                .set(Courier::getAvailabilityStatus, availability));
        if (updated != 1) {
            throw conflict("配送员位置已被其他请求修改");
        }
        courier.setCurrentLocationId(location.getId());
        courier.setLocationUpdateTime(now);
        courier.setAvailabilityStatus(availability);
        if (activeCount > 0) {
            refreshRoute(courier, now);
        }
        return new CourierLocationStateVO(courier.getId(), location.getId(),
                location.getLocationName(), now, availability, activeCount, true);
    }

    @Transactional
    public void setOffline(long userId) {
        Courier courier = requireCourierForUpdate(userId);
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, "OFFLINE"));
    }

    @Transactional(readOnly = true)
    public CourierRouteVO currentRoute(long userId) {
        Courier courier = requireCourier(userId);
        ensureLocationConfirmed(courier);
        return buildRoute(courier, routeMapper.selectActiveByCourierId(courier.getId()));
    }

    @Transactional
    public CourierExecutionVO confirmPickup(long userId, long orderId) {
        ExecutionContext context = lockExecutionContext(userId, orderId);
        DeliveryOrder order = context.order();
        if (!List.of("ACCEPTED", "PICKING_UP").contains(order.getStatus())) {
            throw statusConflict("当前订单状态不能确认取件");
        }
        if (!"DELIVERING".equals(context.parcel().getCurrentStatus())) {
            throw statusConflict("包裹状态与配送订单不一致，不能确认取件");
        }

        LocalDateTime now = LocalDateTime.now();
        RouteStop stop = requireActionableStop(context.courier(), order,
                "PICKUP", order.getPickupLocationId(), now);
        completeStop(stop, "COMPLETED", now);
        updateOrderStatus(order, List.of("ACCEPTED", "PICKING_UP"), "IN_TRANSIT", null);
        updateCourierLocation(context.courier(), order.getPickupLocationId(), now);
        DeliveryRoute refreshed = refreshRoute(context.courier(), now);
        return executionResult(order, context.parcel(), context.courier(), refreshed);
    }

    /** 阶段5只确认到达；入柜、取件码和COMPLETED由阶段6的入柜事务完成。 */
    @Transactional
    public CourierExecutionVO confirmArrival(long userId, long orderId) {
        ExecutionContext context = lockExecutionContext(userId, orderId);
        DeliveryOrder order = context.order();
        if (!"IN_TRANSIT".equals(order.getStatus())) {
            throw statusConflict("当前订单状态不能确认到达");
        }
        if (!"DELIVERING".equals(context.parcel().getCurrentStatus())) {
            throw statusConflict("包裹状态与配送订单不一致，不能确认到达");
        }

        LocalDateTime now = LocalDateTime.now();
        RouteStop stop = requireActionableStop(context.courier(), order,
                "DROPOFF", order.getDestinationLocationId(), now);
        completeStop(stop, "ARRIVED", now);
        updateOrderStatus(order, List.of("IN_TRANSIT"), "ARRIVED", null);
        updateCourierLocation(context.courier(), order.getDestinationLocationId(), now);
        DeliveryRoute refreshed = refreshRoute(context.courier(), now);
        return executionResult(order, context.parcel(), context.courier(), refreshed);
    }

    private ExecutionContext lockExecutionContext(long userId, long orderId) {
        Courier snapshot = requireCourier(userId);
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        Courier courier = courierMapper.selectByIdForUpdate(snapshot.getId());
        if (!courier.getId().equals(order.getAssignedCourierId())) {
            throw new ForbiddenException("不能操作其他配送员的任务");
        }
        ensureEmployed(courier);
        ensureLocationConfirmed(courier);
        ParcelEntity parcel = parcelMapper.selectByIdForUpdate(order.getParcelId());
        if (parcel == null) {
            throw new ResourceNotFoundException("关联包裹不存在");
        }
        return new ExecutionContext(courier, order, parcel);
    }

    private RouteStop requireActionableStop(Courier courier, DeliveryOrder order,
                                            String stopType, Long expectedLocationId,
                                            LocalDateTime now) {
        if (expectedLocationId == null) {
            throw routeConflict("配送订单缺少节点地点");
        }
        DeliveryRoute route = routeMapper.selectActiveByCourierIdForUpdate(courier.getId());
        List<RouteStop> pending = route == null
                ? List.of() : stopMapper.selectPendingByRouteIdForUpdate(route.getId());
        if (pending.isEmpty()) {
            route = refreshRoute(courier, now);
            if ("COMPLETED".equals(route.getStatus())) {
                throw routeConflict("当前路线中没有可执行节点");
            }
            routeMapper.selectActiveByCourierIdForUpdate(courier.getId());
            pending = stopMapper.selectPendingByRouteIdForUpdate(route.getId());
        }
        RouteStop target = pending.stream()
                .filter(stop -> Objects.equals(stop.getDeliveryOrderId(), order.getId()))
                .filter(stop -> stopType.equals(stop.getStopType()))
                .findFirst()
                .orElseThrow(() -> routeConflict("当前路线不包含该订单的待执行节点"));
        Long firstLocationId = pending.get(0).getLocationId();
        if (!Objects.equals(firstLocationId, expectedLocationId)
                || !Objects.equals(target.getLocationId(), expectedLocationId)) {
            throw routeConflict("请按当前路线顺序完成下一地点的任务");
        }
        return target;
    }

    private void completeStop(RouteStop stop, String status, LocalDateTime now) {
        int updated = stopMapper.update(null, new LambdaUpdateWrapper<RouteStop>()
                .eq(RouteStop::getId, stop.getId())
                .eq(RouteStop::getStopStatus, "PENDING")
                .set(RouteStop::getStopStatus, status)
                .set(RouteStop::getActualArrivalTime, now));
        if (updated != 1) {
            throw routeConflict("路线节点已被其他请求处理");
        }
        stop.setStopStatus(status);
        stop.setActualArrivalTime(now);
    }

    private void updateOrderStatus(DeliveryOrder order, List<String> sourceStatuses,
                                   String targetStatus, LocalDateTime completedTime) {
        LambdaUpdateWrapper<DeliveryOrder> update = new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getAssignedCourierId, order.getAssignedCourierId())
                .in(DeliveryOrder::getStatus, sourceStatuses)
                .set(DeliveryOrder::getStatus, targetStatus);
        if (completedTime != null) {
            update.set(DeliveryOrder::getCompletedTime, completedTime);
        }
        if (orderMapper.update(null, update) != 1) {
            throw conflict("配送订单状态已被其他请求修改");
        }
        order.setStatus(targetStatus);
        order.setCompletedTime(completedTime);
    }

    private void updateCourierLocation(Courier courier, Long locationId, LocalDateTime now) {
        if (courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getCurrentLocationId, locationId)
                .set(Courier::getLocationUpdateTime, now)) != 1) {
            throw conflict("配送员位置已被其他请求修改");
        }
        courier.setCurrentLocationId(locationId);
        courier.setLocationUpdateTime(now);
    }

    private DeliveryRoute refreshRoute(Courier courier, LocalDateTime now) {
        RoutePlan plan = planningService.planRemaining(courier.getId(),
                courier.getCurrentLocationId(), now);
        return routeVersionService.saveAcceptedPlan(courier.getId(), plan);
    }

    private CourierExecutionVO executionResult(DeliveryOrder order, ParcelEntity parcel,
                                               Courier courier, DeliveryRoute route) {
        CampusLocation location = requireActiveLocation(courier.getCurrentLocationId());
        return new CourierExecutionVO(order.getId(), order.getOrderNo(), order.getStatus(),
                parcel.getCurrentStatus(), courier.getCurrentLocationId(),
                location.getLocationName(), buildRoute(courier, route));
    }

    private CourierRouteVO buildRoute(Courier courier, DeliveryRoute route) {
        CampusLocation current = courier.getCurrentLocationId() == null
                ? null : locationMapper.selectById(courier.getCurrentLocationId());
        if (route == null) {
            return new CourierRouteVO(courier.getId(), courier.getCurrentLocationId(),
                    current == null ? null : current.getLocationName(), null, 0, 0,
                    null, List.of());
        }
        List<RouteStop> pending = stopMapper.selectByRouteId(route.getId()).stream()
                .filter(stop -> "PENDING".equals(stop.getStopStatus()))
                .filter(stop -> !"CURRENT".equals(stop.getStopType()))
                .toList();
        if (pending.isEmpty()) {
            return new CourierRouteVO(courier.getId(), courier.getCurrentLocationId(),
                    current == null ? null : current.getLocationName(), route.getVersionNo(),
                    route.getTotalDistanceMeters(), route.getEstimatedDurationSeconds(),
                    route.getAlgorithmVersion(), List.of());
        }
        Map<Long, CampusLocation> locations = new LinkedHashMap<>();
        for (CampusLocation location : locationMapper.selectBatchIds(
                pending.stream().map(RouteStop::getLocationId).distinct().toList())) {
            locations.put(location.getId(), location);
        }
        Map<Long, DeliveryOrder> orders = new LinkedHashMap<>();
        for (DeliveryOrder order : orderMapper.selectBatchIds(
                pending.stream().map(RouteStop::getDeliveryOrderId).filter(Objects::nonNull)
                        .distinct().toList())) {
            orders.put(order.getId(), order);
        }

        List<RouteStepAccumulator> grouped = new ArrayList<>();
        for (RouteStop stop : pending) {
            RouteStepAccumulator accumulator = grouped.isEmpty()
                    ? null : grouped.get(grouped.size() - 1);
            if (accumulator == null || !Objects.equals(accumulator.locationId, stop.getLocationId())) {
                CampusLocation location = locations.get(stop.getLocationId());
                accumulator = new RouteStepAccumulator(grouped.size() + 1, stop.getLocationId(),
                        location == null ? "未知地点" : location.getLocationName(),
                        stop.getEstimatedArrivalTime());
                grouped.add(accumulator);
            }
            DeliveryOrder order = orders.get(stop.getDeliveryOrderId());
            String orderNo = order == null ? "#" + stop.getDeliveryOrderId() : order.getOrderNo();
            if ("PICKUP".equals(stop.getStopType())) {
                accumulator.pickupOrderNos.add(orderNo);
            } else if ("DROPOFF".equals(stop.getStopType())) {
                accumulator.dropoffOrderNos.add(orderNo);
            }
        }
        List<CourierRouteStepVO> steps = grouped.stream().map(RouteStepAccumulator::toVo).toList();
        return new CourierRouteVO(courier.getId(), courier.getCurrentLocationId(),
                current == null ? null : current.getLocationName(), route.getVersionNo(),
                route.getTotalDistanceMeters(), route.getEstimatedDurationSeconds(),
                route.getAlgorithmVersion(), steps);
    }

    private CourierLocationStateVO toLocationState(Courier courier) {
        CampusLocation location = courier.getCurrentLocationId() == null
                ? null : locationMapper.selectById(courier.getCurrentLocationId());
        long activeCount = orderMapper.countActiveByCourierId(courier.getId());
        boolean confirmed = courier.getCurrentLocationId() != null
                && courier.getLocationUpdateTime() != null
                && !"OFFLINE".equals(courier.getAvailabilityStatus());
        return new CourierLocationStateVO(courier.getId(), courier.getCurrentLocationId(),
                location == null ? null : location.getLocationName(),
                courier.getLocationUpdateTime(), courier.getAvailabilityStatus(),
                activeCount, confirmed);
    }

    private Courier requireCourier(long userId) {
        Courier courier = courierMapper.selectByUserId(userId);
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND,
                    "配送员档案不存在", HttpStatus.NOT_FOUND);
        }
        return courier;
    }

    private Courier requireCourierForUpdate(long userId) {
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND,
                    "配送员档案不存在", HttpStatus.NOT_FOUND);
        }
        return courier;
    }

    private CampusLocation requireActiveLocation(long locationId) {
        CampusLocation location = locationMapper.selectById(locationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())) {
            throw new ResourceNotFoundException("校园地点不存在或已停用");
        }
        return location;
    }

    private void ensureEmployed(Courier courier) {
        if (!"ACTIVE".equals(courier.getEmploymentStatus())) {
            throw new ForbiddenException("配送员账号当前不可执行配送任务");
        }
    }

    private void ensureLocationConfirmed(Courier courier) {
        if (courier.getCurrentLocationId() == null
                || courier.getLocationUpdateTime() == null
                || "OFFLINE".equals(courier.getAvailabilityStatus())) {
            throw new BusinessException(ErrorCodes.COURIER_LOCATION_REQUIRED,
                    "请先确认当前校园位置", HttpStatus.CONFLICT);
        }
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCodes.DELIVERY_CONFLICT, message, HttpStatus.CONFLICT);
    }

    private BusinessException routeConflict(String message) {
        return new BusinessException(ErrorCodes.ROUTE_STEP_CONFLICT, message, HttpStatus.CONFLICT);
    }

    private BusinessException statusConflict(String message) {
        return new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID,
                message, HttpStatus.CONFLICT);
    }

    private record ExecutionContext(Courier courier, DeliveryOrder order, ParcelEntity parcel) {
    }

    private static final class RouteStepAccumulator {
        private final int sequenceNo;
        private final Long locationId;
        private final String locationName;
        private final LocalDateTime estimatedArrivalTime;
        private final List<String> pickupOrderNos = new ArrayList<>();
        private final List<String> dropoffOrderNos = new ArrayList<>();

        private RouteStepAccumulator(int sequenceNo, Long locationId, String locationName,
                                     LocalDateTime estimatedArrivalTime) {
            this.sequenceNo = sequenceNo;
            this.locationId = locationId;
            this.locationName = locationName;
            this.estimatedArrivalTime = estimatedArrivalTime;
        }

        private CourierRouteStepVO toVo() {
            return new CourierRouteStepVO(sequenceNo, locationId, locationName,
                    List.copyOf(pickupOrderNos), List.copyOf(dropoffOrderNos),
                    estimatedArrivalTime);
        }
    }
}
