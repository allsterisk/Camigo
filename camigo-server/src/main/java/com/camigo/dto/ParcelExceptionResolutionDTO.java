package com.camigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ParcelExceptionResolutionDTO(
        @NotBlank(message = "请选择处理方式")
        @Pattern(regexp = "RESUME_DELIVERY|RETURN_TO_SORTING|CONFIRM_RETURNED", message = "处理方式不合法")
        String resolutionType,
        @NotBlank(message = "请填写处理说明")
        @Size(max = 500, message = "处理说明不能超过500个字符")
        String resolutionNote) {
}
