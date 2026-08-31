package com.camigo.vo;

import java.time.LocalDateTime;

/** 仅收件学生本人可见的活动取件信息。 */
public record ParcelPickupAccessVO(
        Long parcelId,
        Long storageRecordId,
        Long lockerId,
        String lockerName,
        Long lockerSlotId,
        String slotNo,
        String pickupCode,
        LocalDateTime storedTime) {
}
