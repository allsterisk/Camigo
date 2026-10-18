package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.RouteStop;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.vo.CourierBatchPickupVO;
import com.camigo.vo.CourierExecutionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CourierExecutionServiceTest {

    private static final long USER_ID = 8L;
    private static final long COURIER_ID = 2L;
    private static final long STATION_ID = 25L;
    private static final long DORM_ID = 31L;
    private static final long PICKUP_ORDER_ID = 35L;
    private static final long IN_TRANSIT_ORDER_ID = 10L;

    @Mock private CourierMapper courierMapper;
    @Mock private CampusLocationMapper locationMapper;
    @Mock private DeliveryOrderMapper orderMapper;
    @Mock private ParcelMapper parcelMapper;
    @Mock private DeliveryRouteMapper routeMapper;
    @Mock private RouteStopMapper stopMapper;
    @Mock private CourierRoutePlanningService planningService;
    @Mock private RouteVersionService routeVersionService;

    private CourierExecutionService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "courier-execution-test");
        TableInfoHelper.initTableInfo(assistant, Courier.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
        TableInfoHelper.initTableInfo(assistant, ParcelEntity.class);
        TableInfoHelper.initTableInfo(assistant, RouteStop.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryRoute.class);
        service = new CourierExecutionService(courierMapper, locationMapper, orderMapper,
                parcelMapper, routeMapper, stopMapper, planningService, routeVersionService);
    }

    @Test
    void shouldConfirmPickupAtCurrentStationEvenIfNextStopIsDropoff() {
        stubCourierAt(STATION_ID);
        stubPickupOrder();
        stubRouteWithDropoffThenPickup();
        stubSuccessfulPickupWrites();

        CourierExecutionVO result = service.confirmPickup(USER_ID, PICKUP_ORDER_ID);

        assertThat(result.orderStatus()).isEqualTo("IN_TRANSIT");
        assertThat(result.currentLocationId()).isEqualTo(STATION_ID);
    }

    @Test
    void shouldBatchPickupAtCurrentStationEvenIfNextStopIsDropoff() {
        stubCourierAt(STATION_ID);
        stubPickupOrder();
        stubRouteWithDropoffThenPickup();
        stubSuccessfulPickupWrites();

        CourierBatchPickupVO result = service.confirmPickupBatch(USER_ID, List.of(PICKUP_ORDER_ID));

        assertThat(result.pickedCount()).isEqualTo(1);
        assertThat(result.orderIds()).containsExactly(PICKUP_ORDER_ID);
        assertThat(result.pickupLocationId()).isEqualTo(STATION_ID);
    }

    @Test
    void shouldRejectPickupWhenCourierIsNotAtTheStationAndNextStopIsDropoff() {
        stubCourierAt(DORM_ID);
        stubPickupOrder();
        stubRouteWithDropoffThenPickup();
        CampusLocation station = location(STATION_ID, "食堂北快递服务站");
        when(locationMapper.selectById(STATION_ID)).thenReturn(station);

        assertThatThrownBy(() -> service.confirmPickup(USER_ID, PICKUP_ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先到达「食堂北快递服务站」再确认取件")
                .extracting(ex -> ((BusinessException) ex).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    private void stubCourierAt(long locationId) {
        Courier snapshot = courier(locationId);
        Courier locked = courier(locationId);
        when(courierMapper.selectByUserId(USER_ID)).thenReturn(snapshot);
        when(courierMapper.selectByIdForUpdate(COURIER_ID)).thenReturn(locked);
    }

    private void stubPickupOrder() {
        DeliveryOrder order = order(PICKUP_ORDER_ID, "ACCEPTED", STATION_ID, DORM_ID);
        ParcelEntity parcel = parcel();
        when(orderMapper.selectByIdForUpdate(PICKUP_ORDER_ID)).thenReturn(order);
        when(parcelMapper.selectByIdForUpdate(order.getParcelId())).thenReturn(parcel);
    }

    private void stubRouteWithDropoffThenPickup() {
        DeliveryRoute route = activeRoute();
        when(routeMapper.selectActiveByCourierIdForUpdate(COURIER_ID)).thenReturn(route);
        when(stopMapper.selectPendingByRouteIdForUpdate(route.getId())).thenReturn(List.of(
                stop(1L, IN_TRANSIT_ORDER_ID, DORM_ID, "DROPOFF"),
                stop(2L, PICKUP_ORDER_ID, STATION_ID, "PICKUP")));
    }

    private void stubSuccessfulPickupWrites() {
        when(stopMapper.update(any(), any())).thenReturn(1);
        when(orderMapper.update(any(), any())).thenReturn(1);
        when(courierMapper.update(any(), any())).thenReturn(1);
        DeliveryRoute refreshed = activeRoute();
        refreshed.setId(99L);
        when(planningService.planRemaining(eq(COURIER_ID), anyLong(), any(LocalDateTime.class)))
                .thenReturn(new RoutePlan(STATION_ID, List.of(), 0, 0, 0,
                        "campus-dijkstra-insertion-v1", LocalDateTime.now()));
        when(routeVersionService.saveAcceptedPlan(eq(COURIER_ID), any(RoutePlan.class)))
                .thenReturn(refreshed);
        when(stopMapper.selectByRouteId(refreshed.getId())).thenReturn(List.of());
        CampusLocation station = location(STATION_ID, "食堂北快递服务站");
        when(locationMapper.selectById(STATION_ID)).thenReturn(station);
    }

    private Courier courier(long locationId) {
        Courier courier = new Courier();
        courier.setId(COURIER_ID);
        courier.setUserId(USER_ID);
        courier.setEmploymentStatus("ACTIVE");
        courier.setAvailabilityStatus("AVAILABLE");
        courier.setCurrentLocationId(locationId);
        courier.setLocationUpdateTime(LocalDateTime.now());
        return courier;
    }

    private DeliveryOrder order(long id, String status, long pickup, long destination) {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(id);
        order.setOrderNo("DO" + id);
        order.setParcelId(100L + id);
        order.setPickupLocationId(pickup);
        order.setDestinationLocationId(destination);
        order.setAssignedCourierId(COURIER_ID);
        order.setStatus(status);
        return order;
    }

    private ParcelEntity parcel() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(100L + PICKUP_ORDER_ID);
        parcel.setCurrentStatus("DELIVERING");
        return parcel;
    }

    private DeliveryRoute activeRoute() {
        DeliveryRoute route = new DeliveryRoute();
        route.setId(7L);
        route.setCourierId(COURIER_ID);
        route.setStatus("ACTIVE");
        route.setVersionNo(3);
        route.setTotalDistanceMeters(260);
        route.setEstimatedDurationSeconds(300);
        route.setAlgorithmVersion("campus-dijkstra-insertion-v1");
        return route;
    }

    private RouteStop stop(long id, long orderId, long locationId, String type) {
        RouteStop stop = new RouteStop();
        stop.setId(id);
        stop.setDeliveryRouteId(7L);
        stop.setDeliveryOrderId(orderId);
        stop.setLocationId(locationId);
        stop.setStopType(type);
        stop.setStopStatus("PENDING");
        return stop;
    }

    private CampusLocation location(long id, String name) {
        CampusLocation location = new CampusLocation();
        location.setId(id);
        location.setLocationName(name);
        location.setStatus("ACTIVE");
        return location;
    }
}
