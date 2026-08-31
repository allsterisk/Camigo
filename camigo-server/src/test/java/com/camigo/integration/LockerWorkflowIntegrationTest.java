package com.camigo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.CamigoApplication;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.dto.ParcelPickupDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.LockerSlot;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.StorageRecord;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.DeliveryRouteMapper;
import com.camigo.mapper.LockerSlotMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.PickupRecordMapper;
import com.camigo.mapper.RouteStopMapper;
import com.camigo.mapper.StorageRecordMapper;
import com.camigo.routing.RoutePlan;
import com.camigo.routing.RouteVersionService;
import com.camigo.service.CourierExecutionService;
import com.camigo.service.LockerService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.vo.CourierStorageVO;
import com.camigo.vo.LockerSlotOptionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/** 阶段6真实MySQL验收；全部写入在测试结束后回滚。 */
@SpringBootTest(classes = CamigoApplication.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "CAMIGO_RUN_DB_TESTS", matches = "true")
class LockerWorkflowIntegrationTest {

    @Autowired private LockerService lockerService;
    @Autowired private CourierExecutionService executionService;
    @Autowired private CourierRoutePlanningService planningService;
    @Autowired private RouteVersionService routeVersionService;
    @Autowired private ParcelLifecycleService lifecycleService;
    @Autowired private CourierMapper courierMapper;
    @Autowired private DeliveryOrderMapper orderMapper;
    @Autowired private DeliveryRouteMapper routeMapper;
    @Autowired private ParcelMapper parcelMapper;
    @Autowired private LockerSlotMapper slotMapper;
    @Autowired private StorageRecordMapper storageMapper;
    @Autowired private PickupRecordMapper pickupMapper;
    @Autowired private RouteStopMapper stopMapper;

    @Test
    void shouldStoreWithCompatibleSlotAndInvalidateCodeAfterPickup() {
        Fixture fixture = prepareArrivedOrder();
        List<LockerSlotOptionVO> options = lockerService.listCompatibleSlots(
                fixture.courier().getUserId(), fixture.order().getId());
        assertThat(options).isNotEmpty();

        CourierStorageVO stored = lockerService.storeParcel(fixture.courier().getUserId(),
                fixture.order().getId(), options.get(0).id());

        StorageRecord storage = storageMapper.selectActiveByParcelId(fixture.parcel().getId());
        assertThat(stored.orderStatus()).isEqualTo("COMPLETED");
        assertThat(stored.parcelStatus()).isEqualTo("READY_PICKUP");
        assertThat(storage.getPickupCode()).matches("^[0-9]{4}$");
        assertThat(storage.getPickupCodeHash()).isNull();
        assertThat(storage.getCodeExpireTime()).isNull();
        assertThat(orderMapper.selectById(fixture.order().getId()).getStatus())
                .isEqualTo("COMPLETED");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus())
                .isEqualTo("READY_PICKUP");
        assertThat(slotMapper.selectById(options.get(0).id()).getStatus()).isEqualTo("OCCUPIED");
        assertThat(stopMapper.selectLatestArrivedDropoffForUpdate(fixture.order().getId())).isNull();
        assertThat(lockerService.getPickupAccess(fixture.parcel().getRecipientUserId(),
                fixture.parcel().getId()).pickupCode()).isEqualTo(storage.getPickupCode());

        var pickup = lockerService.pickupParcel(fixture.parcel().getRecipientUserId(),
                new ParcelPickupDTO(fixture.parcel().getId(), storage.getPickupCode()));

        StorageRecord released = storageMapper.selectById(storage.getId());
        assertThat(pickup.verificationType()).isEqualTo("PICKUP_CODE");
        assertThat(pickup.result()).isEqualTo("SUCCESS");
        assertThat(released.getStatus()).isEqualTo("PICKED_UP");
        assertThat(released.getPickupCode()).isNull();
        assertThat(released.getPickupCodeHash()).isNull();
        assertThat(released.getReleasedTime()).isNotNull();
        assertThat(slotMapper.selectById(options.get(0).id()).getStatus()).isEqualTo("AVAILABLE");
        assertThat(parcelMapper.selectById(fixture.parcel().getId()).getCurrentStatus())
                .isEqualTo("PICKED_UP");
        assertThat(pickupMapper.selectById(pickup.id()).getAppointmentId()).isNull();
    }

    @Test
    void shouldRejectLargeParcelInSmallerSlot() {
        Fixture fixture = prepareArrivedOrder();
        parcelMapper.update(null, new LambdaUpdateWrapper<ParcelEntity>()
                .eq(ParcelEntity::getId, fixture.parcel().getId())
                .set(ParcelEntity::getSizeClass, "L"));
        LockerSlot smallSlot = slotMapper.selectList(new LambdaQueryWrapper<LockerSlot>()
                        .eq(LockerSlot::getLockerId, fixture.parcel().getTargetLockerId())
                        .eq(LockerSlot::getSizeClass, "S")
                        .eq(LockerSlot::getStatus, "AVAILABLE")
                        .orderByAsc(LockerSlot::getId))
                .stream().findFirst().orElseThrow();

        assertThatThrownBy(() -> lockerService.storeParcel(fixture.courier().getUserId(),
                fixture.order().getId(), smallSlot.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格口尺寸小于包裹尺寸");
    }

    @Test
    void shouldEnforceUniqueNonNullPickupCodeInDatabase() {
        List<ParcelEntity> parcels = parcelsWithoutActiveStorage(2);
        List<LockerSlot> slots = availableSlots(2);
        storageMapper.insert(storage(parcels.get(0).getId(), slots.get(0).getId(), "0042"));

        assertThatThrownBy(() -> storageMapper.insert(
                storage(parcels.get(1).getId(), slots.get(1).getId(), "0042")))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_storage_active_pickup_code");
    }

    @Test
    void shouldEnforceOneActiveStoragePerSlotInDatabase() {
        List<ParcelEntity> parcels = parcelsWithoutActiveStorage(2);
        LockerSlot slot = availableSlots(1).get(0);
        storageMapper.insert(storage(parcels.get(0).getId(), slot.getId(), "1042"));

        assertThatThrownBy(() -> storageMapper.insert(
                storage(parcels.get(1).getId(), slot.getId(), "2042")))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_storage_active_slot");
    }

    private Fixture prepareArrivedOrder() {
        Courier courier = idleCourier();
        DeliveryOrder order = orderMapper.selectList(new LambdaQueryWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getStatus, "CREATED")
                        .isNotNull(DeliveryOrder::getPickupLocationId)
                        .orderByAsc(DeliveryOrder::getId))
                .stream()
                .filter(candidate -> storageMapper.selectActiveByParcelId(candidate.getParcelId()) == null)
                .filter(candidate -> {
                    ParcelEntity candidateParcel = parcelMapper.selectById(candidate.getParcelId());
                    return candidateParcel != null
                            && !slotMapper.selectCompatibleAvailable(
                                    candidateParcel.getTargetLockerId(),
                                    candidateParcel.getSizeClass()).isEmpty();
                })
                .findFirst().orElseThrow();
        ParcelEntity parcel = parcelMapper.selectById(order.getParcelId());
        LocalDateTime now = LocalDateTime.now();
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, "AVAILABLE")
                .set(Courier::getCurrentLocationId, order.getPickupLocationId())
                .set(Courier::getLocationUpdateTime, now));
        courier.setAvailabilityStatus("AVAILABLE");
        courier.setCurrentLocationId(order.getPickupLocationId());
        courier.setLocationUpdateTime(now);
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .set(DeliveryOrder::getAssignedCourierId, courier.getId())
                .set(DeliveryOrder::getStatus, "ACCEPTED")
                .set(DeliveryOrder::getAcceptedTime, now));
        order.setAssignedCourierId(courier.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(now);
        lifecycleService.changeStatus(parcel, "DELIVERING", courier.getUserId(),
                "COURIER", "阶段6集成验收准备");
        RoutePlan plan = planningService.planRemaining(courier.getId(),
                courier.getCurrentLocationId(), now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);
        executionService.confirmPickup(courier.getUserId(), order.getId());
        executionService.confirmArrival(courier.getUserId(), order.getId());
        order.setStatus("ARRIVED");
        return new Fixture(courier, order, parcel);
    }

    private Courier idleCourier() {
        Courier courier = courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                        .eq(Courier::getEmploymentStatus, "ACTIVE")
                        .orderByAsc(Courier::getId)).stream()
                .findFirst().orElseThrow();
        orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId())
                .in(DeliveryOrder::getStatus, "ACCEPTED", "PICKING_UP", "IN_TRANSIT", "ARRIVED")
                .set(DeliveryOrder::getStatus, "CANCELLED"));
        routeMapper.supersedeActive(courier.getId());
        return courier;
    }

    private List<ParcelEntity> parcelsWithoutActiveStorage(int count) {
        List<ParcelEntity> parcels = parcelMapper.selectList(new LambdaQueryWrapper<ParcelEntity>()
                .orderByAsc(ParcelEntity::getId)).stream()
                .filter(parcel -> storageMapper.selectActiveByParcelId(parcel.getId()) == null)
                .limit(count).toList();
        assertThat(parcels).hasSize(count);
        return parcels;
    }

    private List<LockerSlot> availableSlots(int count) {
        List<LockerSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<LockerSlot>()
                .eq(LockerSlot::getStatus, "AVAILABLE")
                .orderByAsc(LockerSlot::getId)).stream().limit(count).toList();
        assertThat(slots).hasSize(count);
        return slots;
    }

    private StorageRecord storage(Long parcelId, Long slotId, String code) {
        StorageRecord storage = new StorageRecord();
        storage.setParcelId(parcelId);
        storage.setLockerSlotId(slotId);
        storage.setStoredByUserId(courierMapper.selectList(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getEmploymentStatus, "ACTIVE")
                .orderByAsc(Courier::getId))
                .stream().findFirst().orElseThrow().getUserId());
        storage.setStatus("STORED");
        storage.setStoredTime(LocalDateTime.now());
        storage.setPickupCode(code);
        return storage;
    }

    private record Fixture(Courier courier, DeliveryOrder order, ParcelEntity parcel) {
    }
}
