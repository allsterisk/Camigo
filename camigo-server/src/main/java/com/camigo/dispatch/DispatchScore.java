package com.camigo.dispatch;

import java.math.BigDecimal;

/** 三项派单得分和总分。 */
public record DispatchScore(BigDecimal distanceScore,
                            BigDecimal workloadScore,
                            BigDecimal serviceScore,
                            BigDecimal totalScore) {
}
