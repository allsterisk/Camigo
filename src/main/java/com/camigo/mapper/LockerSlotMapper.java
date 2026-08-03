package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.LockerSlot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 格口数据访问。
 */
public interface LockerSlotMapper extends BaseMapper<LockerSlot> {

    @Select("SELECT * FROM locker_slots WHERE id = #{id} FOR UPDATE")
    LockerSlot selectByIdForUpdate(@Param("id") Long id);
}
