package com.camigo.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camigo.config.DispatchProperties;
import com.camigo.dto.DispatchRejectDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryAssignment;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryAssignmentMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.service.ParcelLifecycleService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntelligentDispatchServiceTest {

    @Mock private DeliveryOrderMapper orderMapper;
    @Mock private DeliveryAssignmentMapper assignmentMapper;
    @Mock private CourierMapper courierMapper;
    @Mock private ParcelMapper parcelMapper;
    @Mock private CourierRoutePlanningService routePlanningService;
    @Mock private DispatchScoringService scoringService;
    @Mock private CourierRejectionCooldownService cooldownService;
    @Mock private RouteVersionService routeVersionService;
    @Mock private ParcelLifecycleService parcelLifecycleService;

    private IntelligentDispatchService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "dispatch-service-test");
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryAssignment.class);
        TableInfoHelper.initTableInfo(assistant, Courier.class);
        DispatchProperties properties = new DispatchProperties(180, 10, 3, 15,
                40, 35, 25, 500, "dispatch-score-v1");
        service = new IntelligentDispatchService(orderMapper, assignmentMapper, courierMapper,
                parcelMapper, routePlanningService, scoringService, cooldownService,
                routeVersionService, parcelLifecycleService, properties);
    }

    @Test
    void shouldScoreCandidatesAndOfferHighestEligibleCourier() {
        DeliveryOrder order = order("CREATED");
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(assignmentMapper.selectCount(any())).thenReturn(0L);
        DispatchCandidate candidate = new DispatchCandidate(3L, 8L, 25L, 2,
                new BigDecimal("4.50"));
        when(assignmentMapper.selectEligibleCandidates(eq(5L), any()))
                .thenReturn(List.of(candidate));
        when(cooldownService.isCoolingDown(eq(3L), any())).thenReturn(false);
        RoutePlan plan = new RoutePlan(25, List.of(), 0, 0, 200,
                "dijkstra-insertion-v1", LocalDateTime.now());
        when(routePlanningService.planInsertion(eq(3L), eq(25L), eq(order), any()))
                .thenReturn(plan);
        DispatchScore score = new DispatchScore(new BigDecimal("71.4286"),
                new BigDecimal("75.0000"), new BigDecimal("90.0000"),
                new BigDecimal("77.3214"));
        when(scoringService.score(200, 2, new BigDecimal("4.50"))).thenReturn(score);
        when(assignmentMapper.insert(any(DeliveryAssignment.class))).thenAnswer(invocation -> {
            DeliveryAssignment inserted = invocation.getArgument(0);
            inserted.setId(12L);
            return 1;
        });
        DeliveryAssignment ranked = assignment(12L, 3L, "CANDIDATE");
        ranked.setTotalScore(score.totalScore());
        when(assignmentMapper.selectList(any())).thenReturn(List.of(ranked));
        when(courierMapper.selectByIdForUpdate(3L)).thenReturn(courier());
        when(orderMapper.countActiveByCourierId(3L)).thenReturn(2L);
        when(assignmentMapper.update(isNull(), any())).thenReturn(1);

        service.startDispatch(5L);

        ArgumentCaptor<DeliveryAssignment> captor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getRouteIncrementMeters()).isEqualTo(200);
        assertThat(captor.getValue().getTotalScore()).isEqualByComparingTo("77.3214");
        verify(assignmentMapper).update(isNull(), any());
    }

    @Test
    void shouldCreateNewOrderWhenAdminRestartsReturnedSortingParcel() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(10L);
        parcel.setCurrentStatus("SORTING");
        when(parcelMapper.selectByIdForUpdate(10L)).thenReturn(parcel);
        when(orderMapper.selectLatestIdByParcelId(10L)).thenReturn(5L);
        DeliveryOrder failed = order("FAILED");
        failed.setPriority("NORMAL");
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(failed);
        when(orderMapper.insert(any(DeliveryOrder.class))).thenAnswer(invocation -> {
            DeliveryOrder inserted = invocation.getArgument(0);
            inserted.setId(6L);
            return 1;
        });
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(assignmentMapper.selectCount(any())).thenReturn(0L);
        when(assignmentMapper.selectEligibleCandidates(eq(6L), any())).thenReturn(List.of());

        service.startDispatchByParcel(7L, 10L);

        ArgumentCaptor<DeliveryOrder> orderCaptor = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getParcelId()).isEqualTo(10L);
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("ASSIGNING");
        assertThat(orderCaptor.getValue().getOrderNo()).startsWith("DO");
        verify(parcelLifecycleService).changeStatus(parcel, "WAITING_ASSIGNMENT", 7L,
                "ADMIN", "管理员开始重新配送");
    }

    @Test
    void shouldAcceptOnlyOfferedAssignmentAndPersistRouteAtomically() {
        DeliveryAssignment offered = assignment(12L, 3L, "OFFERED");
        offered.setDeliveryOrderId(5L);
        offered.setOfferExpireTime(LocalDateTime.now().plusMinutes(2));
        when(assignmentMapper.selectSnapshotById(12L)).thenReturn(offered);
        when(assignmentMapper.selectByIdForUpdate(12L)).thenReturn(offered);
        DeliveryOrder order = order("ASSIGNING");
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order);
        Courier courier = courier();
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier);
        when(orderMapper.countActiveByCourierId(3L)).thenReturn(7L);
        RoutePlan plan = new RoutePlan(25, List.of(), 0, 0, 100,
                "dijkstra-insertion-v1", LocalDateTime.now());
        when(routePlanningService.planInsertion(eq(3L), eq(25L), eq(order), any()))
                .thenReturn(plan);
        when(assignmentMapper.update(isNull(), any())).thenReturn(1);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        DeliveryRoute route = new DeliveryRoute();
        route.setId(99L);
        when(routeVersionService.saveAcceptedPlan(3L, plan)).thenReturn(route);
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(10L);
        parcel.setCurrentStatus("WAITING_ASSIGNMENT");
        when(parcelMapper.selectById(10L)).thenReturn(parcel);

        DeliveryOrder accepted = service.acceptAssignment(8L, 12L);

        assertThat(accepted.getStatus()).isEqualTo("ACCEPTED");
        assertThat(accepted.getAssignedCourierId()).isEqualTo(3L);
        verify(routeVersionService).saveAcceptedPlan(3L, plan);
        verify(parcelLifecycleService).changeStatus(eq(parcel), eq("DELIVERING"),
                eq(8L), eq("COURIER"), any());
        verify(courierMapper).update(isNull(), any());
    }

    @Test
    void shouldRequireDetailWhenRejectReasonIsOther() {
        assertThatThrownBy(() -> service.rejectAssignment(8L, 12L,
                new DispatchRejectDTO("OTHER", " ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须填写");
    }

    @Test
    void shouldRecheckEightOrderCapacityWhileHoldingCourierLock() {
        DeliveryAssignment offered = assignment(12L, 3L, "OFFERED");
        offered.setOfferExpireTime(LocalDateTime.now().plusMinutes(2));
        when(assignmentMapper.selectSnapshotById(12L)).thenReturn(offered);
        when(assignmentMapper.selectByIdForUpdate(12L)).thenReturn(offered);
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order("ASSIGNING"));
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier());
        when(orderMapper.countActiveByCourierId(3L)).thenReturn(8L);

        assertThatThrownBy(() -> service.acceptAssignment(8L, 12L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("达到上限");
        verify(routePlanningService, org.mockito.Mockito.never())
                .planInsertion(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(), any(), any());
    }

    private DeliveryOrder order(String status) {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(5L);
        order.setOrderNo("DO001");
        order.setParcelId(10L);
        order.setPickupLocationId(25L);
        order.setDestinationLocationId(26L);
        order.setStatus(status);
        order.setDispatchRound(0);
        return order;
    }

    private DeliveryAssignment assignment(long id, long courierId, String decision) {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setId(id);
        assignment.setDeliveryOrderId(5L);
        assignment.setCourierId(courierId);
        assignment.setAssignmentRound(1);
        assignment.setDecision(decision);
        return assignment;
    }

    private Courier courier() {
        Courier courier = new Courier();
        courier.setId(3L);
        courier.setUserId(8L);
        courier.setEmploymentStatus("ACTIVE");
        courier.setAvailabilityStatus("AVAILABLE");
        courier.setCurrentLocationId(25L);
        return courier;
    }
}
