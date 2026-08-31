package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.CamigoApplication;
import com.camigo.dispatch.IntelligentDispatchService;
import com.camigo.dto.DispatchRejectDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryAssignment;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.RouteStop;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryAssignmentMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.RouteStopMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/** 阶段4真实MySQL验收；所有新增邀请、路线和状态变化在测试结束时回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class IntelligentDispatchIntegrationTest {

    @Autowired private IntelligentDispatchService dispatchService;
    @Autowired private DeliveryOrderMapper orderMapper;
    @Autowired private DeliveryAssignmentMapper assignmentMapper;
    @Autowired private CourierMapper courierMapper;
    @Autowired private ParcelMapper parcelMapper;
    @Autowired private DeliveryRouteMapper routeMapper;
    @Autowired private RouteStopMapper stopMapper;

    @Test
    void shouldScoreOfferAcceptAndPersistExactlyOneCourierRoute() {
        DeliveryOrder order = findDispatchableOrder();

        dispatchService.startDispatch(order.getId());
        assertThat(orderMapper.selectDeliveryStartAvailableParcelIds(List.of(order.getParcelId())))
                .isEmpty();

        List<DeliveryAssignment> round = assignmentMapper.selectList(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                        .eq(DeliveryAssignment::getAssignmentRound, 1)
                        .orderByDesc(DeliveryAssignment::getTotalScore));
        assertThat(round).isNotEmpty();
        assertThat(round).allSatisfy(candidate -> {
            assertThat(candidate.getRouteIncrementMeters()).isNotNegative();
            assertThat(candidate.getDistanceScore()).isBetween(
                    java.math.BigDecimal.ZERO, new java.math.BigDecimal("100.0000"));
            assertThat(candidate.getActiveOrderCount()).isLessThan(8);
            assertThat(candidate.getAlgorithmVersion()).isEqualTo("dispatch-score-v1");
        });
        assertThat(round).filteredOn(candidate -> "OFFERED".equals(candidate.getDecision()))
                .hasSize(1);
        DeliveryAssignment offered = round.stream()
                .filter(candidate -> "OFFERED".equals(candidate.getDecision()))
                .findFirst().orElseThrow();
        assertThat(offered.getOfferExpireTime()).isEqualTo(offered.getOfferTime().plusMinutes(3));

        dispatchService.startDispatch(order.getId());
        assertThat(orderMapper.selectById(order.getId()).getDispatchRound()).isEqualTo(1);
        assertThat(assignmentMapper.selectCount(new LambdaQueryWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())))
                .isEqualTo((long) round.size());

        Courier courier = courierMapper.selectById(offered.getCourierId());
        DeliveryOrder accepted = dispatchService.acceptAssignment(courier.getUserId(), offered.getId());

        assertThat(accepted.getStatus()).isEqualTo("ACCEPTED");
        assertThat(accepted.getAssignedCourierId()).isEqualTo(courier.getId());
        Long acceptedCount = assignmentMapper.selectCount(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                        .eq(DeliveryAssignment::getDecision, "ACCEPTED"));
        assertThat(acceptedCount).isEqualTo(1);
        assertThat(orderMapper.countActiveByCourierId(courier.getId())).isLessThanOrEqualTo(8);

        DeliveryRoute activeRoute = routeMapper.selectActiveByCourierId(courier.getId());
        assertThat(activeRoute).isNotNull();
        List<RouteStop> stops = stopMapper.selectList(new LambdaQueryWrapper<RouteStop>()
                .eq(RouteStop::getDeliveryRouteId, activeRoute.getId())
                .orderByAsc(RouteStop::getSequenceNo));
        assertThat(stops).anySatisfy(stop -> {
            assertThat(stop.getDeliveryOrderId()).isEqualTo(order.getId());
            assertThat(stop.getStopType()).isEqualTo("PICKUP");
        }).anySatisfy(stop -> {
            assertThat(stop.getDeliveryOrderId()).isEqualTo(order.getId());
            assertThat(stop.getStopType()).isEqualTo("DROPOFF");
        });
        assertThat(dispatchService.adminDetails(order.getId())).hasSize(round.size());
    }

    @Test
    void shouldEnforceOneAcceptedCourierPerOrderAtDatabaseLevel() {
        DeliveryOrder order = findDispatchableOrder();
        List<Courier> couriers = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getEmploymentStatus, "ACTIVE")
                .isNotNull(Courier::getCurrentLocationId)
                .orderByAsc(Courier::getId)
                .last("LIMIT 2"));
        assertThat(couriers).hasSize(2);
        DeliveryAssignment first = legacyCompatibleAccepted(order.getId(), couriers.get(0).getId(), 98);
        DeliveryAssignment second = legacyCompatibleAccepted(order.getId(), couriers.get(1).getId(), 99);

        assignmentMapper.insert(first);

        assertThatThrownBy(() -> assignmentMapper.insert(second))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(assignmentMapper.selectCount(new LambdaQueryWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getDeliveryOrderId, order.getId())
                .eq(DeliveryAssignment::getDecision, "ACCEPTED"))).isEqualTo(1);
    }

    @Test
    void shouldRejectWithReasonAndOfferNextRankedCandidate() {
        DeliveryOrder order = findDispatchableOrder();
        dispatchService.startDispatch(order.getId());
        DeliveryAssignment firstOffer = offered(order.getId());
        Courier firstCourier = courierMapper.selectById(firstOffer.getCourierId());

        dispatchService.rejectAssignment(firstCourier.getUserId(), firstOffer.getId(),
                new DispatchRejectDTO("TOO_FAR", null));

        DeliveryAssignment rejected = assignmentMapper.selectById(firstOffer.getId());
        assertThat(rejected.getDecision()).isEqualTo("REJECTED");
        assertThat(rejected.getRejectReasonCode()).isEqualTo("TOO_FAR");
        DeliveryAssignment nextOffer = offered(order.getId());
        assertThat(nextOffer.getId()).isNotEqualTo(firstOffer.getId());
        assertThat(nextOffer.getTotalScore()).isLessThanOrEqualTo(firstOffer.getTotalScore());
    }

    @Test
    void shouldExpireInvitationByDatabaseTimeAndOfferNextCandidate() {
        DeliveryOrder order = findDispatchableOrder();
        dispatchService.startDispatch(order.getId());
        DeliveryAssignment firstOffer = offered(order.getId());
        java.time.LocalDateTime expiredAt = java.time.LocalDateTime.now().minusSeconds(1);
        assignmentMapper.update(null, new LambdaUpdateWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getId, firstOffer.getId())
                .set(DeliveryAssignment::getOfferTime, expiredAt.minusMinutes(3))
                .set(DeliveryAssignment::getOfferExpireTime, expiredAt));

        dispatchService.expireOffer(firstOffer.getId());

        assertThat(assignmentMapper.selectById(firstOffer.getId()).getDecision())
                .isEqualTo("EXPIRED");
        assertThat(offered(order.getId()).getId()).isNotEqualTo(firstOffer.getId());
    }

    @Test
    void shouldPauseAfterEveryEmptyRoundUntilAdminStartsAgain() {
        DeliveryOrder order = findDispatchableOrder();
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .set(Courier::getAvailabilityStatus, "OFFLINE"));

        dispatchService.startDispatch(order.getId());
        DeliveryOrder firstPause = orderMapper.selectById(order.getId());
        assertThat(firstPause.getDispatchRound()).isEqualTo(1);
        assertThat(firstPause.getManualDispatchRequired()).isEqualTo(1);
        assertThat(firstPause.getNextDispatchTime()).isNull();
        assertThat(orderMapper.selectDeliveryStartAvailableParcelIds(List.of(order.getParcelId())))
                .containsExactly(order.getParcelId());

        dispatchService.startDispatchByParcel(order.getParcelId());
        DeliveryOrder secondPause = orderMapper.selectById(order.getId());
        assertThat(secondPause.getDispatchRound()).isEqualTo(2);
        assertThat(secondPause.getManualDispatchRequired()).isEqualTo(1);
        assertThat(secondPause.getNextDispatchTime()).isNull();
        assertThat(orderMapper.selectDeliveryStartAvailableParcelIds(List.of(order.getParcelId())))
                .containsExactly(order.getParcelId());
    }

    private DeliveryOrder findDispatchableOrder() {
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getEmploymentStatus, "ACTIVE")
                .isNotNull(Courier::getCurrentLocationId)
                .set(Courier::getAvailabilityStatus, "AVAILABLE"));
        List<DeliveryOrder> created = orderMapper.selectList(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getStatus, "CREATED")
                .isNotNull(DeliveryOrder::getPickupLocationId)
                .orderByAsc(DeliveryOrder::getId));
        DeliveryOrder order = created.stream()
                .filter(candidate -> {
                    ParcelEntity parcel = parcelMapper.selectById(candidate.getParcelId());
                    return parcel != null && "WAITING_ASSIGNMENT".equals(parcel.getCurrentStatus());
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少可用于阶段4验收的CREATED订单"));
        // 阶段10的正式联调数据为待分配单保留了候选评分快照；本测试在事务内
        // 删除该订单的快照，以构造 startDispatch 所需的全新派单边界。
        assignmentMapper.delete(new LambdaQueryWrapper<DeliveryAssignment>()
                .eq(DeliveryAssignment::getDeliveryOrderId, order.getId()));
        return order;
    }

    private DeliveryAssignment legacyCompatibleAccepted(long orderId, long courierId, int round) {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setDeliveryOrderId(orderId);
        assignment.setCourierId(courierId);
        assignment.setAssignmentRound(round);
        assignment.setAlgorithmVersion("dispatch-concurrency-test");
        assignment.setRouteIncrementMeters(0);
        assignment.setActiveOrderCount(0);
        assignment.setDistanceScore(new java.math.BigDecimal("100.0000"));
        assignment.setWorkloadScore(new java.math.BigDecimal("100.0000"));
        assignment.setServiceScore(new java.math.BigDecimal("60.0000"));
        assignment.setTotalScore(new java.math.BigDecimal("90.0000"));
        assignment.setDecision("ACCEPTED");
        return assignment;
    }

    private DeliveryAssignment offered(long orderId) {
        DeliveryAssignment assignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<DeliveryAssignment>()
                        .eq(DeliveryAssignment::getDeliveryOrderId, orderId)
                        .eq(DeliveryAssignment::getDecision, "OFFERED"));
        assertThat(assignment).isNotNull();
        return assignment;
    }
}
