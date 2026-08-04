package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.camigo.dto.ParcelCreateDTO;
import com.camigo.dto.ParcelStatusUpdateDTO;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Locker;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.User;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.impl.ParcelServiceImpl;
import com.camigo.vo.ParcelVO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ParcelServiceImplTest {

    @Mock
    private ParcelMapper parcelMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LockerMapper lockerMapper;
    @Mock
    private CampusLocationMapper locationMapper;
    @Mock
    private CourierMapper courierMapper;
    @Mock
    private DeliveryOrderMapper deliveryOrderMapper;
    @Mock
    private ParcelLifecycleService lifecycleService;

    private ParcelServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "parcel-service-test");
        TableInfoHelper.initTableInfo(assistant, ParcelEntity.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Courier.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
        service = new ParcelServiceImpl(parcelMapper, userMapper, lockerMapper, locationMapper,
                courierMapper, deliveryOrderMapper, lifecycleService);
    }

    @Test
    void shouldCreateParcelForResolvedRecipientAndLocker() {
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        User recipient = activeRecipient();
        when(userMapper.selectOne(any())).thenReturn(recipient);
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));
        when(lockerMapper.selectById(3L)).thenReturn(onlineLocker());
        CampusLocation location = new CampusLocation();
        location.setId(9L);
        location.setStatus("ACTIVE");
        when(locationMapper.selectById(9L)).thenReturn(location);
        when(parcelMapper.insert(any(ParcelEntity.class))).thenAnswer(invocation -> {
            ParcelEntity parcel = invocation.getArgument(0);
            parcel.setId(10L);
            return 1;
        });

        ParcelVO result = service.create(1L, new ParcelCreateDTO(
                "SF202608030001", "SF", "李四", "张三", "13800138000",
                3L, "M", new BigDecimal("1.25")));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.recipientUserId()).isEqualTo(7L);
        assertThat(result.targetLockerId()).isEqualTo(3L);
        assertThat(result.currentStatus()).isEqualTo("ARRIVED");
        verify(lifecycleService).recordInitialStatus(any(ParcelEntity.class), eq(1L), eq("ADMIN"));
    }

    @Test
    void shouldRejectParcelCreationByNonAdmin() {
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));

        assertThatThrownBy(() -> service.create(7L, new ParcelCreateDTO(
                "SF202608030001", "SF", "李四", "张三", "13800138000",
                3L, "M", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldAllowRecipientToConfirmPickedParcelCompleted() {
        ParcelEntity parcel = parcel(20L, 7L, "PICKED_UP");
        when(parcelMapper.selectById(20L)).thenReturn(parcel);
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));
        doAnswer(invocation -> {
            parcel.setCurrentStatus(invocation.getArgument(1));
            return null;
        }).when(lifecycleService).changeStatus(eq(parcel), eq("COMPLETED"), eq(7L),
                eq("USER"), any());

        ParcelVO result = service.updateStatus(7L, 20L,
                new ParcelStatusUpdateDTO("COMPLETED", "确认收货"));

        assertThat(result.currentStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldRejectDirectStoredTransition() {
        ParcelEntity parcel = parcel(20L, 7L, "DELIVERING");
        when(parcelMapper.selectById(20L)).thenReturn(parcel);
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        assertThatThrownBy(() -> service.updateStatus(1L, 20L,
                new ParcelStatusUpdateDTO("STORED", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("入柜");
    }

    @Test
    void shouldRejectUnassignedCourierParcelAccess() {
        ParcelEntity parcel = parcel(20L, 7L, "DELIVERING");
        when(parcelMapper.selectById(20L)).thenReturn(parcel);
        when(userMapper.selectRoleCodes(8L)).thenReturn(List.of("COURIER"));
        when(courierMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getById(8L, 20L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("查看");
    }

    private User activeRecipient() {
        User user = new User();
        user.setId(7L);
        user.setRealName("张三");
        user.setPhone("13800138000");
        user.setStatus("ACTIVE");
        return user;
    }

    private Locker onlineLocker() {
        Locker locker = new Locker();
        locker.setId(3L);
        locker.setLocationId(9L);
        locker.setOperationStatus("ONLINE");
        return locker;
    }

    private ParcelEntity parcel(Long id, Long recipientUserId, String status) {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(id);
        parcel.setRecipientUserId(recipientUserId);
        parcel.setTrackingNo("SF202608030001");
        parcel.setCarrierCode("SF");
        parcel.setSenderName("李四");
        parcel.setDestinationLocationId(9L);
        parcel.setTargetLockerId(3L);
        parcel.setSizeClass("M");
        parcel.setCurrentStatus(status);
        return parcel;
    }
}
