package com.camigo.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.RouteStop;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.RouteStopMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteVersionServiceTest {

    @Mock
    private CourierMapper courierMapper;
    @Mock
    private DeliveryRouteMapper routeMapper;
    @Mock
    private RouteStopMapper stopMapper;
    private RouteVersionService service;

    @BeforeEach
    void setUp() {
        service = new RouteVersionService(courierMapper, routeMapper, stopMapper);
    }

    @Test
    void shouldSupersedeOldRouteAndPersistCurrentPlusEveryOrderStop() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 9, 0);
        Courier courier = courier(7, 1);
        when(courierMapper.selectByIdForUpdate(7L)).thenReturn(courier);
        when(routeMapper.selectLatestVersion(7L)).thenReturn(3);
        when(routeMapper.insert(any(DeliveryRoute.class))).thenAnswer(invocation -> {
            DeliveryRoute route = invocation.getArgument(0);
            route.setId(99L);
            return 1;
        });
        RoutePlan plan = new RoutePlan(1, List.of(
                new ScheduledRouteStop(new RouteStopTask(10, 2, RouteStopType.PICKUP),
                        now.plusMinutes(3)),
                new ScheduledRouteStop(new RouteStopTask(10, 3, RouteStopType.DROPOFF),
                        now.plusMinutes(8))),
                200, 600, 40, "dijkstra-insertion-v1", now);

        DeliveryRoute saved = service.saveAcceptedPlan(7, plan);

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getVersionNo()).isEqualTo(4);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        verify(routeMapper).supersedeActive(7L);
        ArgumentCaptor<RouteStop> stopCaptor = ArgumentCaptor.forClass(RouteStop.class);
        verify(stopMapper, org.mockito.Mockito.times(3)).insert(stopCaptor.capture());
        assertThat(stopCaptor.getAllValues()).extracting(RouteStop::getStopType)
                .containsExactly("CURRENT", "PICKUP", "DROPOFF");
        assertThat(stopCaptor.getAllValues()).extracting(RouteStop::getSequenceNo)
                .containsExactly(1, 2, 3);
        assertThat(stopCaptor.getAllValues().get(0).getDeliveryOrderId()).isNull();
    }

    @Test
    void shouldRejectStalePlanBeforeWritingRoute() {
        Courier courier = courier(7, 2);
        when(courierMapper.selectByIdForUpdate(7L)).thenReturn(courier);
        RoutePlan stale = new RoutePlan(1, List.of(), 0, 0, 0,
                "dijkstra-insertion-v1", LocalDateTime.now());

        assertThatThrownBy(() -> service.saveAcceptedPlan(7, stale))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前位置已变化");
        verify(routeMapper, never()).insert(any(DeliveryRoute.class));
        verify(stopMapper, never()).insert(any(RouteStop.class));
    }

    @Test
    void shouldPersistCurrentAndBusinessStopSeparatelyWhenLocationsMatch() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 30);
        when(courierMapper.selectByIdForUpdate(7L)).thenReturn(courier(7, 2));
        when(routeMapper.selectLatestVersion(7L)).thenReturn(4);
        when(routeMapper.insert(any(DeliveryRoute.class))).thenAnswer(invocation -> {
            DeliveryRoute route = invocation.getArgument(0);
            route.setId(100L);
            return 1;
        });
        RoutePlan plan = new RoutePlan(2, List.of(
                new ScheduledRouteStop(new RouteStopTask(20, 2, RouteStopType.PICKUP), now)),
                0, 150, 0, "dijkstra-insertion-v1", now);

        service.saveAcceptedPlan(7, plan);

        ArgumentCaptor<RouteStop> stopCaptor = ArgumentCaptor.forClass(RouteStop.class);
        verify(stopMapper, org.mockito.Mockito.times(2)).insert(stopCaptor.capture());
        assertThat(stopCaptor.getAllValues()).extracting(RouteStop::getStopType)
                .containsExactly("CURRENT", "PICKUP");
        assertThat(stopCaptor.getAllValues()).extracting(RouteStop::getLocationId)
                .containsExactly(2L, 2L);
        assertThat(stopCaptor.getAllValues()).extracting(RouteStop::getSequenceNo)
                .containsExactly(1, 2);
        assertThat(stopCaptor.getAllValues().get(0).getStopStatus()).isEqualTo("COMPLETED");
        assertThat(stopCaptor.getAllValues().get(1).getStopStatus()).isEqualTo("PENDING");
    }

    private Courier courier(long id, long locationId) {
        Courier courier = new Courier();
        courier.setId(id);
        courier.setCurrentLocationId(locationId);
        return courier;
    }
}
