package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dispatch.CourierRoutePlanningService;
import com.camigo.dto.ParcelPickupDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Locker;
import com.camigo.entity.LockerSlot;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.PickupRecord;
import com.camigo.entity.RouteStop;
import com.camigo.entity.StorageRecord;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
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
import com.camigo.service.PickupCodeGenerator;
import com.camigo.utils.PasswordUtil;
import com.camigo.vo.CourierStorageVO;
import com.camigo.vo.LockerSlotOptionVO;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.ParcelPickupAccessVO;
import com.camigo.vo.PickupRecordVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 格口选择、入柜完成和学生取件的一致性事务。 */
@Service
public class LockerServiceImpl implements LockerService {

    private static final int PICKUP_CODE_ATTEMPTS = 32;
    private static final Map<String, Integer> SIZE_RANK = Map.of(
            "S", 1, "M", 2, "L", 3, "XL", 4);

    private final LockerMapper lockerMapper;
    private final LockerSlotMapper slotMapper;
    private final StorageRecordMapper storageMapper;
    private final PickupRecordMapper pickupMapper;
    private final ParcelMapper parcelMapper;
    private final CourierMapper courierMapper;
    private final DeliveryOrderMapper orderMapper;
    private final RouteStopMapper stopMapper;
    private final ParcelLifecycleService lifecycleService;
    private final CourierRoutePlanningService planningService;
    private final RouteVersionService routeVersionService;
    private final CourierExecutionService executionService;
    private final PickupCodeGenerator pickupCodeGenerator;
    private final PasswordUtil passwordUtil;

    public LockerServiceImpl(LockerMapper lockerMapper,
                             LockerSlotMapper slotMapper,
                             StorageRecordMapper storageMapper,
                             PickupRecordMapper pickupMapper,
                             ParcelMapper parcelMapper,
                             CourierMapper courierMapper,
                             DeliveryOrderMapper orderMapper,
                             RouteStopMapper stopMapper,
                             ParcelLifecycleService lifecycleService,
                             CourierRoutePlanningService planningService,
                             RouteVersionService routeVersionService,
                             CourierExecutionService executionService,
                             PickupCodeGenerator pickupCodeGenerator,
                             PasswordUtil passwordUtil) {
        this.lockerMapper = lockerMapper;
        this.slotMapper = slotMapper;
        this.storageMapper = storageMapper;
        this.pickupMapper = pickupMapper;
        this.parcelMapper = parcelMapper;
        this.courierMapper = courierMapper;
        this.orderMapper = orderMapper;
        this.stopMapper = stopMapper;
        this.lifecycleService = lifecycleService;
        this.planningService = planningService;
        this.routeVersionService = routeVersionService;
        this.executionService = executionService;
        this.pickupCodeGenerator = pickupCodeGenerator;
        this.passwordUtil = passwordUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LockerSlotOptionVO> listCompatibleSlots(Long userId, Long orderId) {
        Courier courier = requireCourier(userId);
        DeliveryOrder order = requireOrder(orderId);
        ensureAssignedArrivedOrder(courier, order);
        ParcelEntity parcel = requireParcel(order.getParcelId());
        Locker locker = requireDestinationLocker(order, parcel);
        return slotMapper.selectCompatibleAvailable(locker.getId(), parcel.getSizeClass()).stream()
                .map(slot -> new LockerSlotOptionVO(slot.getId(), locker.getId(),
                        locker.getLockerName(), slot.getSlotNo(), slot.getSizeClass()))
                .toList();
    }

    @Override
    @Transactional
    public CourierStorageVO storeParcel(Long userId, Long orderId, Long lockerSlotId) {
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        Courier courier = courierMapper.selectByUserIdForUpdate(userId);
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND, "配送员档案不存在",
                    HttpStatus.NOT_FOUND);
        }
        ensureAssignedArrivedOrder(courier, order);

        ParcelEntity parcel = parcelMapper.selectByIdForUpdate(order.getParcelId());
        if (parcel == null) {
            throw new ResourceNotFoundException("关联快递不存在");
        }
        if (!"DELIVERING".equals(parcel.getCurrentStatus())) {
            throw conflict("快递状态与待入柜配送任务不一致");
        }
        Locker locker = requireDestinationLocker(order, parcel);
        if (!Objects.equals(courier.getCurrentLocationId(), order.getDestinationLocationId())) {
            throw conflict("配送员当前位置不在订单目的地");
        }

        LockerSlot slot = slotMapper.selectByIdForUpdate(lockerSlotId);
        if (slot == null || !Objects.equals(slot.getLockerId(), locker.getId())) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "所选格口不属于目标快递柜");
        }
        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口当前不可用");
        }
        ensureSizeCompatible(parcel.getSizeClass(), slot.getSizeClass());

        LocalDateTime now = LocalDateTime.now();
        StorageRecord storage = insertStorageWithUniqueCode(parcel.getId(), slot.getId(),
                userId, now);
        if (slotMapper.update(null, new LambdaUpdateWrapper<LockerSlot>()
                .eq(LockerSlot::getId, slot.getId())
                .eq(LockerSlot::getStatus, "AVAILABLE")
                .set(LockerSlot::getStatus, "OCCUPIED")) != 1) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口已被其他请求占用");
        }

        lifecycleService.changeStatus(parcel, "READY_PICKUP", userId, "LOCKER",
                "包裹已存入 " + locker.getLockerName() + " 格口 " + slot.getSlotNo());
        completeOrder(order, now);
        completeArrivedDropoff(order.getId(), now);
        updateCourierAfterStorage(courier, order.getDestinationLocationId(), now);
        RoutePlan plan = planningService.planRemaining(courier.getId(),
                order.getDestinationLocationId(), now);
        routeVersionService.saveAcceptedPlan(courier.getId(), plan);

        return new CourierStorageVO(storage.getId(), order.getId(), "COMPLETED",
                parcel.getCurrentStatus(), locker.getId(), slot.getId(), slot.getSlotNo(),
                order.getDestinationLocationId(), executionService.currentRoute(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelPickupAccessVO getPickupAccess(Long userId, Long parcelId) {
        ParcelEntity parcel = requireParcel(parcelId);
        if (!Objects.equals(userId, parcel.getRecipientUserId())) {
            throw new ResourceNotFoundException("入柜快递不存在");
        }
        StorageRecord storage = storageMapper.selectActiveByParcelId(parcelId);
        if (storage == null) {
            throw new ResourceNotFoundException("当前没有可用取件码");
        }
        LockerSlot slot = slotMapper.selectById(storage.getLockerSlotId());
        Locker locker = slot == null ? null : lockerMapper.selectById(slot.getLockerId());
        if (slot == null || locker == null) {
            throw new ResourceNotFoundException("入柜位置不存在");
        }
        return new ParcelPickupAccessVO(parcelId, storage.getId(), locker.getId(),
                locker.getLockerName(), slot.getId(), slot.getSlotNo(),
                storage.getPickupCode(), storage.getStoredTime());
    }

    @Override
    @Transactional
    public PickupRecordVO pickupParcel(Long userId, ParcelPickupDTO request) {
        StorageRecord storage = storageMapper.selectActiveByParcelIdForUpdate(request.parcelId());
        if (storage == null) {
            throw new ResourceNotFoundException("当前没有可取件的入柜记录");
        }
        ParcelEntity parcel = parcelMapper.selectByIdForUpdate(storage.getParcelId());
        if (parcel == null || !Objects.equals(userId, parcel.getRecipientUserId())) {
            throw new ResourceNotFoundException("入柜快递不存在");
        }
        if (storage.getCodeExpireTime() != null
                && storage.getCodeExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCodes.PICKUP_CODE_INVALID, "取件码已过期");
        }
        if (!matchesPickupCode(request.pickupCode(), storage)) {
            throw new BusinessException(ErrorCodes.PICKUP_CODE_INVALID, "取件码不正确");
        }

        LockerSlot slot = slotMapper.selectByIdForUpdate(storage.getLockerSlotId());
        if (slot == null || !"OCCUPIED".equals(slot.getStatus())) {
            throw conflict("格口状态异常，请联系管理员");
        }
        LocalDateTime now = LocalDateTime.now();
        if (storageMapper.update(null, new LambdaUpdateWrapper<StorageRecord>()
                .eq(StorageRecord::getId, storage.getId())
                .eq(StorageRecord::getStatus, "STORED")
                .isNull(StorageRecord::getReleasedTime)
                .set(StorageRecord::getStatus, "PICKED_UP")
                .set(StorageRecord::getReleasedTime, now)
                .set(StorageRecord::getPickupCode, null)
                .set(StorageRecord::getPickupCodeHash, null)) != 1) {
            throw conflict("入柜记录已被其他请求处理");
        }
        if (slotMapper.update(null, new LambdaUpdateWrapper<LockerSlot>()
                .eq(LockerSlot::getId, slot.getId())
                .eq(LockerSlot::getStatus, "OCCUPIED")
                .set(LockerSlot::getStatus, "AVAILABLE")) != 1) {
            throw conflict("格口状态异常，请联系管理员");
        }

        PickupRecord pickup = new PickupRecord();
        pickup.setStorageRecordId(storage.getId());
        pickup.setUserId(userId);
        pickup.setVerificationType("PICKUP_CODE");
        pickup.setResult("SUCCESS");
        pickup.setPickupTime(now);
        try {
            pickupMapper.insert(pickup);
        } catch (DuplicateKeyException ex) {
            throw conflict("该快递已有正式取件记录");
        }
        lifecycleService.changeStatus(parcel, "PICKED_UP", userId,
                "LOCKER", "用户使用四位取件码完成取件");
        return toPickupVo(pickup);
    }

    @Override
    @Transactional(readOnly = true)
    public LockerStatusVO getLockerStatus(Long lockerId) {
        Locker locker = lockerMapper.selectById(lockerId);
        if (locker == null) {
            throw new ResourceNotFoundException("柜机不存在");
        }
        List<LockerSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<LockerSlot>()
                .eq(LockerSlot::getLockerId, lockerId));
        long available = count(slots, "AVAILABLE");
        long reserved = count(slots, "RESERVED");
        long occupied = count(slots, "OCCUPIED");
        long unavailable = count(slots, "FAULT") + count(slots, "DISABLED");
        return new LockerStatusVO(locker.getId(), locker.getLockerCode(), locker.getLockerName(),
                locker.getLocationId(), locker.getOperationStatus(), slots.size(), available,
                reserved, occupied, unavailable);
    }

    private StorageRecord insertStorageWithUniqueCode(Long parcelId, Long slotId,
                                                       Long userId, LocalDateTime now) {
        for (int attempt = 0; attempt < PICKUP_CODE_ATTEMPTS; attempt++) {
            StorageRecord storage = new StorageRecord();
            storage.setParcelId(parcelId);
            storage.setLockerSlotId(slotId);
            storage.setStoredByUserId(userId);
            storage.setStatus("STORED");
            storage.setStoredTime(now);
            storage.setPickupCode(pickupCodeGenerator.generate());
            try {
                storageMapper.insert(storage);
                return storage;
            } catch (DuplicateKeyException ex) {
                if (!containsConstraint(ex, "uk_storage_active_pickup_code")) {
                    throw conflict("格口或快递已有活动入柜记录");
                }
            }
        }
        throw conflict("暂时无法生成唯一取件码，请重试");
    }

    private void completeOrder(DeliveryOrder order, LocalDateTime now) {
        if (orderMapper.update(null, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, "ARRIVED")
                .set(DeliveryOrder::getStatus, "COMPLETED")
                .set(DeliveryOrder::getCompletedTime, now)) != 1) {
            throw conflict("配送订单已被其他请求处理");
        }
    }

    private void completeArrivedDropoff(Long orderId, LocalDateTime now) {
        RouteStop stop = stopMapper.selectLatestArrivedDropoffForUpdate(orderId);
        if (stop == null) {
            throw new BusinessException(ErrorCodes.ROUTE_STEP_CONFLICT,
                    "订单缺少已到达的送达节点", HttpStatus.CONFLICT);
        }
        if (stopMapper.update(null, new LambdaUpdateWrapper<RouteStop>()
                .eq(RouteStop::getId, stop.getId())
                .eq(RouteStop::getStopStatus, "ARRIVED")
                .set(RouteStop::getStopStatus, "COMPLETED")
                .set(RouteStop::getActualArrivalTime, now)) != 1) {
            throw new BusinessException(ErrorCodes.ROUTE_STEP_CONFLICT,
                    "送达节点已被其他请求处理", HttpStatus.CONFLICT);
        }
    }

    private void updateCourierAfterStorage(Courier courier, Long locationId, LocalDateTime now) {
        long activeCount = orderMapper.countActiveByCourierId(courier.getId());
        int capacity = courier.getMaxActiveOrders() == null ? 8 : courier.getMaxActiveOrders();
        String availability = activeCount >= capacity ? "BUSY" : "AVAILABLE";
        if (courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getCurrentLocationId, locationId)
                .set(Courier::getLocationUpdateTime, now)
                .set(Courier::getAvailabilityStatus, availability)) != 1) {
            throw conflict("配送员位置已被其他请求修改");
        }
        courier.setCurrentLocationId(locationId);
        courier.setLocationUpdateTime(now);
        courier.setAvailabilityStatus(availability);
    }

    private Courier requireCourier(Long userId) {
        Courier courier = courierMapper.selectByUserId(userId);
        if (courier == null || !"ACTIVE".equals(courier.getEmploymentStatus())) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND,
                    "有效配送员档案不存在", HttpStatus.NOT_FOUND);
        }
        return courier;
    }

    private DeliveryOrder requireOrder(Long orderId) {
        DeliveryOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送任务不存在");
        }
        return order;
    }

    private ParcelEntity requireParcel(Long parcelId) {
        ParcelEntity parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        return parcel;
    }

    private void ensureAssignedArrivedOrder(Courier courier, DeliveryOrder order) {
        if (!Objects.equals(courier.getId(), order.getAssignedCourierId())) {
            throw new ForbiddenException("不能操作其他配送员的任务");
        }
        if (!"ACTIVE".equals(courier.getEmploymentStatus())) {
            throw new ForbiddenException("配送员账号当前不可执行配送任务");
        }
        if (!"ARRIVED".equals(order.getStatus())) {
            throw conflict("只有已到达目的地的订单可以选择格口入柜");
        }
    }

    private Locker requireDestinationLocker(DeliveryOrder order, ParcelEntity parcel) {
        Locker locker = lockerMapper.selectById(parcel.getTargetLockerId());
        if (locker == null || !"ONLINE".equals(locker.getOperationStatus())) {
            throw new BusinessException(ErrorCodes.LOCKER_NOT_FOUND, "目标快递柜不存在或不在线");
        }
        if (!Objects.equals(locker.getLocationId(), order.getDestinationLocationId())
                || !Objects.equals(parcel.getDestinationLocationId(), order.getDestinationLocationId())) {
            throw conflict("订单目的地与目标快递柜地点不一致");
        }
        return locker;
    }

    private void ensureSizeCompatible(String parcelSize, String slotSize) {
        int parcelRank = SIZE_RANK.getOrDefault(parcelSize, Integer.MAX_VALUE);
        int slotRank = SIZE_RANK.getOrDefault(slotSize, 0);
        if (slotRank < parcelRank) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口尺寸小于包裹尺寸");
        }
    }

    private boolean matchesPickupCode(String suppliedCode, StorageRecord storage) {
        if (storage.getPickupCode() != null) {
            return MessageDigest.isEqual(suppliedCode.getBytes(StandardCharsets.US_ASCII),
                    storage.getPickupCode().getBytes(StandardCharsets.US_ASCII));
        }
        return storage.getPickupCodeHash() != null
                && passwordUtil.matches(suppliedCode, storage.getPickupCodeHash());
    }

    private boolean containsConstraint(Throwable throwable, String constraint) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(constraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long count(List<LockerSlot> slots, String status) {
        return slots.stream().filter(slot -> status.equals(slot.getStatus())).count();
    }

    private PickupRecordVO toPickupVo(PickupRecord pickup) {
        return new PickupRecordVO(pickup.getId(), pickup.getStorageRecordId(), pickup.getUserId(),
                pickup.getVerificationType(), pickup.getResult(), pickup.getPickupTime());
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCodes.PICKUP_CONFLICT, message, HttpStatus.CONFLICT);
    }
}
