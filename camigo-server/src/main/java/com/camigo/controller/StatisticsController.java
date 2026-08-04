package com.camigo.controller;

import com.camigo.common.Result;
import com.camigo.service.StatisticsService;
import com.camigo.vo.CourierEfficiencyVO;
import com.camigo.vo.DailyParcelStatisticsVO;
import com.camigo.vo.PopularPickupTimeVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营统计接口。
 */
@Validated
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/parcels/daily")
    public Result<List<DailyParcelStatisticsVO>> dailyParcels(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statisticsService.dailyParcels(startDate, endDate));
    }

    @GetMapping("/couriers/efficiency")
    public Result<List<CourierEfficiencyVO>> courierEfficiency(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return Result.success(statisticsService.courierEfficiency(limit));
    }

    @GetMapping("/pickups/popular-hours")
    public Result<List<PopularPickupTimeVO>> popularPickupTimes(
            @RequestParam(defaultValue = "30") @Min(1) @Max(366) int days,
            @RequestParam(defaultValue = "10") @Min(1) @Max(24) int limit) {
        return Result.success(statisticsService.popularPickupTimes(days, limit));
    }
}
