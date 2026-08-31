package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.LockerSlot;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 格口数据访问。
 */
public interface LockerSlotMapper extends BaseMapper<LockerSlot> {

    @Select("SELECT * FROM locker_slots WHERE id = #{id} FOR UPDATE")
    LockerSlot selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT id, locker_id, slot_no, size_class, status, create_time, update_time
            FROM locker_slots FORCE INDEX (idx_locker_slots_compatible)
            WHERE locker_id = #{lockerId}
              AND status = 'AVAILABLE'
              AND FIELD(size_class, 'S', 'M', 'L', 'XL')
                  >= FIELD(#{parcelSize}, 'S', 'M', 'L', 'XL')
            ORDER BY FIELD(size_class, 'S', 'M', 'L', 'XL'), slot_no, id
            """)
    List<LockerSlot> selectCompatibleAvailable(@Param("lockerId") Long lockerId,
                                                @Param("parcelSize") String parcelSize);
}
