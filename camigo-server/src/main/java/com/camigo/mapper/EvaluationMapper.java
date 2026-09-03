package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.Evaluation;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送评价数据访问。
 */
public interface EvaluationMapper extends BaseMapper<Evaluation> {

    @Select("""
            SELECT ROUND(AVG((delivery_score + service_score) / 2), 2)
            FROM evaluations
            WHERE courier_id = #{courierId} AND status = 'VISIBLE'
            """)
    BigDecimal selectCourierAverageRating(@Param("courierId") Long courierId);

    /**
     * 查询某条配送订单的可见评价；每个订单至多一条，未评价时返回 null。
     */
    @Select("""
            SELECT * FROM evaluations
            WHERE delivery_order_id = #{orderId} AND status = 'VISIBLE'
            LIMIT 1
            """)
    Evaluation selectVisibleByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询某个快递关联的全部配送订单评价，按评价时间倒序（最新一条在前）。
     * 一个快递可能因失败重派产生多条配送订单，因此可能对应多条评价。
     */
    @Select("""
            SELECT e.*
            FROM evaluations e
            JOIN delivery_orders d ON d.id = e.delivery_order_id
            WHERE d.parcel_id = #{parcelId} AND e.status = 'VISIBLE'
            ORDER BY e.evaluated_time DESC, e.id DESC
            """)
    List<Evaluation> selectVisibleByParcelId(@Param("parcelId") Long parcelId);
}
