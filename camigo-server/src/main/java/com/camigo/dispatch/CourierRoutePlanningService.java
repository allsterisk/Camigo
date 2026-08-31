package com.camigo.dispatch;

import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.RouteStop;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.RouteInsertionPlanner;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteStopTask;
import com.camigo.routing.RouteStopType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** 读取配送员当前未完成路线，并为新订单生成候选插入方案。 */
@Service
public class CourierRoutePlanningService {

    private final DeliveryRouteMapper routeMapper;
    private final RouteStopMapper stopMapper;
    private final DeliveryOrderMapper orderMapper;
    private final RouteInsertionPlanner insertionPlanner;

    public CourierRoutePlanningService(DeliveryRouteMapper routeMapper,
                                       RouteStopMapper stopMapper,
                                       DeliveryOrderMapper orderMapper,
                                       RouteInsertionPlanner insertionPlanner) {
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.orderMapper = orderMapper;
        this.insertionPlanner = insertionPlanner;
    }

    public RoutePlan planInsertion(long courierId, long currentLocationId,
                                   DeliveryOrder newOrder, LocalDateTime calculatedAt) {
        if (newOrder.getPickupLocationId() == null) {
            throw new IllegalArgumentException("配送订单缺少取件服务站");
        }
        List<RouteStopTask> unfinished = loadUnfinishedTasks(courierId);
        return insertionPlanner.insert(currentLocationId, unfinished,
                new RouteStopTask(newOrder.getId(), newOrder.getPickupLocationId(),
                        RouteStopType.PICKUP),
                new RouteStopTask(newOrder.getId(), newOrder.getDestinationLocationId(),
                        RouteStopType.DROPOFF),
                calculatedAt);
    }

    /** 完成一个节点或确认新位置后，从当前地点重算未完成节点。 */
    public RoutePlan planRemaining(long courierId, long currentLocationId,
                                   LocalDateTime calculatedAt) {
        return insertionPlanner.recalculate(currentLocationId,
                loadUnfinishedTasks(courierId), calculatedAt);
    }

    /** 管理员恢复由原配送员继续配送时，将该订单送达节点重新加入剩余路线。 */
    public RoutePlan planRestoredDelivery(long courierId, long currentLocationId,
                                          DeliveryOrder order, LocalDateTime calculatedAt) {
        List<RouteStopTask> tasks = new ArrayList<>(loadUnfinishedTasks(courierId));
        boolean exists = tasks.stream().anyMatch(task -> task.deliveryOrderId() == order.getId()
                && task.stopType() == RouteStopType.DROPOFF);
        if (!exists) {
            tasks.add(new RouteStopTask(order.getId(), order.getDestinationLocationId(),
                    RouteStopType.DROPOFF));
        }
        return insertionPlanner.recalculate(currentLocationId, tasks, calculatedAt);
    }

    private List<RouteStopTask> loadUnfinishedTasks(long courierId) {
        DeliveryRoute activeRoute = routeMapper.selectActiveByCourierId(courierId);
        if (activeRoute != null) {
            List<RouteStopTask> persisted = stopMapper.selectUnfinishedByRouteId(activeRoute.getId()).stream()
                    .map(this::toTask)
                    .toList();
            if (!persisted.isEmpty()) {
                return persisted;
            }
        }

        // 阶段3以前的历史活动订单可能没有综合路线，按订单状态构造兼容上下文。
        List<RouteStopTask> tasks = new ArrayList<>();
        for (DeliveryOrder order : orderMapper.selectActiveByCourierId(courierId)) {
            if (order.getPickupLocationId() != null
                    && ("ACCEPTED".equals(order.getStatus())
                    || "PICKING_UP".equals(order.getStatus()))) {
                tasks.add(new RouteStopTask(order.getId(), order.getPickupLocationId(),
                        RouteStopType.PICKUP));
            }
            if (List.of("ACCEPTED", "PICKING_UP", "IN_TRANSIT").contains(order.getStatus())) {
                tasks.add(new RouteStopTask(order.getId(), order.getDestinationLocationId(),
                        RouteStopType.DROPOFF));
            }
        }
        return tasks;
    }

    private RouteStopTask toTask(RouteStop stop) {
        return new RouteStopTask(stop.getDeliveryOrderId(), stop.getLocationId(),
                RouteStopType.valueOf(stop.getStopType()));
    }
}
