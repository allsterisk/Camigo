package com.camigo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 创建配送任务请求。
 */
public record DeliveryOrderCreateDTO(
        @NotNull(message = "快递编号不能为空") Long parcelId,
        Long destinationLocationId,
        @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "优先级不正确") String priority,
        @PositiveOrZero(message = "预计时长不能为负数") Integer predictedMinutes) {
}
