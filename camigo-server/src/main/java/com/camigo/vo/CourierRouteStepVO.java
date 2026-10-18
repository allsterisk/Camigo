package com.camigo.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 同一地点连续取送任务的合并展示步骤。 */
public record CourierRouteStepVO(
        int sequenceNo,
        Long locationId,
        String locationName,
        List<Long> pickupOrderIds,
        List<String> pickupOrderNos,
        List<Long> dropoffOrderIds,
        List<String> dropoffOrderNos,
        LocalDateTime estimatedArrivalTime) {
}
