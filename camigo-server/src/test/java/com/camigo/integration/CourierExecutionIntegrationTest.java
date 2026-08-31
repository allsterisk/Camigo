package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.CamigoApplication;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.RouteStop;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.service.CourierExecutionService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.vo.CourierExecutionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 阶段5真实MySQL验收；位置、订单、包裹和路线变更在测试后回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class CourierExecutionIntegrationTest {

    @Autowired private CourierExecutionService executionService;
    @Autowired private CourierRoutePlanningService planningService;
    @Autowired private RouteVersionService routeVersionService;
    @Autowired private ParcelLifecycleService parcelLifecycleService;
    @Autowired private CourierMapper courierMapper;
    @Autowired private DeliveryOrderMapper orderMapper;
    @Autowired private ParcelMapper parcelMapper;
    @Autowired private DeliveryRouteMapper routeMapper;
    @Autowired private RouteStopMapper stopMapper;

    @Test
    void shouldConfirmLocationBeforeCourierBecomesAvailable() {
        Courier courier = idleCourier();
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, "OFFLINE"));

        var state = executionService.confirmLocation(courier.getUserId(), 28L);

        assertThat(state.locationConfirmed()).isTrue();
        assertThat(state.currentLocationId()).isEqualTo(28L);
        assertThat(state.availabilityStatus()).isEqualTo("AVAILABLE");
        Courier persisted = courierMapper.selectById(courier.getId());
        assertThat(persisted.getCurrentLocationId()).isEqualTo(28L);
        assertThat(persisted.getLocationUpdateTime()).isNotNull();
    }

    @Test
    void shouldKeepLocationOrderParcelAndRouteConsistentAcrossPickupAndArrival() {
        Fixture fixture = prepareAcceptedOrder();
        DeliveryRoute initialRoute = routeMapper.selectActiveByCourierId(fixture.courier().getId());
        List<RouteStop> initialStops = stopMapper.selectByRouteId(initialRoute.getId());
        assertThat(initialStops).extracting(RouteStop::getStopType)
                .startsWith("CURRENT", "PICKUP");
        assertThat(initialStops.get(0).getLocationId())
                .isEqualTo(fixture.order().getPickupLocationId());
        assertThat(initialStops.get(1).getLocationId())
                .isEqualTo(fixture.order().getPickupLocationId());
        assertThat(initialStops.get(1).getStopStatus()).isEqualTo("PENDING");

        CourierExecutionVO pickedUp = executionService.confirmPickup(
                fixture.courier().getUserId(), fixture.order().getId());

        assertThat(pickedUp.orderStatus()).isEqualTo("IN_TRANSIT");
        assertThat(pickedUp.parcelStatus()).isEqualTo("DELIVERING");
        assertThat(pickedUp.currentLocationId()).isEqualTo(fixture.order().getPickupLocationId());
        assertThat(pickedUp.route().steps())
                .allSatisfy(step -> assertThat(step.pickupOrderNos()).isEmpty());
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus())
                .isEqualTo("IN_TRANSIT");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus())
                .isEqualTo("DELIVERING");
        assertThat(stopMapper.selectByRouteId(initialRoute.getId()))
                .filteredOn(stop -> "PICKUP".equals(stop.getStopType()))
                .allSatisfy(stop -> assertThat(stop.getStopStatus()).isEqualTo("COMPLETED"));

        executionService.confirmLocation(fixture.courier().getUserId(),
                fixture.order().getDestinationLocationId());
        DeliveryRoute routeBeforeArrival = routeMapper.selectActiveByCourierId(fixture.courier().getId());
        List<RouteStop> arrivalStops = stopMapper.selectByRouteId(routeBeforeArrival.getId());
        assertThat(arrivalStops).extracting(RouteStop::getStopType)
                .containsExactly("CURRENT", "DROPOFF");
        assertThat(arrivalStops).extracting(RouteStop::getLocationId)
                .containsExactly(fixture.order().getDestinationLocationId(),
                        fixture.order().getDestinationLocationId());
        assertThat(arrivalStops.get(1).getStopStatus()).isEqualTo("PENDING");
        CourierExecutionVO arrived = executionService.confirmArrival(
                fixture.courier().getUserId(), fixture.order().getId());

        assertThat(arrived.orderStatus()).isEqualTo("ARRIVED");
        assertThat(arrived.parcelStatus()).isEqualTo("DELIVERING");
        assertThat(arrived.currentLocationId()).isEqualTo(fixture.order().getDestinationLocationId());
        assertThat(arrived.route().steps()).isEmpty();
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus()).isEqualTo("ARRIVED");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus())
                .isEqualTo("DELIVERING");
        assertThat(courierMapper.selectById(fixture.courier().getId()).getCurrentLocationId())
                .isEqualTo(fixture.order().getDestinationLocationId());
        assertThat(stopMapper.selectByRouteId(routeBeforeArrival.getId()))
                .filteredOn(stop -> "DROPOFF".equals(stop.getStopType()))
                .allSatisfy(stop -> assertThat(stop.getStopStatus()).isEqualTo("ARRIVED"));
        assertThat(routeMapper.selectActiveByCourierId(fixture.courier().getId())).isNull();
    }

    private Fixture prepareAcceptedOrder() {
        Courier courier = idleCourier();
        DeliveryOrder order = orderMapper.selectList(new LambdaQueryWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getStatus, "CREATED")
                        .isNotNull(DeliveryOrder::getPickupLocationId)
                        .orderByAsc(DeliveryOrder::getId))
                .stream().findFirst().orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, "AVAILABLE")
                .set(Courier::getCurrentLocationId, order.getPickupLocationId())
                .set(Courier::getLocationUpdateTime, now));
        courier.setAvailabilityStatus("AVAILABLE");
        courier.setCurrentLocationId(order.getPickupLocationId());
        courier.setLocationUpdateTime(now);
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .set(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "ACCEPTED")
                .set(DeliveryOrder::getAcceptedTime, now));
        order.setAssignedCourierId(courier.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(now);
        ParcelEntity parcel = parcelMapper.selectById(order.getParcelId());
        parcelLifecycleService.changeStatus(parcel, "DELIVERING", courier.getUserId(),
                "COURIER", "阶段5集成验收准备");
        RoutePlan plan = planningService.planRemaining(courier.getId(),
                courier.getCurrentLocationId(), now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);
        return new Fixture(courier, order, parcel);
    }

    private Courier idleCourier() {
        List<Courier> couriers = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getEmploymentStatus, "ACTIVE")
                .orderByAsc(Courier::getId));
        Courier courier = couriers.stream().findFirst()
                .orElseThrow(() -> new AssertionError("缺少阶段5验收配送员"));
        isolateCourier(courier.getId());
        return courier;
    }

    private void isolateCourier(long courierId) {
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getAssignedCourierId, courierId)
                .in(DeliveryOrder::getStatus, "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED")
                .set(DeliveryOrder::getStatus, "CANCELLED"));
        routeMapper.supersedeActive(courierId);
    }

    private record Fixture(Courier courier, DeliveryOrder order, ParcelEntity parcel) {
    }
}
