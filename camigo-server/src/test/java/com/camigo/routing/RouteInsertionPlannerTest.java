package com.camigo.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.camigo.config.RoutingProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteInsertionPlannerTest {

    private final RoutingProperties properties = new RoutingProperties(
            5, 120, 30, 30, 60, 8, "dijkstra-insertion-v1");
    private final ShortestPathProvider lineDistance = (from, to) ->
            new ShortestPath(Math.toIntExact(Math.abs(to - from) * 10), List.of(from, to));
    private final RouteInsertionPlanner planner = new RouteInsertionPlanner(
            new RouteScheduleEstimator(lineDistance, properties), properties);

    @Test
    void shouldChooseMinimumIncrementWhileKeepingPickupBeforeDropoff() {
        List<RouteStopTask> existing = List.of(
                new RouteStopTask(1, 4, RouteStopType.PICKUP),
                new RouteStopTask(1, 8, RouteStopType.DROPOFF));
        RouteStopTask pickup = new RouteStopTask(2, 2, RouteStopType.PICKUP);
        RouteStopTask dropoff = new RouteStopTask(2, 6, RouteStopType.DROPOFF);

        RoutePlan result = planner.insert(1, existing, pickup, dropoff,
                LocalDateTime.of(2026, 8, 31, 9, 0));

        assertThat(result.orderedTasks()).containsExactly(
                pickup, existing.get(0), dropoff, existing.get(1));
        assertThat(result.totalDistanceMeters()).isEqualTo(70);
        assertThat(result.incrementalDistanceMeters()).isZero();
        assertThat(result.algorithmVersion()).isEqualTo("dijkstra-insertion-v1");
    }

    @Test
    void shouldRejectASeventeenthUnfinishedNode() {
        List<RouteStopTask> existing = new ArrayList<>();
        for (int order = 1; order <= 15; order++) {
            existing.add(new RouteStopTask(order, order + 1L, RouteStopType.DROPOFF));
        }

        assertThatThrownBy(() -> planner.insert(1, existing,
                new RouteStopTask(100, 20, RouteStopType.PICKUP),
                new RouteStopTask(100, 21, RouteStopType.DROPOFF),
                LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超过配送员容量");
    }

    @Test
    void shouldPlanAtTheEightOrderBoundary() {
        List<RouteStopTask> existing = new ArrayList<>();
        for (int order = 1; order <= 7; order++) {
            existing.add(new RouteStopTask(order, order + 1L, RouteStopType.PICKUP));
        }
        for (int order = 1; order <= 7; order++) {
            existing.add(new RouteStopTask(order, order + 8L, RouteStopType.DROPOFF));
        }
        RouteStopTask pickup = new RouteStopTask(8, 16, RouteStopType.PICKUP);
        RouteStopTask dropoff = new RouteStopTask(8, 17, RouteStopType.DROPOFF);

        RoutePlan result = planner.insert(1, existing, pickup, dropoff, LocalDateTime.now());

        assertThat(result.stops()).hasSize(16);
        assertThat(result.orderedTasks().indexOf(pickup))
                .isLessThan(result.orderedTasks().indexOf(dropoff));
    }

    @Test
    void shouldClusterSameStationPickupsWhenDistanceIsEqual() {
        List<RouteStopTask> existing = List.of(
                new RouteStopTask(1, 5, RouteStopType.PICKUP),
                new RouteStopTask(1, 9, RouteStopType.DROPOFF));
        RouteStopTask pickup = new RouteStopTask(2, 5, RouteStopType.PICKUP);
        RouteStopTask dropoff = new RouteStopTask(2, 7, RouteStopType.DROPOFF);

        RoutePlan result = planner.insert(1, existing, pickup, dropoff,
                LocalDateTime.of(2026, 8, 31, 9, 0));

        assertThat(result.orderedTasks().subList(0, 2))
                .extracting(RouteStopTask::locationId, RouteStopTask::stopType)
                .containsExactly(
                        tuple(5L, RouteStopType.PICKUP),
                        tuple(5L, RouteStopType.PICKUP));
        assertThat(result.orderedTasks()).contains(pickup, dropoff, existing.get(0), existing.get(1));
        assertThat(result.orderedTasks().indexOf(pickup))
                .isLessThan(result.orderedTasks().indexOf(dropoff));
        assertThat(result.orderedTasks().indexOf(existing.get(0)))
                .isLessThan(result.orderedTasks().indexOf(existing.get(1)));
    }

    @Test
    void shouldRejectExistingDropoffBeforeItsPendingPickup() {
        List<RouteStopTask> invalid = List.of(
                new RouteStopTask(1, 8, RouteStopType.DROPOFF),
                new RouteStopTask(1, 4, RouteStopType.PICKUP));

        assertThatThrownBy(() -> planner.recalculate(1, invalid, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("先取后送");
    }
}
