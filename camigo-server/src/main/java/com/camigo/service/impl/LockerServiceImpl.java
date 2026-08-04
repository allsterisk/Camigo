package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dto.ParcelPickupDTO;
import com.camigo.dto.ParcelStorageDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.Locker;
import com.camigo.entity.LockerSlot;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.PickupRecord;
import com.camigo.entity.StorageRecord;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.LockerSlotMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.PickupRecordMapper;
import com.camigo.mapper.StorageRecordMapper;
import com.camigo.service.LockerService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.utils.PasswordUtil;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.PickupRecordVO;
import com.camigo.vo.StorageRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能柜业务实现，使用行锁和数据库条件唯一约束共同保护格口占用。
 */
@Service
public class LockerServiceImpl implements LockerService {

    private static final Map<String, Integer> SIZE_RANK = Map.of(
            "S", 1, "M", 2, "L", 3, "XL", 4);

    private final LockerMapper lockerMapper;
    private final LockerSlotMapper slotMapper;
    private final StorageRecordMapper storageMapper;
    private final PickupRecordMapper pickupMapper;
    private final ParcelMapper parcelMapper;
    private final CourierMapper courierMapper;
    private final ParcelLifecycleService lifecycleService;
    private final PasswordUtil passwordUtil;

    public LockerServiceImpl(LockerMapper lockerMapper,
                             LockerSlotMapper slotMapper,
                             StorageRecordMapper storageMapper,
                             PickupRecordMapper pickupMapper,
                             ParcelMapper parcelMapper,
                             CourierMapper courierMapper,
                             ParcelLifecycleService lifecycleService,
                             PasswordUtil passwordUtil) {
        this.lockerMapper = lockerMapper;
        this.slotMapper = slotMapper;
        this.storageMapper = storageMapper;
        this.pickupMapper = pickupMapper;
        this.parcelMapper = parcelMapper;
        this.courierMapper = courierMapper;
        this.lifecycleService = lifecycleService;
        this.passwordUtil = passwordUtil;
    }

    @Override
    @Transactional
    public StorageRecordVO storeParcel(Long userId, ParcelStorageDTO request) {
        requireActiveCourier(userId);
        LockerSlot slot = slotMapper.selectByIdForUpdate(request.lockerSlotId());
        if (slot == null) {
            throw new ResourceNotFoundException("格口不存在");
        }
        Locker locker = lockerMapper.selectById(slot.getLockerId());
        if (locker == null || !"ONLINE".equals(locker.getOperationStatus())) {
            throw new BusinessException(ErrorCodes.LOCKER_NOT_FOUND, "柜机不存在或不在线");
        }
        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口当前不可用");
        }

        ParcelEntity parcel = parcelMapper.selectById(request.parcelId());
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        if (!"DELIVERING".equals(parcel.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.PARCEL_STATUS_INVALID, "只有配送中的快递可以入柜");
        }
        if (SIZE_RANK.getOrDefault(slot.getSizeClass(), 0)
                < SIZE_RANK.getOrDefault(parcel.getSizeClass(), Integer.MAX_VALUE)) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口尺寸小于包裹尺寸");
        }

        LocalDateTime now = LocalDateTime.now();
        StorageRecord storage = new StorageRecord();
        storage.setParcelId(parcel.getId());
        storage.setLockerSlotId(slot.getId());
        storage.setStoredByUserId(userId);
        storage.setStatus("STORED");
        storage.setStoredTime(now);
        storage.setPickupCodeHash(passwordUtil.encode(request.pickupCode()));
        storage.setCodeExpireTime(request.codeExpireTime());
        try {
            storageMapper.insert(storage);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.PICKUP_CONFLICT, "格口或快递已有活动入柜记录");
        }

        int slotUpdated = slotMapper.update(null, new LambdaUpdateWrapper<LockerSlot>()
                .eq(LockerSlot::getId, slot.getId())
                .eq(LockerSlot::getStatus, "AVAILABLE")
                .set(LockerSlot::getStatus, "OCCUPIED"));
        if (slotUpdated != 1) {
            throw new BusinessException(ErrorCodes.SLOT_NOT_AVAILABLE, "格口已被占用");
        }
        lifecycleService.changeStatus(parcel, "READY_PICKUP", userId,
                "LOCKER", "包裹已存入柜机 " + locker.getLockerCode() + " 格口 " + slot.getSlotNo());
        return toStorageVo(storage);
    }

    @Override
    @Transactional
    public PickupRecordVO pickupParcel(Long userId, ParcelPickupDTO request) {
        StorageRecord storage = storageMapper.selectByIdForUpdate(request.storageRecordId());
        if (storage == null) {
            throw new ResourceNotFoundException("入柜记录不存在");
        }
        if (!"STORED".equals(storage.getStatus())) {
            throw new BusinessException(ErrorCodes.PICKUP_CONFLICT, "包裹已取出或不可取件");
        }
        if (storage.getCodeExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCodes.PICKUP_CODE_INVALID, "取件码已过期");
        }

        ParcelEntity parcel = parcelMapper.selectById(storage.getParcelId());
        if (parcel == null || !userId.equals(parcel.getRecipientUserId())) {
            throw new ResourceNotFoundException("入柜包裹不存在");
        }
        if (!passwordUtil.matches(request.pickupCode(), storage.getPickupCodeHash())) {
            throw new BusinessException(ErrorCodes.PICKUP_CODE_INVALID, "取件码不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        storage.setStatus("PICKED_UP");
        storage.setReleasedTime(now);
        storageMapper.updateById(storage);

        int slotUpdated = slotMapper.update(null, new LambdaUpdateWrapper<LockerSlot>()
                .eq(LockerSlot::getId, storage.getLockerSlotId())
                .eq(LockerSlot::getStatus, "OCCUPIED")
                .set(LockerSlot::getStatus, "AVAILABLE"));
        if (slotUpdated != 1) {
            throw new BusinessException(ErrorCodes.PICKUP_CONFLICT, "格口状态异常，请联系管理员");
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
            throw new BusinessException(ErrorCodes.PICKUP_CONFLICT, "该包裹已有取件记录");
        }
        lifecycleService.changeStatus(parcel, "PICKED_UP", userId,
                "LOCKER", "用户完成柜机取件");
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

    private Courier requireActiveCourier(Long userId) {
        Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getUserId, userId)
                .eq(Courier::getEmploymentStatus, "ACTIVE"));
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND, "有效配送员档案不存在");
        }
        return courier;
    }

    private long count(List<LockerSlot> slots, String status) {
        return slots.stream().filter(slot -> status.equals(slot.getStatus())).count();
    }

    private StorageRecordVO toStorageVo(StorageRecord storage) {
        return new StorageRecordVO(storage.getId(), storage.getParcelId(), storage.getLockerSlotId(),
                storage.getStatus(), storage.getStoredTime(), storage.getCodeExpireTime());
    }

    private PickupRecordVO toPickupVo(PickupRecord pickup) {
        return new PickupRecordVO(pickup.getId(), pickup.getStorageRecordId(), pickup.getUserId(),
                pickup.getVerificationType(), pickup.getResult(), pickup.getPickupTime());
    }
}
