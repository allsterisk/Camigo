package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.StorageRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 入柜记录数据访问。
 */
public interface StorageRecordMapper extends BaseMapper<StorageRecord> {

    @Select("SELECT * FROM storage_records WHERE id = #{id} FOR UPDATE")
    StorageRecord selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT id, parcel_id, locker_slot_id, stored_by_user_id, status, stored_time,
                   released_time, pickup_code, pickup_code_hash, code_expire_time,
                   active_slot_id, active_parcel_id, create_time, update_time
            FROM storage_records
            WHERE parcel_id = #{parcelId}
              AND status = 'STORED'
              AND released_time IS NULL
            ORDER BY id DESC
            LIMIT 1
            """)
    StorageRecord selectActiveByParcelId(@Param("parcelId") Long parcelId);

    @Select("""
            SELECT id, parcel_id, locker_slot_id, stored_by_user_id, status, stored_time,
                   released_time, pickup_code, pickup_code_hash, code_expire_time,
                   active_slot_id, active_parcel_id, create_time, update_time
            FROM storage_records
            WHERE parcel_id = #{parcelId}
              AND status = 'STORED'
              AND released_time IS NULL
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
            """)
    StorageRecord selectActiveByParcelIdForUpdate(@Param("parcelId") Long parcelId);
}
