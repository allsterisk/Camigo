package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.Courier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送员数据访问。
 */
public interface CourierMapper extends BaseMapper<Courier> {

    /**
     * 接单和完成任务时锁定配送员档案，串行化容量判断与可用状态更新。
     */
    @Select("SELECT * FROM couriers WHERE user_id = #{userId} FOR UPDATE")
    Courier selectByUserIdForUpdate(@Param("userId") Long userId);
}
