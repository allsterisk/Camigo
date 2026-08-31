package com.camigo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录成功响应。
 */
@Schema(description = "登录成功响应")
public record LoginVO(
        @Schema(description = "Token 类型", example = "Bearer") String tokenType,
        @Schema(description = "JWT 访问令牌") String accessToken,
        @Schema(description = "当前用户信息") UserInfoVO user,
        @Schema(description = "配送员本次登录是否已确认位置") boolean locationConfirmed) {
}
