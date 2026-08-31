package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.CamigoApplication;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.dto.ParcelExceptionReportDTO;
import com.camigo.dto.ParcelExceptionResolutionDTO;
import com.camigo.dto.ParcelStatusUpdateDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.ParcelExceptionReportMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.service.CourierExecutionService;
import com.camigo.service.ParcelExceptionService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.service.ParcelService;
import com.camigo.exception.BusinessException;
import com.camigo.vo.ParcelExceptionVO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 阶段7真实MySQL验收；每个用例结束后整体回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class ParcelExceptionWorkflowIntegrationTest {
    @Autowired private ParcelExceptionService exceptionService;
    @Autowired private CourierExecutionService executionService;
    @Autowired private CourierRoutePlanningService planningService;
    @Autowired private RouteVersionService routeVersionService;
    @Autowired private ParcelLifecycleService lifecycleService;
    @Autowired private ParcelService parcelService;
    @Autowired private CourierMapper courierMapper;
    @Autowired private DeliveryOrderMapper orderMapper;
    @Autowired private ParcelMapper parcelMapper;
    @Autowired private DeliveryRouteMapper routeMapper;
    @Autowired private RouteStopMapper stopMapper;
    @Autowired private ParcelExceptionReportMapper exceptionMapper;
    @Autowired private UserMapper userMapper;

    @Test
    void shouldPauseAbnormalParcelAndRemoveItFromRoute() {
        Fixture fixture = prepareInTransitOrder();
        ParcelExceptionVO report = report(fixture);

        assertThat(report.status()).isEqualTo("PENDING");
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus()).isEqualTo("FAILED");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus()).isEqualTo("EXCEPTION");
        DeliveryRoute active = routeMapper.selectActiveByCourierId(fixture.courier().getId());
        assertThat(active).isNull();
        assertThat(orderMapper.countActiveByCourierId(fixture.courier().getId())).isZero();
        assertThatThrownBy(() -> parcelService.updateStatus(activeAdminId(), fixture.parcel().getId(),
                new ParcelStatusUpdateDTO("DELIVERING", "尝试绕过异常处理")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能由管理员通过异常处理接口");
    }

    @Test
    void shouldResumeDeliveryAndRestoreDropoffRoute() {
        Fixture fixture = prepareInTransitOrder();
        ParcelExceptionVO report = report(fixture);
        ParcelExceptionVO resolved = exceptionService.resolve(activeAdminId(), report.id(),
                new ParcelExceptionResolutionDTO("RESUME_DELIVERY", "收件信息已经核实"));

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.resolutionType()).isEqualTo("RESUME_DELIVERY");
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus()).isEqualTo("DELIVERING");
        DeliveryRoute active = routeMapper.selectActiveByCourierId(fixture.courier().getId());
        assertThat(active).isNotNull();
        assertThat(stopMapper.selectUnfinishedByRouteId(active.getId()))
                .anyMatch(stop -> stop.getDeliveryOrderId().equals(fixture.order().getId())
                        && "DROPOFF".equals(stop.getStopType()));
    }

    @Test
    void shouldReturnToSortingAndUpdateCourierLocation() {
        Fixture fixture = prepareInTransitOrder();
        ParcelExceptionVO report = report(fixture);
        ParcelExceptionVO resolved = exceptionService.resolve(activeAdminId(), report.id(),
                new ParcelExceptionResolutionDTO("RETURN_TO_SORTING", "包裹已交回服务站复核"));

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus()).isEqualTo("SORTING");
        assertThat(courierMapper.selectById(fixture.courier().getId()).getCurrentLocationId())
                .isEqualTo(fixture.order().getPickupLocationId());
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldConfirmReturnedAndKeepCompleteAuditRecord() {
        Fixture fixture = prepareInTransitOrder();
        ParcelExceptionVO report = report(fixture);
        long adminId = activeAdminId();
        ParcelExceptionVO returned = exceptionService.resolve(adminId, report.id(),
                new ParcelExceptionResolutionDTO("CONFIRM_RETURNED", "已核对包裹并完成退回交接"));

        assertThat(returned.status()).isEqualTo("RETURNED");
        assertThat(returned.handledByUserId()).isEqualTo(adminId);
        assertThat(returned.handledTime()).isNotNull();
        assertThat(returned.resolutionNote()).isEqualTo("已核对包裹并完成退回交接");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus()).isEqualTo("RETURNED");
        assertThat(exceptionMapper.selectById(report.id()).getPendingParcelId()).isNull();
    }

    private ParcelExceptionVO report(Fixture fixture) {
        return exceptionService.report(fixture.courier().getUserId(), fixture.order().getId(),
                new ParcelExceptionReportDTO("USER_UNREACHABLE", "多次联系收件人均未接听"));
    }

    private Fixture prepareInTransitOrder() {
        Courier courier = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                        .eq(Courier::getEmploymentStatus, "ACTIVE").orderByAsc(Courier::getId))
                .stream()
                .findFirst().orElseThrow();
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId())
                .in(DeliveryOrder::getStatus, "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED")
                .set(DeliveryOrder::getStatus, "CANCELLED"));
        routeMapper.supersedeActive(courier.getId());
        DeliveryOrder order = orderMapper.selectList(new LambdaQueryWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getStatus, "CREATED").isNotNull(DeliveryOrder::getPickupLocationId)
                        .orderByAsc(DeliveryOrder::getId)).stream().findFirst().orElseThrow();
        ParcelEntity parcel = parcelMapper.selectById(order.getParcelId());
        LocalDateTime now = LocalDateTime.now();
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId()).set(Courier::getAvailabilityStatus, "AVAILABLE")
                .set(Courier::getCurrentLocationId, order.getPickupLocationId())
                .set(Courier::getLocationUpdateTime, now));
        courier.setAvailabilityStatus("AVAILABLE"); courier.setCurrentLocationId(order.getPickupLocationId()); courier.setLocationUpdateTime(now);
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId()).set(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "ACCEPTED").set(DeliveryOrder::getAcceptedTime, now));
        order.setAssignedCourierId(courier.getId()); order.setStatus("ACCEPTED"); order.setAcceptedTime(now);
        lifecycleService.changeStatus(parcel, "DELIVERING", courier.getUserId(), "COURIER", "阶段7集成验收准备");
        RoutePlan plan = planningService.planRemaining(courier.getId(), courier.getCurrentLocationId(), now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);
        executionService.confirmPickup(courier.getUserId(), order.getId());
        order.setStatus("IN_TRANSIT");
        return new Fixture(courier, order, parcel);
    }

    private record Fixture(Courier courier, DeliveryOrder order, ParcelEntity parcel) {}

    private long activeAdminId() {
        return userMapper.selectList(new LambdaQueryWrapper<com.camigo.entity.User>()
                        .eq(com.camigo.entity.User::getStatus, "ACTIVE"))
                .stream()
                .filter(user -> userMapper.selectRoleCodes(user.getId()).contains("ADMIN"))
                .mapToLong(com.camigo.entity.User::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少阶段7验收管理员"));
    }
}
