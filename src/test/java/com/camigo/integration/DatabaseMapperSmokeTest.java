package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.camigo.CamigoApplication;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.EvaluationMapper;
import com.camigo.mapper.LockerSlotMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.StatisticsMapper;
import com.camigo.mapper.StorageRecordMapper;
import com.camigo.mapper.UserMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 只读数据库 Mapper 冒烟测试。默认跳过，设置 CAMIGO_RUN_DB_TESTS=true 时启用。
 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class DatabaseMapperSmokeTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;
    @Autowired
    private CourierMapper courierMapper;
    @Autowired
    private LockerSlotMapper lockerSlotMapper;
    @Autowired
    private ParcelMapper parcelMapper;
    @Autowired
    private StorageRecordMapper storageRecordMapper;
    @Autowired
    private EvaluationMapper evaluationMapper;
    @Autowired
    private StatisticsMapper statisticsMapper;

    @Test
    void shouldExecuteAllCustomMapperStatementsWithoutWritingData() {
        assertThatCode(() -> {
            userMapper.selectByAccount("__camigo_mapper_smoke_test__");
            userMapper.selectRoleCodes(Long.MAX_VALUE);
            parcelMapper.selectById(Long.MAX_VALUE);
            deliveryOrderMapper.selectByIdForUpdate(Long.MAX_VALUE);
            courierMapper.selectByUserIdForUpdate(Long.MAX_VALUE);
            lockerSlotMapper.selectByIdForUpdate(Long.MAX_VALUE);
            storageRecordMapper.selectByIdForUpdate(Long.MAX_VALUE);
            evaluationMapper.selectCourierAverageRating(Long.MAX_VALUE);
            statisticsMapper.selectDailyParcels(LocalDate.now().minusDays(7), LocalDate.now());
            statisticsMapper.selectCourierEfficiency(10);
            statisticsMapper.selectPopularPickupTimes(LocalDateTime.now().minusDays(30), 10);
        }).doesNotThrowAnyException();
    }
}
