package com.camigo.vo;

/**
 * 热门取件小时统计，hourOfDay 范围为 0～23。
 */
public record PopularPickupTimeVO(Integer hourOfDay, Long pickupCount) {
}
