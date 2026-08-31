package com.camigo.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 智能派单的评分、邀请和冷却参数。 */
@Validated
@ConfigurationProperties(prefix = "camigo.dispatch")
public record DispatchProperties(
        @Positive int offerSeconds,
        @Positive int rejectionWindowMinutes,
        @Positive int rejectionThreshold,
        @Positive int cooldownMinutes,
        @Min(0) @Max(100) int distanceWeightPercent,
        @Min(0) @Max(100) int workloadWeightPercent,
        @Min(0) @Max(100) int serviceWeightPercent,
        @Positive int distanceScaleMeters,
        @NotBlank String algorithmVersion) {

    public DispatchProperties {
        if (distanceWeightPercent + workloadWeightPercent + serviceWeightPercent != 100) {
            throw new IllegalArgumentException("智能派单评分权重之和必须为100");
        }
    }
}
