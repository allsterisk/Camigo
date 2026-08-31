package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.CamigoApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 阶段10固定联调数据验收；全部查询只读且测试事务回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_STAGE10_TESTS", matches = "true")
class Stage10TestDataIntegrityIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldKeepExactAccountAndParcelCardinality() {
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM couriers")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM parcels")).isEqualTo(40);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders")).isEqualTo(40);

        Map<String, Integer> roles = jdbcTemplate.query(
                """
                SELECT r.role_code, COUNT(*) AS role_count
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                GROUP BY r.role_code
                """,
                result -> {
                    var values = new java.util.HashMap<String, Integer>();
                    while (result.next()) values.put(result.getString(1), result.getInt(2));
                    return values;
                });
        assertThat(roles).containsEntry("ADMIN", 1)
                .containsEntry("COURIER", 3)
                .containsEntry("USER", 5);

        assertThat(count("""
                SELECT COUNT(*) FROM (
                    SELECT recipient_user_id
                    FROM parcels
                    GROUP BY recipient_user_id
                    HAVING COUNT(*) = 8
                ) student_parcels
                """)).isEqualTo(5);
    }

    @Test
    void shouldCoverEightBusinessScenariosWithoutStateContradictions() {
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'CREATED'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'ASSIGNING'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'ACCEPTED'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'IN_TRANSIT'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'COMPLETED'"))
                .isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM delivery_orders WHERE status = 'FAILED'"))
                .isEqualTo(10);

        assertThat(count("""
                SELECT COUNT(*)
                FROM parcels p
                JOIN delivery_orders o ON o.parcel_id = p.id
                WHERE NOT (
                    (o.status IN ('CREATED', 'ASSIGNING') AND p.current_status = 'WAITING_ASSIGNMENT')
                    OR (o.status IN ('ACCEPTED', 'IN_TRANSIT') AND p.current_status = 'DELIVERING')
                    OR (o.status = 'COMPLETED' AND p.current_status IN ('READY_PICKUP', 'PICKED_UP'))
                    OR (o.status = 'FAILED' AND p.current_status IN ('EXCEPTION', 'RETURNED'))
                )
                """)).isZero();

        assertThat(count("""
                SELECT COUNT(*)
                FROM parcels p
                LEFT JOIN parcel_status_logs latest
                  ON latest.id = (
                      SELECT l.id FROM parcel_status_logs l
                      WHERE l.parcel_id = p.id
                      ORDER BY l.occurred_time DESC, l.id DESC LIMIT 1
                  )
                WHERE latest.id IS NULL OR latest.to_status <> p.current_status
                """)).isZero();
    }

    @Test
    void shouldKeepDispatchRouteLockerAndExceptionInvariants() {
        assertThat(count("SELECT COUNT(*) FROM delivery_assignments")).isEqualTo(120);
        assertThat(count("""
                SELECT COUNT(*) FROM (
                    SELECT delivery_order_id
                    FROM delivery_assignments
                    GROUP BY delivery_order_id
                    HAVING COUNT(*) <> 3
                ) invalid_candidates
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM (
                    SELECT delivery_order_id
                    FROM delivery_assignments
                    WHERE decision = 'ACCEPTED'
                    GROUP BY delivery_order_id
                    HAVING COUNT(*) > 1
                ) duplicate_acceptance
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*)
                FROM delivery_assignments
                WHERE decision = 'OFFERED'
                  AND TIMESTAMPDIFF(SECOND, offer_time, offer_expire_time) <> 180
                """)).isZero();

        assertThat(count("""
                SELECT COUNT(*) FROM (
                    SELECT courier_id
                    FROM delivery_routes
                    WHERE status = 'ACTIVE'
                    GROUP BY courier_id
                    HAVING COUNT(*) <> 1
                ) invalid_active_routes
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*)
                FROM route_stops dropoff_stop
                JOIN route_stops pickup_stop
                  ON pickup_stop.delivery_route_id = dropoff_stop.delivery_route_id
                 AND pickup_stop.delivery_order_id = dropoff_stop.delivery_order_id
                 AND pickup_stop.stop_type = 'PICKUP'
                WHERE dropoff_stop.stop_type = 'DROPOFF'
                  AND dropoff_stop.sequence_no <= pickup_stop.sequence_no
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*)
                FROM route_stops current_stop
                JOIN route_stops business_stop
                  ON business_stop.delivery_route_id = current_stop.delivery_route_id
                 AND business_stop.sequence_no = current_stop.sequence_no + 1
                 AND business_stop.location_id = current_stop.location_id
                WHERE current_stop.stop_type = 'CURRENT'
                  AND business_stop.stop_type IN ('PICKUP', 'DROPOFF')
                """)).isPositive();

        assertThat(count("""
                SELECT COUNT(*) FROM storage_records
                WHERE status = 'STORED' AND released_time IS NULL AND pickup_code IS NOT NULL
                """)).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM pickup_records WHERE result = 'SUCCESS'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM parcel_exception_reports WHERE status = 'PENDING'"))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM parcel_exception_reports WHERE status = 'RETURNED'"))
                .isEqualTo(5);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
