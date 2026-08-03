package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.DeliveryOrder;
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
}
