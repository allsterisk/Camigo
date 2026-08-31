package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.RouteStop;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 综合路线停靠点数据访问。 */
public interface RouteStopMapper extends BaseMapper<RouteStop> {

    @Select("""
            SELECT id, delivery_route_id, delivery_order_id, location_id, sequence_no,
                   stop_type, stop_status, estimated_arrival_time, actual_arrival_time,
                   create_time, update_time
            FROM route_stops
            WHERE delivery_route_id = #{routeId}
              AND stop_status = 'PENDING'
              AND stop_type <> 'CURRENT'
            ORDER BY sequence_no
            """)
    List<RouteStop> selectUnfinishedByRouteId(@Param("routeId") Long routeId);

    @Select("""
            SELECT id, delivery_route_id, delivery_order_id, location_id, sequence_no,
                   stop_type, stop_status, estimated_arrival_time, actual_arrival_time,
                   create_time, update_time
            FROM route_stops
            WHERE delivery_route_id = #{routeId}
              AND stop_status = 'PENDING'
              AND stop_type <> 'CURRENT'
            ORDER BY sequence_no
            FOR UPDATE
            """)
    List<RouteStop> selectPendingByRouteIdForUpdate(@Param("routeId") Long routeId);

    @Select("""
            SELECT id, delivery_route_id, delivery_order_id, location_id, sequence_no,
                   stop_type, stop_status, estimated_arrival_time, actual_arrival_time,
                   create_time, update_time
            FROM route_stops
            WHERE delivery_route_id = #{routeId}
            ORDER BY sequence_no
            """)
    List<RouteStop> selectByRouteId(@Param("routeId") Long routeId);

    @Select("""
            SELECT rs.id, rs.delivery_route_id, rs.delivery_order_id, rs.location_id,
                   rs.sequence_no, rs.stop_type, rs.stop_status,
                   rs.estimated_arrival_time, rs.actual_arrival_time,
                   rs.create_time, rs.update_time
            FROM route_stops rs
            JOIN delivery_routes dr ON dr.id = rs.delivery_route_id
            WHERE rs.delivery_order_id = #{orderId}
              AND rs.stop_type = 'DROPOFF'
              AND rs.stop_status = 'ARRIVED'
            ORDER BY dr.version_no DESC, rs.id DESC
            LIMIT 1
            FOR UPDATE
            """)
    RouteStop selectLatestArrivedDropoffForUpdate(@Param("orderId") Long orderId);

    @Update("""
            UPDATE route_stops rs
            JOIN delivery_routes dr ON dr.id = rs.delivery_route_id
            SET rs.stop_status = 'SKIPPED', rs.update_time = CURRENT_TIMESTAMP(3)
            WHERE dr.courier_id = #{courierId}
              AND dr.status = 'ACTIVE'
              AND rs.delivery_order_id = #{orderId}
              AND rs.stop_status IN ('PENDING', 'ARRIVED')
            """)
    int skipActiveByOrderId(@Param("courierId") Long courierId, @Param("orderId") Long orderId);
}
