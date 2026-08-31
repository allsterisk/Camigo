package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.dispatch.DispatchCandidate;
import com.camigo.entity.DeliveryAssignment;
import com.camigo.vo.DispatchAssignmentDetailVO;
import com.camigo.vo.DispatchOfferVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 智能派单候选、邀请和审计数据访问。 */
public interface DeliveryAssignmentMapper extends BaseMapper<DeliveryAssignment> {

    @Select("""
            SELECT c.id AS courier_id,
                   c.user_id,
                   c.current_location_id,
                   COUNT(active_order.id) AS active_order_count,
                   c.rating_avg
            FROM couriers c
            LEFT JOIN delivery_orders active_order
              ON active_order.assigned_courier_id = c.id
             AND active_order.status IN ('ACCEPTED', 'PICKING_UP', 'IN_TRANSIT', 'ARRIVED')
            WHERE c.employment_status = 'ACTIVE'
              AND c.availability_status = 'AVAILABLE'
              AND c.current_location_id IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM delivery_assignments active_offer
                  WHERE active_offer.delivery_order_id = #{orderId}
                    AND active_offer.courier_id = c.id
                    AND active_offer.decision = 'OFFERED'
                    AND active_offer.offer_expire_time > #{now}
              )
            GROUP BY c.id, c.user_id, c.current_location_id, c.rating_avg
            HAVING COUNT(active_order.id) < 8
            ORDER BY c.id
            """)
    List<DispatchCandidate> selectEligibleCandidates(@Param("orderId") Long orderId,
                                                      @Param("now") LocalDateTime now);

    @Select("""
            SELECT id, delivery_order_id, courier_id, assignment_round, algorithm_version,
                   route_increment_meters, active_order_count, distance_score, workload_score,
                   service_score, total_score, decision, decision_reason, offer_time,
                   offer_expire_time, response_time, reject_reason_code, reject_reason_detail,
                   decided_time, create_time, update_time
            FROM delivery_assignments
            WHERE id = #{assignmentId}
            FOR UPDATE
            """)
    DeliveryAssignment selectByIdForUpdate(@Param("assignmentId") Long assignmentId);

    @Select("""
            SELECT id, delivery_order_id, courier_id, assignment_round, algorithm_version,
                   route_increment_meters, active_order_count, distance_score, workload_score,
                   service_score, total_score, decision, decision_reason, offer_time,
                   offer_expire_time, response_time, reject_reason_code, reject_reason_detail,
                   decided_time, create_time, update_time
            FROM delivery_assignments
            WHERE id = #{assignmentId}
            """)
    DeliveryAssignment selectSnapshotById(@Param("assignmentId") Long assignmentId);

    @Select("""
            SELECT id, delivery_order_id, courier_id, assignment_round, algorithm_version,
                   route_increment_meters, active_order_count, distance_score, workload_score,
                   service_score, total_score, decision, decision_reason, offer_time,
                   offer_expire_time, response_time, reject_reason_code, reject_reason_detail,
                   decided_time, create_time, update_time
            FROM delivery_assignments
            WHERE delivery_order_id = #{orderId}
              AND courier_id = #{courierId}
              AND decision = 'OFFERED'
            ORDER BY assignment_round DESC, id DESC
            LIMIT 1
            FOR UPDATE
            """)
    DeliveryAssignment selectCurrentOfferForUpdate(@Param("orderId") Long orderId,
                                                    @Param("courierId") Long courierId);

    @Select("""
            SELECT id
            FROM delivery_assignments
            WHERE decision = 'OFFERED' AND offer_expire_time <= #{now}
            ORDER BY offer_expire_time, id
            LIMIT #{limit}
            """)
    List<Long> selectExpiredOfferIds(@Param("now") LocalDateTime now,
                                     @Param("limit") int limit);

    @Select("""
            SELECT assignment.id AS assignment_id,
                   assignment.delivery_order_id,
                   delivery_order.order_no,
                   assignment.courier_id,
                   courier.courier_no,
                   courier_user.real_name AS courier_name,
                   assignment.assignment_round,
                   assignment.decision,
                   assignment.route_increment_meters,
                   assignment.active_order_count,
                   assignment.distance_score,
                   assignment.workload_score,
                   assignment.service_score,
                   assignment.total_score,
                   assignment.algorithm_version,
                   assignment.decision_reason,
                   assignment.offer_time,
                   assignment.offer_expire_time,
                   assignment.response_time,
                   assignment.reject_reason_code,
                   assignment.reject_reason_detail
            FROM delivery_assignments assignment
            JOIN delivery_orders delivery_order ON delivery_order.id = assignment.delivery_order_id
            JOIN couriers courier ON courier.id = assignment.courier_id
            JOIN users courier_user ON courier_user.id = courier.user_id
            WHERE assignment.delivery_order_id = #{orderId}
            ORDER BY assignment.assignment_round,
                     assignment.total_score DESC,
                     assignment.courier_id
            """)
    List<DispatchAssignmentDetailVO> selectAdminDetails(@Param("orderId") Long orderId);

    @Select("""
            SELECT assignment.id AS assignment_id,
                   assignment.delivery_order_id,
                   delivery_order.order_no,
                   delivery_order.pickup_location_id,
                   pickup.location_name AS pickup_location_name,
                   delivery_order.destination_location_id,
                   destination.location_name AS destination_location_name,
                   assignment.route_increment_meters,
                   assignment.total_score,
                   assignment.offer_time,
                   assignment.offer_expire_time
            FROM delivery_assignments assignment
            JOIN couriers courier ON courier.id = assignment.courier_id
            JOIN delivery_orders delivery_order ON delivery_order.id = assignment.delivery_order_id
            JOIN campus_locations pickup ON pickup.id = delivery_order.pickup_location_id
            JOIN campus_locations destination ON destination.id = delivery_order.destination_location_id
            WHERE courier.user_id = #{userId}
              AND assignment.decision = 'OFFERED'
              AND assignment.offer_expire_time > #{now}
            ORDER BY assignment.offer_expire_time, assignment.id
            """)
    List<DispatchOfferVO> selectCurrentOffers(@Param("userId") Long userId,
                                              @Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(*)
            FROM delivery_assignments
            WHERE courier_id = #{courierId}
              AND decision = 'REJECTED'
              AND response_time >= #{windowStart}
            """)
    long countRecentRejections(@Param("courierId") Long courierId,
                               @Param("windowStart") LocalDateTime windowStart);
}
