package com.camigo.vo;

import java.time.LocalDate;

/**
 * 每日承运商快递统计。
 */
public record DailyParcelStatisticsVO(
        LocalDate statDate,
        String carrierCode,
        Long parcelCount,
        Long pickedUpCount,
        Long exceptionCount) {
}
