package com.camigo.routing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** 可用于派单评分或持久化的完整路线计算结果。 */
public record RoutePlan(long currentLocationId,
                        List<ScheduledRouteStop> stops,
                        int totalDistanceMeters,
                        int estimatedDurationSeconds,
                        int incrementalDistanceMeters,
                        String algorithmVersion,
                        LocalDateTime calculatedAt) {

    public RoutePlan {
        if (currentLocationId <= 0 || totalDistanceMeters < 0
                || estimatedDurationSeconds < 0 || incrementalDistanceMeters < 0) {
            throw new IllegalArgumentException("路线计算结果无效");
        }
        stops = List.copyOf(stops);
        Objects.requireNonNull(algorithmVersion, "算法版本不能为空");
        Objects.requireNonNull(calculatedAt, "计算时间不能为空");
    }

    public List<RouteStopTask> orderedTasks() {
        return stops.stream().map(ScheduledRouteStop::task).toList();
    }
}
