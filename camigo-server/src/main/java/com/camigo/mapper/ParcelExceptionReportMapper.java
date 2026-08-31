package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.ParcelExceptionReport;
import com.camigo.vo.ParcelExceptionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ParcelExceptionReportMapper extends BaseMapper<ParcelExceptionReport> {
    @Select("SELECT * FROM parcel_exception_reports WHERE id = #{id} FOR UPDATE")
    ParcelExceptionReport selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            <script>
            SELECT r.id, r.parcel_id, p.tracking_no,
                   r.delivery_order_id, d.order_no,
                   r.courier_id, courier_user.real_name AS courier_name,
                   r.exception_type, r.description, r.requires_admin,
                   r.status, r.reported_time, r.handled_by_user_id,
                   handler.real_name AS handler_name, r.handled_time,
                   r.resolution_type, r.resolution_note
            FROM parcel_exception_reports r
            JOIN parcels p ON p.id = r.parcel_id
            JOIN delivery_orders d ON d.id = r.delivery_order_id
            JOIN couriers c ON c.id = r.courier_id
            JOIN users courier_user ON courier_user.id = c.user_id
            LEFT JOIN users handler ON handler.id = r.handled_by_user_id
            WHERE 1 = 1
            <if test="status != null">AND r.status = #{status}</if>
            <if test="keyword != null">
              AND (p.tracking_no LIKE CONCAT('%', #{keyword}, '%')
                   OR d.order_no LIKE CONCAT('%', #{keyword}, '%')
                   OR courier_user.real_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY CASE WHEN r.status = 'PENDING' THEN 0 ELSE 1 END,
                     r.reported_time DESC, r.id DESC
            </script>
            """)
    Page<ParcelExceptionVO> selectAdminPage(Page<ParcelExceptionVO> page,
                                             @Param("status") String status,
                                             @Param("keyword") String keyword);

    @Select("""
            SELECT r.id, r.parcel_id, p.tracking_no,
                   r.delivery_order_id, d.order_no,
                   r.courier_id, courier_user.real_name AS courier_name,
                   r.exception_type, r.description, r.requires_admin,
                   r.status, r.reported_time, r.handled_by_user_id,
                   handler.real_name AS handler_name, r.handled_time,
                   r.resolution_type, r.resolution_note
            FROM parcel_exception_reports r
            JOIN parcels p ON p.id = r.parcel_id
            JOIN delivery_orders d ON d.id = r.delivery_order_id
            JOIN couriers c ON c.id = r.courier_id
            JOIN users courier_user ON courier_user.id = c.user_id
            LEFT JOIN users handler ON handler.id = r.handled_by_user_id
            WHERE r.id = #{id}
            """)
    ParcelExceptionVO selectDetailById(@Param("id") Long id);
}
