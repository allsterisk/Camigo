package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.Courier;
import com.camigo.vo.AdminCourierVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 配送员数据访问。
 */
public interface CourierMapper extends BaseMapper<Courier> {

    /**
     * 接单和完成任务时锁定配送员档案，串行化容量判断与可用状态更新。
     */
    @Select("SELECT * FROM couriers WHERE user_id = #{userId} FOR UPDATE")
    Courier selectByUserIdForUpdate(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT c.id, c.user_id, c.courier_no, u.real_name, u.phone,
                   c.employment_status, c.availability_status, c.max_active_orders,
                   c.rating_avg, COALESCE(v.total_orders, 0) AS total_orders,
                   COALESCE(v.completed_orders, 0) AS completed_orders,
                   COALESCE(v.failed_orders, 0) AS failed_orders, v.avg_delivery_seconds
            FROM couriers c
            JOIN users u ON u.id = c.user_id
            LEFT JOIN v_courier_efficiency v ON v.courier_id = c.id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (c.courier_no LIKE CONCAT('%', #{keyword}, '%')
                   OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="employmentStatus != null">AND c.employment_status = #{employmentStatus}</if>
            ORDER BY c.create_time DESC, c.id DESC
            </script>
            """)
    Page<AdminCourierVO> selectAdminPage(Page<AdminCourierVO> page,
                                         @Param("keyword") String keyword,
                                         @Param("employmentStatus") String employmentStatus);
}
