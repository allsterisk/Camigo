package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.camigo.CamigoApplication;
import com.camigo.dto.AdminParcelGenerateDTO;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Locker;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.ParcelStatusLog;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.ParcelStatusLogMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.AdminParcelGenerationService;
import com.camigo.vo.AdminGeneratedParcelVO;
import com.camigo.vo.AdminStudentOptionVO;
import com.camigo.vo.CampusLocationOptionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 阶段8真实MySQL验收；事务结束后生成数据全部回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class AdminParcelGenerationIntegrationTest {
    @Autowired private AdminParcelGenerationService generationService;
    @Autowired private UserMapper userMapper;
    @Autowired private CampusLocationMapper locationMapper;
    @Autowired private LockerMapper lockerMapper;
    @Autowired private ParcelMapper parcelMapper;
    @Autowired private ParcelStatusLogMapper logMapper;
    @Autowired private DeliveryOrderMapper orderMapper;

    @Test
    void shouldGenerateCompleteBatchAndStartDispatchInOneTransaction() {
        long adminId = activeAdminId();
        AdminStudentOptionVO student = userMapper.selectActiveStudentOptions().get(0);
        CampusLocationOptionVO pickup = locationMapper.selectPickupOptions().get(0);
        CompatibleDestination compatible = compatibleDestination();
        long parcelCountBefore = parcelMapper.selectCount(null);
        long orderCountBefore = orderMapper.selectCount(null);
        long logCountBefore = logMapper.selectCount(null);

        List<AdminGeneratedParcelVO> generated = generationService.generate(adminId,
                new AdminParcelGenerateDTO(student.id(), pickup.id(), compatible.destination().id(),
                        compatible.sizeClass(), new BigDecimal("1.250"),
                        LocalDateTime.now().minusMinutes(5), 2));

        assertThat(generated).hasSize(2);
        assertThat(parcelMapper.selectCount(null)).isEqualTo(parcelCountBefore + 2);
        assertThat(orderMapper.selectCount(null)).isEqualTo(orderCountBefore + 2);
        assertThat(logMapper.selectCount(null)).isEqualTo(logCountBefore + 4);
        generated.forEach(item -> {
            ParcelEntity parcel = parcelMapper.selectById(item.parcelId());
            DeliveryOrder order = orderMapper.selectById(item.deliveryOrderId());
            List<ParcelStatusLog> logs = logMapper.selectList(
                    new LambdaQueryWrapper<ParcelStatusLog>()
                            .eq(ParcelStatusLog::getParcelId, item.parcelId())
                            .orderByAsc(ParcelStatusLog::getId));

            assertThat(parcel.getCurrentStatus()).isEqualTo("WAITING_ASSIGNMENT");
            assertThat(parcel.getRecipientUserId()).isEqualTo(student.id());
            assertThat(parcel.getDestinationLocationId()).isEqualTo(compatible.destination().id());
            assertThat(parcel.getTargetLockerId()).isEqualTo(compatible.locker().getId());
            assertThat(order.getParcelId()).isEqualTo(parcel.getId());
            assertThat(order.getPickupLocationId()).isEqualTo(pickup.id());
            assertThat(order.getDestinationLocationId()).isEqualTo(compatible.destination().id());
            assertThat(order.getStatus()).isNotEqualTo("CREATED");
            assertThat(order.getDispatchRound()).isEqualTo(1);
            assertThat(logs).extracting(ParcelStatusLog::getToStatus)
                    .containsExactly("REGISTERED", "WAITING_ASSIGNMENT");
            assertThat(logs).allSatisfy(log -> {
                assertThat(log.getOperatorUserId()).isEqualTo(adminId);
                assertThat(log.getSourceType()).isEqualTo("ADMIN");
            });
        });
    }

    @Test
    void shouldRejectServiceStationAsDestinationBeforeWriting() {
        long parcelCountBefore = parcelMapper.selectCount(null);
        AdminStudentOptionVO student = userMapper.selectActiveStudentOptions().get(0);
        CampusLocationOptionVO serviceStation = locationMapper.selectPickupOptions().get(0);

        assertThatThrownBy(() -> generationService.generate(activeAdminId(),
                new AdminParcelGenerateDTO(student.id(), serviceStation.id(), serviceStation.id(),
                        "S", BigDecimal.ONE, null, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("服务站");
        assertThat(parcelMapper.selectCount(null)).isEqualTo(parcelCountBefore);
    }

    private long activeAdminId() {
        return userMapper.selectList(null).stream()
                .filter(user -> "ACTIVE".equals(user.getStatus()))
                .filter(user -> userMapper.selectRoleCodes(user.getId()).contains("ADMIN"))
                .mapToLong(user -> user.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少有效管理员测试数据"));
    }

    private CompatibleDestination compatibleDestination() {
        for (CampusLocationOptionVO destination : locationMapper.selectDeliveryOptions()) {
            for (String size : List.of("S", "M", "L", "XL")) {
                Locker locker = lockerMapper.selectCompatibleOnlineAtLocation(destination.id(), size);
                if (locker != null) return new CompatibleDestination(destination, size, locker);
            }
        }
        throw new AssertionError("缺少带在线兼容快递柜的配送目的地");
    }

    private record CompatibleDestination(CampusLocationOptionVO destination,
                                         String sizeClass, Locker locker) {}
}
