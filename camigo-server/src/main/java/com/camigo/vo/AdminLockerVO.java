package com.camigo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理员快递柜运行状态与格口利用率视图。 */
public record AdminLockerVO(
        Long id, String lockerCode, String lockerName, Long locationId,
        String locationName, String operationStatus, Integer totalSlots,
        Long availableSlots, Long reservedSlots, Long occupiedSlots,
        Long unavailableSlots, BigDecimal usageRate, LocalDateTime lastHeartbeatTime) {
}
