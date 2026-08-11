package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户资料修改请求。普通用户不能修改 status。
 */
@Schema(description = "用户资料修改请求；所有字段可选，但至少需要提供一个字段")
public record UserUpdateDTO(
        @Schema(description = "真实姓名", example = "张三")
        @Size(min = 1, max = 50, message = "真实姓名长度必须为1到50位")
        String realName,

        @Schema(description = "手机号", example = "13800138000")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "学号；空字符串表示清除学号", example = "20260001")
        @Size(max = 32, message = "学号不能超过32位")
        String studentNo,

        @Schema(description = "账号状态，仅管理员可修改", example = "ACTIVE")
        @Pattern(regexp = "ACTIVE|DISABLED|CANCELLED",
                message = "账号状态只能为ACTIVE、DISABLED或CANCELLED")
        String status) {
}
