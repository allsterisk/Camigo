package com.camigo.routing;

import com.camigo.config.RoutingProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 带先取后送约束的最小新增距离插入算法。 */
@Service
public class RouteInsertionPlanner {

    private final RouteScheduleEstimator estimator;
    private final RoutingProperties properties;

    public RouteInsertionPlanner(RouteScheduleEstimator estimator, RoutingProperties properties) {
        this.estimator = estimator;
        this.properties = properties;
    }

    public RoutePlan insert(long currentLocationId,
                            List<RouteStopTask> unfinishedTasks,
                            RouteStopTask pickup,
                            RouteStopTask dropoff,
                            LocalDateTime calculatedAt) {
        validateNewPair(pickup, dropoff);
        List<RouteStopTask> existing = List.copyOf(unfinishedTasks);
        validateExistingOrder(existing);
        if (existing.size() + 2 > properties.maxActiveOrders() * 2) {
            throw new IllegalArgumentException("未完成取送节点超过配送员容量");
        }
        if (existing.stream().anyMatch(task -> task.deliveryOrderId() == pickup.deliveryOrderId())) {
            throw new IllegalArgumentException("新订单已存在于当前路线");
        }

        int originalDistance = estimator.estimate(currentLocationId, existing, calculatedAt)
                .totalDistanceMeters();
        List<RouteStopTask> best = null;
        int bestDistance = Integer.MAX_VALUE;
        int bestClusterScore = -1;
        int bestPickupIndex = Integer.MAX_VALUE;
        int bestDropoffIndex = Integer.MAX_VALUE;

        for (int pickupIndex = 0; pickupIndex <= existing.size(); pickupIndex++) {
            List<RouteStopTask> withPickup = new ArrayList<>(existing);
            withPickup.add(pickupIndex, pickup);
            for (int dropoffIndex = pickupIndex + 1;
                 dropoffIndex <= withPickup.size(); dropoffIndex++) {
                List<RouteStopTask> candidate = new ArrayList<>(withPickup);
                candidate.add(dropoffIndex, dropoff);
                int candidateDistance = estimator.estimate(currentLocationId, candidate, calculatedAt)
                        .totalDistanceMeters();
                int clusterScore = samePickupClusterScore(candidate, pickup);
                if (candidateDistance < bestDistance
                        || (candidateDistance == bestDistance && clusterScore > bestClusterScore)
                        || (candidateDistance == bestDistance
                        && clusterScore == bestClusterScore
                        && (pickupIndex < bestPickupIndex
                        || (pickupIndex == bestPickupIndex && dropoffIndex < bestDropoffIndex)))) {
                    best = candidate;
                    bestDistance = candidateDistance;
                    bestClusterScore = clusterScore;
                    bestPickupIndex = pickupIndex;
                    bestDropoffIndex = dropoffIndex;
                }
            }
        }
        RouteScheduleEstimator.ScheduleResult schedule = estimator.estimate(
                currentLocationId, best, calculatedAt);
        return new RoutePlan(currentLocationId, schedule.stops(), schedule.totalDistanceMeters(),
                schedule.estimatedDurationSeconds(), bestDistance - originalDistance,
                properties.algorithmVersion(), calculatedAt);
    }

    public RoutePlan recalculate(long currentLocationId, List<RouteStopTask> unfinishedTasks,
                                 LocalDateTime calculatedAt) {
        List<RouteStopTask> tasks = promoteCurrentStationPickups(currentLocationId,
                List.copyOf(unfinishedTasks));
        validateExistingOrder(tasks);
        if (tasks.size() > properties.maxActiveOrders() * 2) {
            throw new IllegalArgumentException("未完成取送节点超过配送员容量");
        }
        RouteScheduleEstimator.ScheduleResult schedule = estimator.estimate(
                currentLocationId, tasks, calculatedAt);
        return new RoutePlan(currentLocationId, schedule.stops(), schedule.totalDistanceMeters(),
                schedule.estimatedDurationSeconds(), 0, properties.algorithmVersion(), calculatedAt);
    }

    /** 配送员已在某快递站时，将该站剩余取件提前到送达之前，便于一次取齐。 */
    private List<RouteStopTask> promoteCurrentStationPickups(long currentLocationId,
                                                             List<RouteStopTask> tasks) {
        List<RouteStopTask> pickupsHere = new ArrayList<>();
        List<RouteStopTask> rest = new ArrayList<>();
        for (RouteStopTask task : tasks) {
            if (task.stopType() == RouteStopType.PICKUP && task.locationId() == currentLocationId) {
                pickupsHere.add(task);
            } else {
                rest.add(task);
            }
        }
        if (pickupsHere.isEmpty()) {
            return tasks;
        }
        List<RouteStopTask> promoted = new ArrayList<>(pickupsHere.size() + rest.size());
        promoted.addAll(pickupsHere);
        promoted.addAll(rest);
        return promoted;
    }

    private int samePickupClusterScore(List<RouteStopTask> candidate, RouteStopTask pickup) {
        int index = -1;
        for (int i = 0; i < candidate.size(); i++) {
            RouteStopTask task = candidate.get(i);
            if (task.deliveryOrderId() == pickup.deliveryOrderId()
                    && task.stopType() == RouteStopType.PICKUP) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return 0;
        }
        int score = 0;
        if (index > 0) {
            RouteStopTask previous = candidate.get(index - 1);
            if (previous.stopType() == RouteStopType.PICKUP
                    && previous.locationId() == pickup.locationId()) {
                score += 2;
            }
        }
        if (index + 1 < candidate.size()) {
            RouteStopTask next = candidate.get(index + 1);
            if (next.stopType() == RouteStopType.PICKUP
                    && next.locationId() == pickup.locationId()) {
                score += 2;
            }
        }
        return score;
    }

    private void validateNewPair(RouteStopTask pickup, RouteStopTask dropoff) {
        if (pickup.stopType() != RouteStopType.PICKUP
                || dropoff.stopType() != RouteStopType.DROPOFF
                || pickup.deliveryOrderId() != dropoff.deliveryOrderId()) {
            throw new IllegalArgumentException("新订单必须包含同一订单的先取后送节点");
        }
    }

    private void validateExistingOrder(List<RouteStopTask> tasks) {
        Map<Long, Integer> pickupIndexes = new HashMap<>();
        Set<String> uniqueStops = new HashSet<>();
        for (int index = 0; index < tasks.size(); index++) {
            RouteStopTask task = tasks.get(index);
            String key = task.deliveryOrderId() + ":" + task.stopType();
            if (!uniqueStops.add(key)) {
                throw new IllegalArgumentException("同一订单存在重复停靠类型");
            }
            if (task.stopType() == RouteStopType.PICKUP) {
                pickupIndexes.put(task.deliveryOrderId(), index);
            } else {
                Integer pickupIndex = pickupIndexes.get(task.deliveryOrderId());
                boolean pickupAppearsLater = false;
                for (int later = index + 1; later < tasks.size(); later++) {
                    RouteStopTask candidate = tasks.get(later);
                    if (candidate.deliveryOrderId() == task.deliveryOrderId()
                            && candidate.stopType() == RouteStopType.PICKUP) {
                        pickupAppearsLater = true;
                        break;
                    }
                }
                if (pickupIndex == null && pickupAppearsLater) {
                    throw new IllegalArgumentException("现有路线违反先取后送约束");
                }
            }
        }
    }
}
