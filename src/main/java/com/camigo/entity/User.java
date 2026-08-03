package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户账号实体，对应 users 表。
 */
@Getter
@Setter
@TableName("users")
public class User extends BaseEntity {

    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String studentNo;
    private String status;
    private LocalDateTime lastLoginTime;
}
