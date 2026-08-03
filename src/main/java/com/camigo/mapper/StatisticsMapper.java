package com.camigo.mapper;

import com.camigo.vo.CourierEfficiencyVO;
import com.camigo.vo.DailyParcelStatisticsVO;
import com.camigo.vo.PopularPickupTimeVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 运营统计只读查询。所有动态值均使用 MyBatis 参数绑定。
 */
public interface StatisticsMapper {

    @Select("""
            SELECT stat_date, carrier_code, parcel_count, picked_up_count, exception_count
            FROM v_daily_parcel_statistics
            WHERE stat_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY stat_date DESC, carrier_code
            """)
    List<DailyParcelStatisticsVO> selectDailyParcels(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT courier_id, courier_no, real_name, total_orders, completed_orders,
                   failed_orders, avg_delivery_seconds, avg_rating
            FROM v_courier_efficiency
            ORDER BY completed_orders DESC, avg_rating DESC, courier_id
            LIMIT #{limit}
            """)
    List<CourierEfficiencyVO> selectCourierEfficiency(@Param("limit") int limit);

    @Select("""
            SELECT HOUR(pickup_time) AS hour_of_day, COUNT(*) AS pickup_count
            FROM pickup_records
            WHERE result = 'SUCCESS' AND pickup_time >= #{startTime}
            GROUP BY HOUR(pickup_time)
            ORDER BY pickup_count DESC, hour_of_day
            LIMIT #{limit}
            """)
    List<PopularPickupTimeVO> selectPopularPickupTimes(
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);
}
