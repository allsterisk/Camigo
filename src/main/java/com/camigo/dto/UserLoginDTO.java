package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求，账号可填写用户名、手机号或学号。
 */
@Schema(description = "用户登录请求")
public record UserLoginDTO(
        @Schema(description = "用户名、手机号或学号", example = "student_001")
        @NotBlank(message = "账号不能为空")
        @Size(max = 50, message = "账号不能超过50位")
        String account,

        @Schema(description = "登录密码", example = "Camigo@123")
        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码不能超过64位")
        String password) {
}
