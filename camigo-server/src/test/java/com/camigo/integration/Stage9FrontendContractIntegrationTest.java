package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.camigo.CamigoApplication;
import com.camigo.common.PageResult;
import com.camigo.dispatch.IntelligentDispatchService;
import com.camigo.dto.AdminParcelGenerateDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Locker;
import com.camigo.entity.User;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.AdminParcelGenerationService;
import com.camigo.service.DeliveryOrderService;
import com.camigo.vo.CourierTaskVO;
import com.camigo.vo.DispatchAssignmentDetailVO;
import com.camigo.vo.AdminGeneratedParcelVO;
import com.camigo.vo.AdminStudentOptionVO;
import com.camigo.vo.CampusLocationOptionVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 阶段9真实MySQL前端契约验收；只读查询且事务结束回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class Stage9FrontendContractIntegrationTest {
    @Autowired private DeliveryOrderService orderService;
    @Autowired private IntelligentDispatchService dispatchService;
    @Autowired private CourierMapper courierMapper;
    @Autowired private DeliveryOrderMapper orderMapper;
    @Autowired private AdminParcelGenerationService generationService;
    @Autowired private UserMapper userMapper;
    @Autowired private CampusLocationMapper locationMapper;
    @Autowired private LockerMapper lockerMapper;

    @Test
    void shouldProvideCompleteCourierTaskFactsWithoutObsoleteFields() {
        Courier courier = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                        .eq(Courier::getEmploymentStatus, "ACTIVE")
                        .orderByAsc(Courier::getId))
                .stream()
                .filter(item -> orderMapper.selectCount(new LambdaQueryWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getAssignedCourierId, item.getId())
                        .in(DeliveryOrder::getStatus, "CREATED", "ASSIGNING", "ASSIGNED",
                                "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED", "COMPLETED")) > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少配送员任务测试数据"));

        PageResult<CourierTaskVO> page = orderService.listTasks(courier.getUserId(), 1, 50, null);

        assertThat(page.records()).isNotEmpty().allSatisfy(task -> {
            assertThat(task.orderNo()).isNotBlank();
            assertThat(task.trackingNo()).isNotBlank();
            assertThat(task.carrierCode()).isNotBlank();
            assertThat(task.pickupLocationName()).isNotBlank();
            assertThat(task.destinationLocationName()).isNotBlank();
            assertThat(task.sizeClass()).isIn("S", "M", "L", "XL");
            assertThat(task.weightKg()).isNotNull();
        });
    }

    @Test
    void shouldResolveCompleteDispatchAuditFromParcel() {
        AdminStudentOptionVO student = userMapper.selectActiveStudentOptions().get(0);
        CampusLocationOptionVO pickup = locationMapper.selectPickupOptions().get(0);
        CompatibleDestination compatible = compatibleDestination();
        AdminGeneratedParcelVO generated = generationService.generate(activeAdminId(),
                new AdminParcelGenerateDTO(student.id(), pickup.id(), compatible.destination().id(),
                        compatible.sizeClass(), new BigDecimal("1.250"), null, 1)).get(0);

        List<DispatchAssignmentDetailVO> details =
                dispatchService.adminDetailsByParcel(generated.parcelId());

        assertThat(details).isNotEmpty().allSatisfy(detail -> {
            assertThat(detail.deliveryOrderId()).isEqualTo(generated.deliveryOrderId());
            assertThat(detail.courierName()).isNotBlank();
            assertThat(detail.assignmentRound()).isPositive();
            assertThat(detail.routeIncrementMeters()).isNotNegative();
            assertThat(detail.distanceScore()).isNotNull();
            assertThat(detail.workloadScore()).isNotNull();
            assertThat(detail.serviceScore()).isNotNull();
            assertThat(detail.totalScore()).isNotNull();
            assertThat(detail.algorithmVersion()).isNotBlank();
        });
    }

    private long activeAdminId() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, "ACTIVE"))
                .stream()
                .filter(user -> userMapper.selectRoleCodes(user.getId()).contains("ADMIN"))
                .mapToLong(User::getId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少有效管理员测试数据"));
    }

    private CompatibleDestination compatibleDestination() {
        for (CampusLocationOptionVO destination : locationMapper.selectDeliveryOptions()) {
            for (String size : List.of("S", "M", "L", "XL")) {
                Locker locker = lockerMapper.selectCompatibleOnlineAtLocation(destination.id(), size);
                if (locker != null) return new CompatibleDestination(destination, size);
            }
        }
        throw new AssertionError("缺少在线兼容快递柜目的地");
    }

    private record CompatibleDestination(CampusLocationOptionVO destination, String sizeClass) {}
}
