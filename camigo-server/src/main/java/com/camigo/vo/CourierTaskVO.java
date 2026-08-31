package com.camigo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 配送员任务中心所需的完整业务信息，不暴露已废弃的优先级和订单预计分钟数。 */
public record CourierTaskVO(
        Long id,
        String orderNo,
        Long parcelId,
        String trackingNo,
        String carrierCode,
        Long pickupLocationId,
        String pickupLocationName,
        Long destinationLocationId,
        String destinationLocationName,
        Long assignedCourierId,
        String sizeClass,
        BigDecimal weightKg,
        String status,
        String executionStatus,
        Integer routeSequenceNo,
        LocalDateTime estimatedArrivalTime,
        LocalDateTime acceptedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
