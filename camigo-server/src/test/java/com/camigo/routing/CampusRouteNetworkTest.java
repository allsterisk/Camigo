package com.camigo.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camigo.entity.CampusRouteEdge;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CampusRouteEdgeMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CampusRouteNetworkTest {

    @Test
    void shouldWarmAllPairsAndReloadAfterInvalidation() {
        CampusLocationMapper locationMapper = mock(CampusLocationMapper.class);
        CampusRouteEdgeMapper edgeMapper = mock(CampusRouteEdgeMapper.class);
        when(locationMapper.selectActiveLocationIds()).thenReturn(List.of(1L, 2L, 3L));
        when(edgeMapper.selectActiveEdges()).thenReturn(List.of(edge(1, 2, 20), edge(2, 3, 30)));
        CampusRouteNetwork network = new CampusRouteNetwork(locationMapper, edgeMapper);

        network.refresh();

        assertThat(network.cachedPathCount()).isEqualTo(9);
        assertThat(network.shortestPath(1, 3).distanceMeters()).isEqualTo(50);
        assertThat(network.shortestPath(1, 3).locationIds()).containsExactly(1L, 2L, 3L);
        network.invalidate();
        assertThat(network.cachedPathCount()).isZero();
        assertThat(network.shortestPath(3, 1).distanceMeters()).isEqualTo(50);
        verify(locationMapper, times(2)).selectActiveLocationIds();
    }

    private CampusRouteEdge edge(long from, long to, int distance) {
        CampusRouteEdge edge = new CampusRouteEdge();
        edge.setFromLocationId(from);
        edge.setToLocationId(to);
        edge.setDistanceMeters(distance);
        edge.setBidirectional(1);
        return edge;
    }
}
