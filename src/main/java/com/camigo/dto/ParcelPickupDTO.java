package com.camigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 用户取件请求。
 */
public record ParcelPickupDTO(
        @NotNull(message = "入柜记录编号不能为空") Long storageRecordId,
        @NotBlank(message = "取件码不能为空")
        @Pattern(regexp = "^[0-9]{4,12}$", message = "取件码格式不正确")
        String pickupCode) {
}
