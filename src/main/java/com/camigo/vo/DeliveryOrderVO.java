package com.camigo.vo;

import java.time.LocalDateTime;

/**
 * 配送订单响应。
 */
public record DeliveryOrderVO(
        Long id,
        String orderNo,
        Long parcelId,
        Long destinationLocationId,
        Long assignedCourierId,
        String priority,
        String status,
        Integer predictedMinutes,
        LocalDateTime acceptedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
