package com.camigo.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.config.RoutingProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RouteScheduleEstimatorTest {

    private final RoutingProperties properties = new RoutingProperties(
            5, 120, 30, 30, 60, 8, "dijkstra-insertion-v1");
    private final ShortestPathProvider lineDistance = (from, to) ->
            new ShortestPath(Math.toIntExact(Math.abs(to - from) * 100), List.of(from, to));
    private final RouteScheduleEstimator estimator =
            new RouteScheduleEstimator(lineDistance, properties);

    @Test
    void shouldUseFiveKilometersPerHourAndGroupedOperationTime() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 9, 0);
        List<RouteStopTask> tasks = List.of(
                new RouteStopTask(10, 2, RouteStopType.PICKUP),
                new RouteStopTask(11, 2, RouteStopType.PICKUP),
                new RouteStopTask(10, 3, RouteStopType.DROPOFF));

        RouteScheduleEstimator.ScheduleResult result = estimator.estimate(1, tasks, start);

        assertThat(estimator.travelSeconds(100)).isEqualTo(72);
        assertThat(result.totalDistanceMeters()).isEqualTo(200);
        // 行驶144秒 + 服务站基础120秒 + 两件取件60秒 + 目的地基础30秒 + 入柜60秒。
        assertThat(result.estimatedDurationSeconds()).isEqualTo(414);
        assertThat(result.stops().get(0).estimatedArrivalTime()).isEqualTo(start.plusSeconds(72));
        assertThat(result.stops().get(1).estimatedArrivalTime()).isEqualTo(start.plusSeconds(72));
        assertThat(result.stops().get(2).estimatedArrivalTime()).isEqualTo(start.plusSeconds(324));
    }

    @Test
    void shouldRoundEachTravelLegUpToWholeSecond() {
        assertThat(estimator.travelSeconds(1)).isEqualTo(1);
        assertThat(estimator.travelSeconds(150)).isEqualTo(108);
    }

    @Test
    void shouldKeepSameLocationBusinessStopAndTreatItsLegAsZeroDistance() {
        AtomicInteger pathQueries = new AtomicInteger();
        ShortestPathProvider provider = (from, to) -> {
            pathQueries.incrementAndGet();
            return new ShortestPath(Math.toIntExact(Math.abs(to - from) * 100),
                    List.of(from, to));
        };
        RouteScheduleEstimator sameLocationEstimator =
                new RouteScheduleEstimator(provider, properties);
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 10, 0);
        List<RouteStopTask> tasks = List.of(
                new RouteStopTask(20, 2, RouteStopType.PICKUP),
                new RouteStopTask(20, 3, RouteStopType.DROPOFF));

        RouteScheduleEstimator.ScheduleResult result =
                sameLocationEstimator.estimate(2, tasks, start);

        assertThat(result.stops()).hasSize(2);
        assertThat(result.stops().get(0).task().stopType()).isEqualTo(RouteStopType.PICKUP);
        assertThat(result.stops().get(0).estimatedArrivalTime()).isEqualTo(start);
        assertThat(result.totalDistanceMeters()).isEqualTo(100);
        assertThat(pathQueries).hasValue(1);
    }
}
