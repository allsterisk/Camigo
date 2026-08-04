package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 */
@Schema(description = "用户注册请求；公开注册仅允许 USER 角色")
public record UserRegisterDTO(
        @Schema(description = "用户名", example = "student_001")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 50, message = "用户名长度必须为4到50位")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @Schema(description = "登录密码", example = "Camigo@123")
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为8到64位")
        String password,

        @Schema(description = "手机号", example = "13800138000")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "注册角色；公开注册只能提交 USER", example = "USER")
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "USER|COURIER|ADMIN", message = "角色只能为USER、COURIER或ADMIN")
        String role,

        @Schema(description = "真实姓名", example = "张三")
        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 50, message = "真实姓名不能超过50位")
        String realName,

        @Schema(description = "学号", example = "20260001", nullable = true)
        @Size(max = 32, message = "学号不能超过32位")
        String studentNo) {
}
