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
}
