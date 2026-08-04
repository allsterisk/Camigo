package com.camigo.vo;

import java.time.LocalDateTime;

/**
 * 入柜结果，不返回取件码摘要。
 */
public record StorageRecordVO(
        Long id,
        Long parcelId,
        Long lockerSlotId,
        String status,
        LocalDateTime storedTime,
        LocalDateTime codeExpireTime) {
}
