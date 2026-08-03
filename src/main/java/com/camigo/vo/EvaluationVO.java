package com.camigo.vo;

import java.time.LocalDateTime;

/**
 * 评价响应。
 */
public record EvaluationVO(
        Long id,
        Long deliveryOrderId,
        Long userId,
        Long courierId,
        Integer deliveryScore,
        Integer serviceScore,
        String content,
        String status,
        LocalDateTime evaluatedTime) {
}
