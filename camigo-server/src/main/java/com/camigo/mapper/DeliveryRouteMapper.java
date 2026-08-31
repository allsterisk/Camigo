package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.DeliveryRoute;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 综合路线版本数据访问。 */
public interface DeliveryRouteMapper extends BaseMapper<DeliveryRoute> {

    @Select("""
            SELECT id, courier_id, version_no, status, total_distance_meters,
                   estimated_duration_seconds, algorithm_version, create_time, update_time
            FROM delivery_routes
            WHERE courier_id = #{courierId} AND status = 'ACTIVE'
            FOR UPDATE
            """)
    DeliveryRoute selectActiveByCourierIdForUpdate(@Param("courierId") Long courierId);

    @Select("""
            SELECT id, courier_id, version_no, status, total_distance_meters,
                   estimated_duration_seconds, algorithm_version, create_time, update_time
            FROM delivery_routes
            WHERE courier_id = #{courierId} AND status = 'ACTIVE'
            """)
    DeliveryRoute selectActiveByCourierId(@Param("courierId") Long courierId);

    @Select("""
            SELECT id, courier_id, version_no, status, total_distance_meters,
                   estimated_duration_seconds, algorithm_version, create_time, update_time
            FROM delivery_routes
            WHERE courier_id = #{courierId}
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    DeliveryRoute selectLatestByCourierId(@Param("courierId") Long courierId);

    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM delivery_routes
            WHERE courier_id = #{courierId}
            """)
    Integer selectLatestVersion(@Param("courierId") Long courierId);

    @Update("""
            UPDATE delivery_routes
            SET status = 'SUPERSEDED', update_time = CURRENT_TIMESTAMP(3)
            WHERE courier_id = #{courierId} AND status = 'ACTIVE'
            """)
    int supersedeActive(@Param("courierId") Long courierId);
}
