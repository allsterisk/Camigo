package com.camigo.vo;

/**
 * 柜机及格口状态汇总。
 */
public record LockerStatusVO(
        Long lockerId,
        String lockerCode,
        String lockerName,
        Long locationId,
        String operationStatus,
        long totalSlots,
        long availableSlots,
        long reservedSlots,
        long occupiedSlots,
        long unavailableSlots) {
}
