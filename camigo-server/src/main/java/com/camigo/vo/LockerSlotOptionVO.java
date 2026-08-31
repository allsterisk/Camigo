package com.camigo.vo;

/** 配送员可选的兼容空闲格口。 */
public record LockerSlotOptionVO(
        Long id,
        Long lockerId,
        String lockerName,
        String slotNo,
        String sizeClass) {
}
