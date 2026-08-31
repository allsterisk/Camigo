package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.CampusRouteEdge;
import java.util.List;
import org.apache.ibatis.annotations.Select;

/** 校园道路图数据访问。 */
public interface CampusRouteEdgeMapper extends BaseMapper<CampusRouteEdge> {

    @Select("""
            SELECT id, from_location_id, to_location_id, distance_meters,
                   bidirectional, status, create_time, update_time
            FROM campus_route_edges
            WHERE status = 'ACTIVE'
            ORDER BY from_location_id, to_location_id, id
            """)
    List<CampusRouteEdge> selectActiveEdges();
}
