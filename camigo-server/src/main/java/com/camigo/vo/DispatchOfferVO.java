package com.camigo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 配送员当前待确认的派单邀请。 */
public record DispatchOfferVO(
        Long assignmentId,
        Long deliveryOrderId,
        String orderNo,
        Long pickupLocationId,
        String pickupLocationName,
        Long destinationLocationId,
        String destinationLocationName,
        Integer routeIncrementMeters,
        BigDecimal totalScore,
        LocalDateTime offerTime,
        LocalDateTime offerExpireTime) {
}
