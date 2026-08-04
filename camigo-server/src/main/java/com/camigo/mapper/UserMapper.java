package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.entity.User;
import com.camigo.vo.AdminUserVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问。
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT * FROM users
            WHERE username = #{account}
               OR phone = #{account}
               OR student_no = #{account}
            LIMIT 1
            """)
    User selectByAccount(@Param("account") String account);

    @Select("""
            SELECT r.role_code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.role_code
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT u.id, u.username, u.real_name, u.phone, u.student_no, u.status,
                   GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_code) AS role_codes,
                   u.last_login_time, u.create_time, u.update_time
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.real_name LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR u.student_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null">AND u.status = #{status}</if>
            <if test="role != null">
              AND EXISTS (SELECT 1 FROM user_roles ur2 JOIN roles r2 ON r2.id = ur2.role_id
                          WHERE ur2.user_id = u.id AND r2.role_code = #{role})
            </if>
            GROUP BY u.id, u.username, u.real_name, u.phone, u.student_no, u.status,
                     u.last_login_time, u.create_time, u.update_time
            ORDER BY u.create_time DESC, u.id DESC
            </script>
            """)
    Page<AdminUserVO> selectAdminPage(Page<AdminUserVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("role") String role);
}
