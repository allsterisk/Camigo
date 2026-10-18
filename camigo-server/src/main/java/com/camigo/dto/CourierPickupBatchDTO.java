package com.camigo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** 同站批量确认取件请求。 */
public record CourierPickupBatchDTO(
        @NotEmpty(message = "至少选择一个待取件订单")
        List<@NotNull @Positive(message = "配送订单编号必须为正数") Long> orderIds) {
}
