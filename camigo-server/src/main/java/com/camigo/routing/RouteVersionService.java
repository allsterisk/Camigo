package com.camigo.routing;

import com.camigo.common.ErrorCodes;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.RouteStop;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.RouteStopMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在配送员接受方案后，以不可覆盖的版本方式保存综合路线。 */
@Service
public class RouteVersionService {

    private final CourierMapper courierMapper;
    private final DeliveryRouteMapper routeMapper;
    private final RouteStopMapper stopMapper;

    public RouteVersionService(CourierMapper courierMapper,
                               DeliveryRouteMapper routeMapper,
                               RouteStopMapper stopMapper) {
        this.courierMapper = courierMapper;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
    }

    /**
     * 保存已接受的计算结果。调用前的候选路线计算不会写数据库。
     * 配送员行锁串行化版本号分配，数据库唯一键再兜底保证单一ACTIVE路线。
     */
    @Transactional
    public DeliveryRoute saveAcceptedPlan(long courierId, RoutePlan plan) {
        Courier courier = courierMapper.selectByIdForUpdate(courierId);
        if (courier == null) {
            throw new ResourceNotFoundException("配送员档案不存在");
        }
        if (courier.getCurrentLocationId() == null
                || courier.getCurrentLocationId() != plan.currentLocationId()) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT,
                    "配送员当前位置已变化，请重新计算路线", HttpStatus.CONFLICT);
        }
        validatePrecedence(plan);

        routeMapper.selectActiveByCourierIdForUpdate(courierId);
        int nextVersion = routeMapper.selectLatestVersion(courierId) + 1;
        routeMapper.supersedeActive(courierId);

        DeliveryRoute route = new DeliveryRoute();
        route.setCourierId(courierId);
        route.setVersionNo(nextVersion);
        route.setStatus(plan.stops().isEmpty() ? "COMPLETED" : "ACTIVE");
        route.setTotalDistanceMeters(plan.totalDistanceMeters());
        route.setEstimatedDurationSeconds(plan.estimatedDurationSeconds());
        route.setAlgorithmVersion(plan.algorithmVersion());
        try {
            routeMapper.insert(route);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT,
                    "路线版本已被并发请求更新，请重新计算", HttpStatus.CONFLICT);
        }

        // CURRENT 是路线版本快照，不能因首个业务节点同地点而去重。
        insertCurrentStop(route.getId(), plan);
        int sequence = 2;
        for (ScheduledRouteStop scheduled : plan.stops()) {
            RouteStopTask task = scheduled.task();
            RouteStop stop = new RouteStop();
            stop.setDeliveryRouteId(route.getId());
            stop.setDeliveryOrderId(task.deliveryOrderId());
            stop.setLocationId(task.locationId());
            stop.setSequenceNo(sequence++);
            stop.setStopType(task.stopType().name());
            stop.setStopStatus("PENDING");
            stop.setEstimatedArrivalTime(scheduled.estimatedArrivalTime());
            stopMapper.insert(stop);
        }
        return route;
    }

    private void insertCurrentStop(Long routeId, RoutePlan plan) {
        RouteStop current = new RouteStop();
        current.setDeliveryRouteId(routeId);
        current.setLocationId(plan.currentLocationId());
        current.setSequenceNo(1);
        current.setStopType("CURRENT");
        current.setStopStatus("COMPLETED");
        current.setEstimatedArrivalTime(plan.calculatedAt());
        current.setActualArrivalTime(plan.calculatedAt());
        stopMapper.insert(current);
    }

    private void validatePrecedence(RoutePlan plan) {
        Map<Long, Integer> pickups = new HashMap<>();
        Map<Long, Integer> dropoffs = new HashMap<>();
        for (int index = 0; index < plan.stops().size(); index++) {
            RouteStopTask task = plan.stops().get(index).task();
            Map<Long, Integer> target = task.stopType() == RouteStopType.PICKUP
                    ? pickups : dropoffs;
            if (target.put(task.deliveryOrderId(), index) != null) {
                throw new IllegalArgumentException("同一订单存在重复停靠类型");
            }
        }
        for (Map.Entry<Long, Integer> pickup : pickups.entrySet()) {
            Integer dropoff = dropoffs.get(pickup.getKey());
            if (dropoff != null && pickup.getValue() >= dropoff) {
                throw new IllegalArgumentException("路线违反先取后送约束");
            }
        }
    }
}
