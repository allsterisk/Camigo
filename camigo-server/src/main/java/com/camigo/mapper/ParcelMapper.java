package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.ParcelEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 快递数据访问。
 */
public interface ParcelMapper extends BaseMapper<ParcelEntity> {

    @Select("""
            SELECT id, tracking_no, carrier_code, recipient_user_id, sender_name,
                   sender_phone_masked, destination_location_id, target_locker_id,
                   size_class, weight_kg, current_status, arrive_time, create_time, update_time
            FROM parcels
            WHERE id = #{parcelId}
            FOR UPDATE
            """)
    ParcelEntity selectByIdForUpdate(@Param("parcelId") Long parcelId);
}
