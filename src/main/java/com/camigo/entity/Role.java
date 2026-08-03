package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统角色实体。
 */
@Getter
@Setter
@TableName("roles")
public class Role extends BaseEntity {

    private String roleCode;
    private String roleName;
    private String status;
    private String description;
}
