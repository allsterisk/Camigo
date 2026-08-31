package com.camigo.dispatch;

import com.camigo.config.DispatchProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** 按最终执行版中的40%/35%/25%公式计算派单得分。 */
@Component
public class DispatchScoringService {

    private static final int SCORE_SCALE = 4;
    private final DispatchProperties properties;

    public DispatchScoringService(DispatchProperties properties) {
        this.properties = properties;
    }

    public DispatchScore score(int routeIncrementMeters, int activeOrderCount,
                               BigDecimal ratingAvg) {
        if (routeIncrementMeters < 0 || activeOrderCount < 0 || activeOrderCount >= 8) {
            throw new IllegalArgumentException("派单评分输入无效");
        }
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal distanceScore = hundred.multiply(
                        BigDecimal.valueOf(properties.distanceScaleMeters()))
                .divide(BigDecimal.valueOf(properties.distanceScaleMeters() + routeIncrementMeters),
                        SCORE_SCALE, RoundingMode.HALF_UP);
        BigDecimal workloadScore = hundred
                .multiply(BigDecimal.valueOf(8L - activeOrderCount))
                .divide(BigDecimal.valueOf(8), SCORE_SCALE, RoundingMode.HALF_UP);
        BigDecimal effectiveRating = ratingAvg == null ? BigDecimal.valueOf(3) : ratingAvg;
        BigDecimal serviceScore = effectiveRating.multiply(BigDecimal.valueOf(20))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = weighted(distanceScore, properties.distanceWeightPercent())
                .add(weighted(workloadScore, properties.workloadWeightPercent()))
                .add(weighted(serviceScore, properties.serviceWeightPercent()))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        return new DispatchScore(distanceScore, workloadScore, serviceScore, total);
    }

    private BigDecimal weighted(BigDecimal score, int percent) {
        return score.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), SCORE_SCALE + 2, RoundingMode.HALF_UP);
    }
}
