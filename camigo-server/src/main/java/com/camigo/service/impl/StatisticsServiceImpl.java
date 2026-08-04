package com.camigo.service.impl;

import com.camigo.exception.BusinessException;
import com.camigo.mapper.StatisticsMapper;
import com.camigo.service.StatisticsService;
import com.camigo.vo.CourierEfficiencyVO;
import com.camigo.vo.DailyParcelStatisticsVO;
import com.camigo.vo.PopularPickupTimeVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计服务实现，限制查询时间范围和返回数量。
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyParcelStatisticsVO> dailyParcels(LocalDate startDate, LocalDate endDate) {
        LocalDate actualEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStart = startDate == null ? actualEnd.minusDays(29) : startDate;
        if (actualStart.isAfter(actualEnd) || actualStart.isBefore(actualEnd.minusDays(365))) {
            throw new BusinessException(40000, "统计日期范围必须在366天以内", HttpStatus.BAD_REQUEST);
        }
        return statisticsMapper.selectDailyParcels(actualStart, actualEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierEfficiencyVO> courierEfficiency(int limit) {
        return statisticsMapper.selectCourierEfficiency(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularPickupTimeVO> popularPickupTimes(int days, int limit) {
        return statisticsMapper.selectPopularPickupTimes(LocalDateTime.now().minusDays(days), limit);
    }
}
