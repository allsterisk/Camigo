package com.camigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParcelExceptionReportDTO(
        @NotBlank(message = "请选择异常类型")
        @Pattern(regexp = "PACKAGE_DAMAGED|RECIPIENT_INFO_ERROR|LOCKER_FAILURE|USER_UNREACHABLE|OTHER",
                message = "异常类型不合法")
        String exceptionType,
        @NotBlank(message = "请填写异常说明")
        @Size(max = 500, message = "异常说明不能超过500个字符")
        String description) {
}
