package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.DeliveryOrder;
import com.camigo.vo.UserDeliveryOrderVO;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送订单数据访问。
 */
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {

    /**
     * 接单和状态流转前锁定订单，防止并发覆盖。
     */
    @Select("SELECT * FROM delivery_orders WHERE id = #{id} FOR UPDATE")
    DeliveryOrder selectByIdForUpdate(@Param("id") Long id);

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
