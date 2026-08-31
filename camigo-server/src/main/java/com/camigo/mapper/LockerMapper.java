package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.Locker;
import com.camigo.vo.AdminLockerVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 智能柜数据访问。
 */
public interface LockerMapper extends BaseMapper<Locker> {
    @Select("""
            <script>
            SELECT l.id, l.locker_code, l.locker_name, l.location_id,
                   cl.location_name, l.operation_status, l.total_slots,
                   COALESCE(SUM(CASE WHEN s.status = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS available_slots,
                   COALESCE(SUM(CASE WHEN s.status = 'RESERVED' THEN 1 ELSE 0 END), 0) AS reserved_slots,
                   COALESCE(SUM(CASE WHEN s.status = 'OCCUPIED' THEN 1 ELSE 0 END), 0) AS occupied_slots,
                   COALESCE(SUM(CASE WHEN s.status IN ('FAULT', 'DISABLED') THEN 1 ELSE 0 END), 0) AS unavailable_slots,
                   CASE WHEN COUNT(s.id) = 0 THEN 0 ELSE ROUND(
                     SUM(CASE WHEN s.status IN ('RESERVED', 'OCCUPIED') THEN 1 ELSE 0 END) * 100.0 / COUNT(s.id), 2)
                   END AS usage_rate,
                   l.last_heartbeat_time
            FROM lockers l
            LEFT JOIN campus_locations cl ON cl.id = l.location_id
            LEFT JOIN locker_slots s ON s.locker_id = l.id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (l.locker_code LIKE CONCAT('%', #{keyword}, '%')
                   OR l.locker_name LIKE CONCAT('%', #{keyword}, '%')
                   OR cl.location_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="operationStatus != null">AND l.operation_status = #{operationStatus}</if>
            GROUP BY l.id, l.locker_code, l.locker_name, l.location_id, cl.location_name,
                     l.operation_status, l.total_slots, l.last_heartbeat_time, l.create_time
            ORDER BY l.create_time DESC, l.id DESC
            </script>
            """)
    Page<AdminLockerVO> selectAdminPage(Page<AdminLockerVO> page,
                                        @Param("keyword") String keyword,
                                        @Param("operationStatus") String operationStatus);

    @Select("""
            SELECT DISTINCT l.id, l.locker_code, l.locker_name, l.location_id,
                   l.total_slots, l.operation_status, l.last_heartbeat_time,
                   l.create_time, l.update_time
            FROM lockers l
            JOIN locker_slots s ON s.locker_id = l.id
            WHERE l.location_id = #{locationId}
              AND l.operation_status = 'ONLINE'
              AND FIELD(s.size_class, 'S', 'M', 'L', 'XL')
                  >= FIELD(#{parcelSize}, 'S', 'M', 'L', 'XL')
            ORDER BY l.id
            LIMIT 1
            """)
    Locker selectCompatibleOnlineAtLocation(@Param("locationId") Long locationId,
                                             @Param("parcelSize") String parcelSize);
}
