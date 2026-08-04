package com.camigo.vo;

import java.math.BigDecimal;

/**
 * 配送员效率排行。
 */
public record CourierEfficiencyVO(
        Long courierId,
        String courierNo,
        String realName,
        Long totalOrders,
        Long completedOrders,
        Long failedOrders,
        BigDecimal avgDeliverySeconds,
        BigDecimal avgRating) {
}
