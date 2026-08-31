package com.camigo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 配送员选择格口并完成入柜。 */
public record CourierStorageDTO(
        @NotNull(message = "格口编号不能为空")
        @Positive(message = "格口编号必须为正数")
        Long lockerSlotId) {
}
