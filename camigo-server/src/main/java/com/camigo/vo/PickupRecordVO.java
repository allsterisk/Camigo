package com.camigo.vo;

import java.time.LocalDateTime;

/**
 * 取件成功响应。
 */
public record PickupRecordVO(
        Long id,
        Long storageRecordId,
        Long userId,
        String verificationType,
        String result,
        LocalDateTime pickupTime) {
}
