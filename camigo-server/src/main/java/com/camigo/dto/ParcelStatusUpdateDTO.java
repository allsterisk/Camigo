package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 包裹状态修改请求。
 */
@Schema(description = "快递状态修改请求")
public record ParcelStatusUpdateDTO(
        @Schema(description = "公共目标状态", example = "DELIVERING")
        @NotBlank(message = "目标状态不能为空")
        @Pattern(regexp = "ARRIVED|STORED|PICKED|DELIVERING|COMPLETED",
                message = "快递状态只能为ARRIVED、STORED、PICKED、DELIVERING或COMPLETED")
        String status,

        @Schema(description = "状态变更备注", example = "配送员已取件", nullable = true)
        @Size(max = 255, message = "备注不能超过255位")
        String remark) {
}
