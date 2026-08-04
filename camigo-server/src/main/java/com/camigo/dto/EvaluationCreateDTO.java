package com.camigo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户评价请求。
 */
public record EvaluationCreateDTO(
        @NotNull(message = "配送订单编号不能为空") Long deliveryOrderId,
        @NotNull(message = "配送评分不能为空")
        @Min(value = 1, message = "配送评分最低为1")
        @Max(value = 5, message = "配送评分最高为5") Integer deliveryScore,
        @NotNull(message = "服务评分不能为空")
        @Min(value = 1, message = "服务评分最低为1")
        @Max(value = 5, message = "服务评分最高为5") Integer serviceScore,
        @Size(max = 500, message = "评价内容不能超过500位") String content) {
}
