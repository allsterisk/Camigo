package com.camigo.vo;

import java.math.BigDecimal;

/** 管理员配送员列表与效率汇总视图。 */
public record AdminCourierVO(
        Long id, Long userId, String courierNo, String realName, String phone,
        String employmentStatus, String availabilityStatus, Integer maxActiveOrders,
        BigDecimal ratingAvg, Long totalOrders, Long completedOrders, Long failedOrders,
        BigDecimal avgDeliverySeconds) {
}
