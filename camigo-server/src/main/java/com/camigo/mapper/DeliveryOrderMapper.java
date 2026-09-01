package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.DeliveryOrder;
import com.camigo.vo.UserDeliveryOrderVO;
import com.camigo.vo.CourierTaskVO;
import java.util.Set;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送订单数据访问。
 */
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {

    @Select("""
            <script>
            SELECT d.id, d.order_no, d.parcel_id, p.tracking_no, p.carrier_code,
                   d.pickup_location_id, pickup.location_name AS pickup_location_name,
                   d.destination_location_id, destination.location_name AS destination_location_name,
                   d.assigned_courier_id, p.size_class, p.weight_kg,
                   d.status, d.status AS execution_status,
                   stop.sequence_no AS route_sequence_no,
                   stop.estimated_arrival_time,
                   d.accepted_time, d.completed_time, d.create_time, d.update_time
            FROM delivery_orders d
            JOIN parcels p ON p.id = d.parcel_id
            JOIN campus_locations pickup ON pickup.id = d.pickup_location_id
            JOIN campus_locations destination ON destination.id = d.destination_location_id
            LEFT JOIN delivery_routes route
                   ON route.courier_id = d.assigned_courier_id AND route.status = 'ACTIVE'
            LEFT JOIN route_stops stop
                   ON stop.delivery_route_id = route.id
                  AND stop.delivery_order_id = d.id
                  AND stop.stop_status = 'PENDING'
                  AND stop.stop_type = CASE
                    WHEN d.status IN ('ACCEPTED', 'PICKING_UP') THEN 'PICKUP'
                    ELSE 'DROPOFF'
                  END
            WHERE d.assigned_courier_id = #{courierId}
              AND d.status IN ('CREATED', 'ASSIGNING', 'ASSIGNED', 'ACCEPTED',
                               'PICKING_UP', 'IN_TRANSIT', 'ARRIVED', 'COMPLETED')
            <if test="statuses != null and !statuses.isEmpty()">
              AND d.status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            </if>
            ORDER BY COALESCE(stop.sequence_no, 2147483647), d.create_time DESC, d.id DESC
            </script>
            """)
    Page<CourierTaskVO> selectCourierTasks(
            Page<CourierTaskVO> page,
            @Param("courierId") Long courierId,
            @Param("statuses") Set<String> statuses);

    /**
     * 接单和状态流转前锁定订单，防止并发覆盖。
     */
    @Select("SELECT * FROM delivery_orders WHERE id = #{id} FOR UPDATE")
    DeliveryOrder selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT id FROM delivery_orders
            WHERE parcel_id = #{parcelId}
            ORDER BY create_time DESC, id DESC
            LIMIT 1
            """)
    Long selectLatestIdByParcelId(@Param("parcelId") Long parcelId);

    @Select("""
            <script>
            SELECT latest_order.parcel_id
            FROM delivery_orders latest_order
            JOIN (
                SELECT parcel_id, MAX(id) AS latest_id
                FROM delivery_orders
                WHERE parcel_id IN
                <foreach collection="parcelIds" item="parcelId" open="(" separator="," close=")">
                  #{parcelId}
                </foreach>
                GROUP BY parcel_id
            ) latest ON latest.latest_id = latest_order.id
            JOIN parcels parcel ON parcel.id = latest_order.parcel_id
            WHERE latest_order.status = 'CREATED'
               OR (latest_order.status = 'ASSIGNING'
                   AND latest_order.manual_dispatch_required = 1)
               OR (latest_order.status = 'FAILED'
                   AND parcel.current_status = 'SORTING')
            </script>
            """)
    List<Long> selectDeliveryStartAvailableParcelIds(@Param("parcelIds") List<Long> parcelIds);

    @Select("""
            SELECT id, order_no, parcel_id, pickup_location_id, destination_location_id,
                   assigned_courier_id, priority, status, predicted_minutes, accepted_time,
                   completed_time, dispatch_round, next_dispatch_time, manual_dispatch_required,
                   create_time, update_time
            FROM delivery_orders
            WHERE assigned_courier_id = #{courierId}
              AND status IN ('ACCEPTED', 'PICKING_UP', 'IN_TRANSIT', 'ARRIVED')
            ORDER BY accepted_time, id
            """)
    List<DeliveryOrder> selectActiveByCourierId(@Param("courierId") Long courierId);

    @Select("""
            SELECT COUNT(*)
            FROM delivery_orders
            WHERE assigned_courier_id = #{courierId}
              AND status IN ('ACCEPTED', 'PICKING_UP', 'IN_TRANSIT', 'ARRIVED')
            """)
    long countActiveByCourierId(@Param("courierId") Long courierId);

    @Select("""
            <script>
            SELECT d.id AS delivery_order_id,
                   d.order_no,
                   d.parcel_id,
                   p.tracking_no,
                   p.carrier_code,
                   d.assigned_courier_id AS courier_id,
                   courier_user.real_name AS courier_name,
                   d.status,
                   d.completed_time,
                   CASE WHEN evaluation.id IS NULL THEN FALSE ELSE TRUE END AS evaluated
            FROM delivery_orders d
            JOIN parcels p ON p.id = d.parcel_id
            LEFT JOIN couriers courier ON courier.id = d.assigned_courier_id
            LEFT JOIN users courier_user ON courier_user.id = courier.user_id
            LEFT JOIN evaluations evaluation ON evaluation.delivery_order_id = d.id
            WHERE p.recipient_user_id = #{userId}
            <if test="statuses != null and !statuses.isEmpty()">
              AND d.status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            </if>
            <if test="evaluated != null and evaluated">
              AND evaluation.id IS NOT NULL
            </if>
            <if test="evaluated != null and !evaluated">
              AND evaluation.id IS NULL
            </if>
            ORDER BY COALESCE(d.completed_time, d.create_time) DESC, d.id DESC
            </script>
            """)
    Page<UserDeliveryOrderVO> selectUserOrders(
            Page<UserDeliveryOrderVO> page,
            @Param("userId") Long userId,
            @Param("statuses") Set<String> statuses,
            @Param("evaluated") Boolean evaluated);
}
