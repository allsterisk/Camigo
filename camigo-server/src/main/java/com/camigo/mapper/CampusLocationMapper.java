package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.CampusLocation;
import com.camigo.vo.CampusLocationOptionVO;
import java.util.List;
import org.apache.ibatis.annotations.Select;

/**
 * 校园地点数据访问。
 */
public interface CampusLocationMapper extends BaseMapper<CampusLocation> {

    @Select("""
            SELECT id
            FROM campus_locations
            WHERE status = 'ACTIVE'
            ORDER BY id
            """)
    List<Long> selectActiveLocationIds();

    @Select("""
            SELECT id, location_code, location_name
            FROM campus_locations
            WHERE status = 'ACTIVE'
            ORDER BY location_name, id
            """)
    List<CampusLocationOptionVO> selectActiveOptions();

    @Select("""
            SELECT id, location_code, location_name FROM campus_locations
            WHERE status = 'ACTIVE' AND pickup_enabled = 1
            ORDER BY location_name, id
            """)
    List<CampusLocationOptionVO> selectPickupOptions();

    @Select("""
            SELECT id, location_code, location_name FROM campus_locations
            WHERE status = 'ACTIVE' AND delivery_enabled = 1 AND pickup_enabled = 0
            ORDER BY location_name, id
            """)
    List<CampusLocationOptionVO> selectDeliveryOptions();
}
