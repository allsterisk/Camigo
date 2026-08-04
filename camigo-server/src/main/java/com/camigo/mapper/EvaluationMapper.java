package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.Evaluation;
import java.math.BigDecimal;
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
}
