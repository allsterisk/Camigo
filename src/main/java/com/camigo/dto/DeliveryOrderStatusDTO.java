package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 配送订单公共状态修改请求。
 */
@Schema(description = "配送任务状态修改请求")
public record DeliveryOrderStatusDTO(
        @Schema(description = "公共目标状态", example = "DELIVERED")
        @NotBlank(message = "配送状态不能为空")
        @Pattern(regexp = "WAITING|PROCESSING|DELIVERED",
                message = "配送状态只能为WAITING、PROCESSING或DELIVERED")
        String status) {
}
