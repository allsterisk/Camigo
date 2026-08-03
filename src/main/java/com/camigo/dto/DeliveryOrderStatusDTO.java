package com.camigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 配送订单状态修改请求。
 */
public record DeliveryOrderStatusDTO(
        @NotNull(message = "配送订单编号不能为空") Long orderId,
        @NotBlank(message = "配送状态不能为空") String status) {
}
