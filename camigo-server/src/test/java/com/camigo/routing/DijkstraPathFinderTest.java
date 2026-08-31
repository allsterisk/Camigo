package com.camigo.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DijkstraPathFinderTest {

    @Test
    void shouldReturnShortestDistanceAndEveryPassedLocation() {
        DijkstraPathFinder finder = new DijkstraPathFinder(List.of(1L, 2L, 3L, 4L), List.of(
                new GraphEdge(1, 2, 100, true),
                new GraphEdge(2, 3, 80, true),
                new GraphEdge(1, 3, 250, true),
                new GraphEdge(3, 4, 20, true)));

        ShortestPath result = finder.shortestPath(1, 4);

        assertThat(result.distanceMeters()).isEqualTo(200);
        assertThat(result.locationIds()).containsExactly(1L, 2L, 3L, 4L);
        assertThat(finder.shortestPath(4, 1).distanceMeters()).isEqualTo(200);
        assertThat(finder.shortestPath(2, 2).locationIds()).containsExactly(2L);
    }

    @Test
    void shouldRespectOneWayEdges() {
        DijkstraPathFinder finder = new DijkstraPathFinder(List.of(1L, 2L),
                List.of(new GraphEdge(1, 2, 10, false)));

        assertThat(finder.shortestPath(1, 2).distanceMeters()).isEqualTo(10);
        assertThatThrownBy(() -> finder.shortestPath(2, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不可达");
    }

    @Test
    void shouldRejectUnknownLocation() {
        DijkstraPathFinder finder = new DijkstraPathFinder(List.of(1L, 2L),
                List.of(new GraphEdge(1, 2, 10, true)));

        assertThatThrownBy(() -> finder.shortestPath(1, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在或未启用");
    }
}
