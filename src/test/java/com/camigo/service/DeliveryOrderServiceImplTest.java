package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.common.PageResult;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.impl.DeliveryOrderServiceImpl;
import com.camigo.vo.DeliveryOrderVO;
import com.camigo.vo.UserDeliveryOrderVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceImplTest {

    @Mock
    private DeliveryOrderMapper orderMapper;
    @Mock
    private ParcelMapper parcelMapper;
    @Mock
    private CourierMapper courierMapper;
    @Mock
    private CampusLocationMapper locationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ParcelLifecycleService parcelLifecycleService;

    private DeliveryOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "delivery-service-test");
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
        TableInfoHelper.initTableInfo(assistant, ParcelEntity.class);
        TableInfoHelper.initTableInfo(assistant, Courier.class);
        service = new DeliveryOrderServiceImpl(orderMapper, parcelMapper, courierMapper,
                locationMapper, userMapper, parcelLifecycleService);
    }

    @Test
    void shouldCreateWaitingOrderForOwnedParcel() {
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));
        ParcelEntity parcel = parcel("REGISTERED");
        when(parcelMapper.selectOne(any())).thenReturn(parcel);
        CampusLocation location = new CampusLocation();
        location.setId(9L);
        location.setStatus("ACTIVE");
        when(locationMapper.selectById(9L)).thenReturn(location);
        when(orderMapper.insert(any(DeliveryOrder.class))).thenAnswer(invocation -> {
            DeliveryOrder order = invocation.getArgument(0);
            order.setId(5L);
            return 1;
        });

        DeliveryOrderVO result = service.create(7L,
                new DeliveryOrderCreateDTO(10L, null, "NORMAL", 30));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.status()).isEqualTo("WAITING");
        verify(parcelLifecycleService).changeStatus(eq(parcel), eq("WAITING_ASSIGNMENT"),
                eq(7L), eq("USER"), any());
    }

    @Test
    void shouldRejectCreationWithoutUserRole() {
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        assertThatThrownBy(() -> service.create(1L,
                new DeliveryOrderCreateDTO(10L, null, "NORMAL", 30)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldListAvailableAndOwnTasksWithPublicStatuses() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        when(courierMapper.selectOne(any())).thenReturn(courier());
        Page<DeliveryOrder> page = new Page<>(1, 20, 2);
        page.setRecords(List.of(order("CREATED", null), order("ACCEPTED", 3L)));
        when(orderMapper.selectPage(any(Page.class), any())).thenReturn(page);

        List<DeliveryOrderVO> result = service.listTasks(8L, 1, 20, null).records();

        assertThat(result).extracting(DeliveryOrderVO::status)
                .containsExactly("WAITING", "PROCESSING");
    }

    @Test
    void shouldListOnlyCurrentUsersUnevaluatedDeliveredOrders() {
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));
        UserDeliveryOrderVO databaseRow = new UserDeliveryOrderVO(
                5L, "DO202608030001", 10L, "SF123456", "SF", 3L,
                "配送员", "COMPLETED", LocalDateTime.of(2026, 8, 3, 12, 0), false);
        Page<UserDeliveryOrderVO> databasePage = new Page<>(1, 20, 1);
        databasePage.setRecords(List.of(databaseRow));
        when(orderMapper.selectUserOrders(any(Page.class), eq(7L),
                eq(Set.of("COMPLETED")), eq(false))).thenReturn(databasePage);

        PageResult<UserDeliveryOrderVO> result = service.listMy(
                7L, 1, 20, "DELIVERED", false);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).status()).isEqualTo("DELIVERED");
        assertThat(result.records().get(0).trackingNo()).isEqualTo("SF123456");
        verify(orderMapper).selectUserOrders(any(Page.class), eq(7L),
                eq(Set.of("COMPLETED")), eq(false));
    }

    @Test
    void shouldRejectUnsupportedStudentOrderStatus() {
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));

        assertThatThrownBy(() -> service.listMy(7L, 1, 20, "COMPLETED", false))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(orderMapper, never()).selectUserOrders(any(), any(), any(), any());
    }

    @Test
    void shouldAcceptAvailableOrderAndStartParcelDelivery() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        Courier courier = courier();
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier);
        DeliveryOrder order = order("CREATED", null);
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.update(eq(null), any())).thenReturn(1);
        ParcelEntity parcel = parcel("WAITING_ASSIGNMENT");
        when(parcelMapper.selectById(10L)).thenReturn(parcel);

        DeliveryOrderVO result = service.accept(8L, 5L);

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.assignedCourierId()).isEqualTo(3L);
        verify(parcelLifecycleService).changeStatus(parcel, "DELIVERING", 8L,
                "COURIER", "配送员接单 DO202608030001");
    }

    @Test
    void shouldRejectAcceptWhenCourierCapacityReached() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        Courier courier = courier();
        courier.setMaxActiveOrders(1);
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier);
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order("CREATED", null));
        when(orderMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.accept(8L, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上限");
        verify(orderMapper, never()).update(any(), any());
    }

    @Test
    void shouldCompleteOwnProcessingOrderAndReleaseIdleCourier() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        Courier courier = courier();
        courier.setAvailabilityStatus("BUSY");
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier);
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order("IN_TRANSIT", 3L));
        when(orderMapper.update(eq(null), any())).thenReturn(1);
        when(orderMapper.selectCount(any())).thenReturn(0L);

        DeliveryOrderVO result = service.updateStatus(
                8L, 5L, new DeliveryOrderStatusDTO("DELIVERED"));

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(result.completedTime()).isNotNull();
        verify(courierMapper).update(eq(null), any());
        verify(parcelLifecycleService, never()).changeStatus(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectUpdatingAnotherCouriersOrder() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier());
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order("ACCEPTED", 4L));

        assertThatThrownBy(() -> service.updateStatus(
                8L, 5L, new DeliveryOrderStatusDTO("DELIVERED")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldReturnBadRequestForBackwardPublicTransition() {
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        when(courierMapper.selectByUserIdForUpdate(8L)).thenReturn(courier());
        when(orderMapper.selectByIdForUpdate(5L)).thenReturn(order("ACCEPTED", 3L));

        assertThatThrownBy(() -> service.updateStatus(
                8L, 5L, new DeliveryOrderStatusDTO("WAITING")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Courier courier() {
        Courier courier = new Courier();
        courier.setId(3L);
        courier.setUserId(8L);
        courier.setEmploymentStatus("ACTIVE");
        courier.setAvailabilityStatus("AVAILABLE");
        courier.setMaxActiveOrders(10);
        return courier;
    }

    private DeliveryOrder order(String status, Long courierId) {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(5L);
        order.setOrderNo("DO202608030001");
        order.setParcelId(10L);
        order.setDestinationLocationId(9L);
        order.setAssignedCourierId(courierId);
        order.setPriority("NORMAL");
        order.setStatus(status);
        order.setPredictedMinutes(30);
        return order;
    }

    private ParcelEntity parcel(String status) {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(10L);
        parcel.setRecipientUserId(7L);
        parcel.setDestinationLocationId(9L);
        parcel.setCurrentStatus(status);
        return parcel;
    }
}
