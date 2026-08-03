package com.camigo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.camigo.entity.User;
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
}
