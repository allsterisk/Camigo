package com.camigo.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理员查看的完整派单依据与结果。 */
public record DispatchAssignmentDetailVO(
        Long assignmentId,
        Long deliveryOrderId,
        String orderNo,
        Long courierId,
        String courierNo,
        String courierName,
        Integer assignmentRound,
        String decision,
        Integer routeIncrementMeters,
        Integer activeOrderCount,
        BigDecimal distanceScore,
        BigDecimal workloadScore,
        BigDecimal serviceScore,
        BigDecimal totalScore,
        String algorithmVersion,
        String decisionReason,
        LocalDateTime offerTime,
        LocalDateTime offerExpireTime,
        LocalDateTime responseTime,
        String rejectReasonCode,
        String rejectReasonDetail) {
}
