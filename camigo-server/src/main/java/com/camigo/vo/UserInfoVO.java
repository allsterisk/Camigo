package com.camigo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 对外用户信息，不包含密码摘要。
 */
@Schema(description = "用户公开资料")
public record UserInfoVO(
        @Schema(description = "用户编号") Long id,
        @Schema(description = "用户名") String username,
        @Schema(description = "真实姓名") String realName,
        @Schema(description = "手机号") String phone,
        @Schema(description = "学号", nullable = true) String studentNo,
        @Schema(description = "账号状态") String status,
        @Schema(description = "角色编码列表") List<String> roles) {

    public UserInfoVO {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
