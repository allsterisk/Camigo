package com.camigo.service;

import com.camigo.vo.CourierEfficiencyVO;
import com.camigo.vo.DailyParcelStatisticsVO;
import com.camigo.vo.PopularPickupTimeVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 运营统计服务。
 */
public interface StatisticsService {

    List<DailyParcelStatisticsVO> dailyParcels(LocalDate startDate, LocalDate endDate);

    List<CourierEfficiencyVO> courierEfficiency(int limit);

    List<PopularPickupTimeVO> popularPickupTimes(int days, int limit);
}
