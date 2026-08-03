package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户角色关联实体。
 */
@Getter
@Setter
@TableName("user_roles")
public class UserRole extends BaseEntity {

    private Long userId;
    private Long roleId;
    private Long grantedByUserId;
    private LocalDateTime grantedTime;
}
