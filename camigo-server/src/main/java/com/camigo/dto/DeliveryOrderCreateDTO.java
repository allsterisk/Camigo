package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 创建配送任务请求。
 */
@Schema(description = "配送任务创建请求")
public record DeliveryOrderCreateDTO(
        @Schema(description = "快递编号", example = "1")
        @NotNull(message = "快递编号不能为空")
        @Positive(message = "快递编号必须为正数")
        Long parcelId,

        @Schema(description = "配送目的地编号；不传时使用快递的目标位置", example = "2", nullable = true)
        @Positive(message = "配送目的地编号必须为正数")
        Long destinationLocationId,

        @Schema(description = "优先级", example = "NORMAL", nullable = true)
        @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "优先级只能为LOW、NORMAL、HIGH或URGENT")
        String priority,

        @Schema(description = "预计配送分钟数", example = "30", nullable = true)
        @PositiveOrZero(message = "预计时长不能为负数")
        @Max(value = 1440, message = "预计时长不能超过1440分钟")
        Integer predictedMinutes) {
}
