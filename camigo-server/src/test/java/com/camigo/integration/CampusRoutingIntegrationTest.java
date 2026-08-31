package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.camigo.CamigoApplication;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.DeliveryRoute;
import com.camigo.entity.RouteStop;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.routing.CampusRouteNetwork;
import com.camigo.routing.RouteInsertionPlanner;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteStopTask;
import com.camigo.routing.RouteStopType;
import com.camigo.routing.RouteVersionService;
import com.camigo.routing.ShortestPath;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 阶段3真实MySQL验收；事务结束后测试写入自动回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class CampusRoutingIntegrationTest {

    @Autowired
    private CampusRouteNetwork network;
    @Autowired
    private CampusLocationMapper locationMapper;
    @Autowired
    private CourierMapper courierMapper;
    @Autowired
    private DeliveryOrderMapper orderMapper;
    @Autowired
    private RouteStopMapper stopMapper;
    @Autowired
    private RouteInsertionPlanner planner;
    @Autowired
    private RouteVersionService versionService;

    @Test
    void shouldResolveEveryActiveLocationPairAndKnownMapPath() {
        List<Long> locationIds = locationMapper.selectActiveLocationIds();
        assertThat(locationIds).hasSize(12);
        assertThat(network.cachedPathCount()).isEqualTo(locationIds.size() * locationIds.size());

        for (Long from : locationIds) {
            for (Long to : locationIds) {
                ShortestPath forward = network.shortestPath(from, to);
                ShortestPath reverse = network.shortestPath(to, from);
                assertThat(forward.distanceMeters()).isEqualTo(reverse.distanceMeters());
                assertThat(forward.locationIds().get(0)).isEqualTo(from);
                assertThat(forward.locationIds().get(forward.locationIds().size() - 1)).isEqualTo(to);
            }
        }

        long library = locationId("CAMIGO-LIBRARY");
        long diningStation = locationId("CAMIGO-GATE-E");
        assertThat(network.shortestPath(library, diningStation))
                .extracting(ShortestPath::distanceMeters)
                .isEqualTo(490);
        assertThat(network.shortestPath(library, diningStation).locationIds())
                .containsExactly(library,
                        locationId("CAMIGO-DORM-07"), locationId("CAMIGO-DORM-06"),
                        locationId("CAMIGO-DORM-05"), locationId("CAMIGO-DORM-02"),
                        locationId("CAMIGO-DORM-01"), diningStation);
    }

    @Test
    void shouldCalculateMapEtaAndPersistAcceptedVersionWithoutLeavingTestData() {
        Courier courier = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                        .isNotNull(Courier::getCurrentLocationId)
                        .orderByAsc(Courier::getId)
                        .last("LIMIT 1"))
                .get(0);
        DeliveryOrder order = orderMapper.selectList(new LambdaQueryWrapper<DeliveryOrder>()
                        .isNotNull(DeliveryOrder::getPickupLocationId)
                        .orderByAsc(DeliveryOrder::getId)
                        .last("LIMIT 1"))
                .get(0);
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 8, 31, 9, 0);
        RoutePlan plan = planner.insert(courier.getCurrentLocationId(), List.of(),
                new RouteStopTask(order.getId(), order.getPickupLocationId(), RouteStopType.PICKUP),
                new RouteStopTask(order.getId(), order.getDestinationLocationId(), RouteStopType.DROPOFF),
                calculatedAt);

        DeliveryRoute saved = versionService.saveAcceptedPlan(courier.getId(), plan);

        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getVersionNo()).isPositive();
        assertThat(saved.getTotalDistanceMeters()).isEqualTo(plan.totalDistanceMeters());
        List<RouteStop> stops = stopMapper.selectList(new LambdaQueryWrapper<RouteStop>()
                .eq(RouteStop::getDeliveryRouteId, saved.getId())
                .orderByAsc(RouteStop::getSequenceNo));
        assertThat(stops).extracting(RouteStop::getStopType)
                .containsExactly("CURRENT", "PICKUP", "DROPOFF");
        assertThat(stops.get(1).getEstimatedArrivalTime()).isNotNull();
        assertThat(stops.get(2).getEstimatedArrivalTime()).isAfterOrEqualTo(
                stops.get(1).getEstimatedArrivalTime());
    }

    private long locationId(String code) {
        CampusLocation location = locationMapper.selectOne(new LambdaQueryWrapper<CampusLocation>()
                .eq(CampusLocation::getLocationCode, code));
        assertThat(location).as("校园地点 %s", code).isNotNull();
        return location.getId();
    }
}
