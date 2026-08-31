package com.camigo.vo;

import java.time.LocalDateTime;

/** 配送员当前位置与可接单状态。 */
public record CourierLocationStateVO(
        Long courierId,
        Long currentLocationId,
        String currentLocationName,
        LocalDateTime locationUpdateTime,
        String availabilityStatus,
        long activeOrderCount,
        boolean locationConfirmed) {
}
