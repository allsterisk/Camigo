package com.camigo.vo;

import java.time.LocalDateTime;

public record ParcelExceptionVO(
        Long id, Long parcelId, String trackingNo,
        Long deliveryOrderId, String orderNo,
        Long courierId, String courierName,
        String exceptionType, String description, Boolean requiresAdmin,
        String status, LocalDateTime reportedTime,
        Long handledByUserId, String handlerName, LocalDateTime handledTime,
        String resolutionType, String resolutionNote) {
}
