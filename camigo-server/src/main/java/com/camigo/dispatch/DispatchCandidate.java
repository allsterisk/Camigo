package com.camigo.dispatch;

import java.math.BigDecimal;

/** SQL初筛后的配送员快照。 */
public record DispatchCandidate(Long courierId, Long userId, Long currentLocationId,
                                Integer activeOrderCount, BigDecimal ratingAvg) {
}
