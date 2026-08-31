package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 配送员登录后确认当前校园位置。 */
@Schema(description = "配送员当前位置确认请求")
public record CourierLocationUpdateDTO(
        @Schema(description = "校园地点编号")
        @NotNull(message = "当前位置不能为空")
        @Positive(message = "当前位置编号必须为正数")
        Long locationId) {
}
