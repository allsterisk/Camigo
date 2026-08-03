package com.camigo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 配送员入柜请求。
 */
public record ParcelStorageDTO(
        @NotNull(message = "快递编号不能为空") Long parcelId,
        @NotNull(message = "格口编号不能为空") Long lockerSlotId,
        @NotBlank(message = "取件码不能为空")
        @Pattern(regexp = "^[0-9]{4,12}$", message = "取件码必须为4到12位数字")
        String pickupCode,
        @NotNull(message = "取件码过期时间不能为空")
        @Future(message = "取件码过期时间必须晚于当前时间")
        LocalDateTime codeExpireTime) {
}
