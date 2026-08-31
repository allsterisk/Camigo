package com.camigo.routing;

import com.camigo.config.RoutingProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 依据最短路、5km/h速度和分组操作耗时生成ETA。 */
@Component
public class RouteScheduleEstimator {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final BigDecimal METERS_PER_KILOMETER = BigDecimal.valueOf(1000);

    private final ShortestPathProvider pathProvider;
    private final RoutingProperties properties;

    public RouteScheduleEstimator(ShortestPathProvider pathProvider, RoutingProperties properties) {
        this.pathProvider = pathProvider;
        this.properties = properties;
    }

    public ScheduleResult estimate(long currentLocationId, List<RouteStopTask> tasks,
                                   LocalDateTime calculatedAt) {
        List<RouteStopTask> immutableTasks = List.copyOf(tasks);
        List<ScheduledRouteStop> scheduled = new ArrayList<>(immutableTasks.size());
        long totalDistance = 0;
        long totalSeconds = 0;
        long previousLocationId = currentLocationId;
        LocalDateTime cursor = calculatedAt;

        int index = 0;
        while (index < immutableTasks.size()) {
            RouteStopTask first = immutableTasks.get(index);
            int groupEnd = index + 1;
            while (groupEnd < immutableTasks.size()
                    && isSameOperationGroup(first, immutableTasks.get(groupEnd))) {
                groupEnd++;
            }

            // CURRENT 与下一业务节点可以位于同一地点；节点仍分别存在，但该段无需寻路。
            int legDistance = previousLocationId == first.locationId()
                    ? 0
                    : pathProvider.shortestPath(previousLocationId,
                            first.locationId()).distanceMeters();
            int travelSeconds = travelSeconds(legDistance);
            totalDistance = Math.addExact(totalDistance, legDistance);
            totalSeconds = Math.addExact(totalSeconds, travelSeconds);
            LocalDateTime arrival = cursor.plusSeconds(travelSeconds);
            for (int taskIndex = index; taskIndex < groupEnd; taskIndex++) {
                scheduled.add(new ScheduledRouteStop(immutableTasks.get(taskIndex), arrival));
            }

            int operationSeconds = operationSeconds(first.stopType(), groupEnd - index);
            totalSeconds = Math.addExact(totalSeconds, operationSeconds);
            cursor = arrival.plusSeconds(operationSeconds);
            previousLocationId = first.locationId();
            index = groupEnd;
        }
        if (totalDistance > Integer.MAX_VALUE || totalSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("路线距离或预计耗时超出可保存范围");
        }
        return new ScheduleResult(scheduled, (int) totalDistance, (int) totalSeconds);
    }

    public int travelSeconds(int distanceMeters) {
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("行驶距离不能为负数");
        }
        BigDecimal numerator = BigDecimal.valueOf(distanceMeters).multiply(SECONDS_PER_HOUR);
        BigDecimal denominator = BigDecimal.valueOf(properties.speedKilometersPerHour())
                .multiply(METERS_PER_KILOMETER);
        return numerator.divide(denominator, 0, RoundingMode.CEILING).intValueExact();
    }

    private int operationSeconds(RouteStopType type, int parcelCount) {
        return switch (type) {
            case PICKUP -> Math.addExact(properties.stationBaseSeconds(),
                    Math.multiplyExact(properties.pickupPerParcelSeconds(), parcelCount));
            case DROPOFF -> Math.addExact(properties.destinationBaseSeconds(),
                    Math.multiplyExact(properties.storagePerParcelSeconds(), parcelCount));
        };
    }

    private boolean isSameOperationGroup(RouteStopTask first, RouteStopTask candidate) {
        return first.locationId() == candidate.locationId()
                && first.stopType() == candidate.stopType();
    }

    public record ScheduleResult(List<ScheduledRouteStop> stops,
                                 int totalDistanceMeters,
                                 int estimatedDurationSeconds) {

        public ScheduleResult {
            stops = List.copyOf(stops);
        }
    }
}
