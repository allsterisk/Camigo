package com.camigo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 校园路线计算参数。 */
@Validated
@ConfigurationProperties(prefix = "camigo.routing")
public record RoutingProperties(
        @Positive int speedKilometersPerHour,
        @Positive int stationBaseSeconds,
        @Positive int pickupPerParcelSeconds,
        @Positive int destinationBaseSeconds,
        @Positive int storagePerParcelSeconds,
        @Positive int maxActiveOrders,
        @NotBlank String algorithmVersion) {
}
