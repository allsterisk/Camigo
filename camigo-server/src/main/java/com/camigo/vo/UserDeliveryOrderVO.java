package com.camigo.vo;

import java.time.LocalDateTime;

/**
 * Student-facing delivery order summary used by the evaluation workflow.
 */
public record UserDeliveryOrderVO(
        Long deliveryOrderId,
        String orderNo,
        Long parcelId,
        String trackingNo,
        String carrierCode,
        Long courierId,
        String courierName,
        String status,
        LocalDateTime completedTime,
        boolean evaluated) {
}
