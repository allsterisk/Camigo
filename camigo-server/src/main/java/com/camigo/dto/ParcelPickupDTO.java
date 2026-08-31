package com.camigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 用户取件请求。
 */
public record ParcelPickupDTO(
        @NotNull(message = "快递编号不能为空")
        @Positive(message = "快递编号必须为正数") Long parcelId,
        @NotBlank(message = "取件码不能为空")
        @Pattern(regexp = "^[0-9]{4}$", message = "取件码必须为四位数字")
        String pickupCode) {
}
