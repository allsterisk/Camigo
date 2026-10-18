package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.common.ErrorCodes;
import com.camigo.common.PageResult;
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
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.service.ParcelService;
import com.camigo.vo.ParcelVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 快递服务实现。角色权限与快递所有权均在事务边界内二次校验。
 */
@Service
public class ParcelServiceImpl implements ParcelService {

    private static final String USER_ROLE = "USER";
    private static final String COURIER_ROLE = "COURIER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final Set<String> PUBLIC_STATUSES = Set.of(
            "ARRIVED", "DELIVERING", "STORED", "PICKED", "COMPLETED");
    private static final Map<String, Set<String>> PUBLIC_TO_INTERNAL_STATUSES = Map.of(
            "ARRIVED", Set.of("REGISTERED", "SORTING", "WAITING_ASSIGNMENT"),
            "DELIVERING", Set.of("DELIVERING"),
            "STORED", Set.of("STORED", "READY_PICKUP"),
            "PICKED", Set.of("PICKED_UP"),
            "COMPLETED", Set.of("COMPLETED"));

    private final ParcelMapper parcelMapper;
    private final UserMapper userMapper;
    private final LockerMapper lockerMapper;
    private final CampusLocationMapper locationMapper;
    private final CourierMapper courierMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final ParcelLifecycleService lifecycleService;

    public ParcelServiceImpl(ParcelMapper parcelMapper,
                             UserMapper userMapper,
                             LockerMapper lockerMapper,
                             CampusLocationMapper locationMapper,
                             CourierMapper courierMapper,
                             DeliveryOrderMapper deliveryOrderMapper,
                             ParcelLifecycleService lifecycleService) {
        this.parcelMapper = parcelMapper;
        this.userMapper = userMapper;
        this.lockerMapper = lockerMapper;
        this.locationMapper = locationMapper;
        this.courierMapper = courierMapper;
        this.deliveryOrderMapper = deliveryOrderMapper;
        this.lifecycleService = lifecycleService;
    }

    @Override
    @Transactional
    public ParcelVO create(Long userId, ParcelCreateDTO request) {
        requireAnyRole(userId, ADMIN_ROLE);
        User recipient = requireActiveRecipient(request.phone().trim(), request.receiver().trim());
        Locker locker = requireOnlineLocker(request.lockerId());
        CampusLocation location = requireActiveLocation(locker.getLocationId());

        ParcelEntity parcel = new ParcelEntity();
        parcel.setTrackingNo(request.trackingNo().trim().toUpperCase());
        parcel.setCarrierCode(request.carrierCode().trim().toUpperCase());
        parcel.setRecipientUserId(recipient.getId());
        parcel.setSenderName(request.sender().trim());
        parcel.setSenderPhoneMasked(null);
        parcel.setDestinationLocationId(locker.getLocationId());
        parcel.setTargetLockerId(locker.getId());
        parcel.setSizeClass(request.sizeClass().trim().toUpperCase());
        parcel.setWeightKg(request.weightKg());
        parcel.setCurrentStatus("REGISTERED");
        parcel.setArriveTime(LocalDateTime.now());
        try {
            parcelMapper.insert(parcel);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "快递单号已存在");
        }
        lifecycleService.recordInitialStatus(parcel, userId, ADMIN_ROLE);
        return toVo(parcel, false, recipient.getRealName(), locker.getLockerName(),
                location.getAddressDetail());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ParcelVO> listAll(Long userId, long page, long pageSize,
                                        String status, String trackingNo) {
        requireAnyRole(userId, ADMIN_ROLE);
        return queryPage(page, pageSize, status, trackingNo, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ParcelVO> listMy(Long userId, long page, long pageSize,
                                       String status, String trackingNo) {
        requireAnyRole(userId, USER_ROLE);
        return queryPage(page, pageSize, status, trackingNo, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelVO getById(Long userId, Long parcelId) {
        ParcelEntity parcel = requireParcel(parcelId);
        Set<String> roles = rolesOf(userId);
        if (roles.contains(ADMIN_ROLE) || parcel.getRecipientUserId().equals(userId)
                || (roles.contains(COURIER_ROLE) && isAssignedCourier(userId, parcelId))) {
            return toVo(parcel);
        }
        throw new ForbiddenException("没有查看该快递的权限");
    }

    @Override
    @Transactional
    public ParcelVO updateStatus(Long userId, Long parcelId, ParcelStatusUpdateDTO request) {
        ParcelEntity parcel = requireParcel(parcelId);
        Set<String> roles = rolesOf(userId);
        String targetStatus = request.status().trim().toUpperCase();

        if ("EXCEPTION".equals(parcel.getCurrentStatus())) {
            throw invalidStatus("异常包裹只能由管理员通过异常处理接口恢复或确认退回");
        }

        if ("COMPLETED".equals(targetStatus)) {
            boolean owner = parcel.getRecipientUserId().equals(userId) && roles.contains(USER_ROLE);
            if (!owner && !roles.contains(ADMIN_ROLE)) {
                throw new ForbiddenException("只有收件用户或管理员可以确认快递完成");
            }
            if (!"PICKED_UP".equals(parcel.getCurrentStatus())) {
                throw invalidStatus("只有已取件快递可以确认完成");
            }
            lifecycleService.changeStatus(parcel, "COMPLETED", userId,
                    roles.contains(ADMIN_ROLE) ? ADMIN_ROLE : USER_ROLE, request.remark());
            return toVo(parcel);
        }

        if (!roles.contains(ADMIN_ROLE) && !roles.contains(COURIER_ROLE)) {
            throw new ForbiddenException("只有管理员或配送员可以更新配送状态");
        }
        if (roles.contains(COURIER_ROLE) && !roles.contains(ADMIN_ROLE)
                && !isAssignedCourier(userId, parcelId)) {
            throw new ForbiddenException("不能更新其他配送员任务中的快递");
        }
        switch (targetStatus) {
            case "ARRIVED" -> {
                if (!PUBLIC_TO_INTERNAL_STATUSES.get("ARRIVED").contains(parcel.getCurrentStatus())) {
                    throw invalidStatus("快递不能回退到已到校状态");
                }
            }
            case "DELIVERING" -> throw invalidStatus(
                    "快递只能在配送员接受派单后进入配送中，请使用开始配送启动智能派单");
            case "STORED" -> throw invalidStatus("快递入柜请使用快递柜入柜接口");
            case "PICKED" -> throw invalidStatus("用户取件请使用快递柜取件接口");
            default -> throw invalidStatus("不支持的快递状态");
        }
        return toVo(parcel);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long parcelId) {
        requireAnyRole(userId, ADMIN_ROLE);
        ParcelEntity parcel = requireParcel(parcelId);
        if (!"REGISTERED".equals(parcel.getCurrentStatus())) {
            throw invalidStatus("只有尚未进入配送流程的快递可以删除");
        }
        try {
            if (parcelMapper.deleteById(parcelId) != 1) {
                throw new ResourceNotFoundException("快递不存在");
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "快递已有业务记录，不能删除");
        }
    }

    private PageResult<ParcelVO> queryPage(long page, long pageSize, String status,
                                            String trackingNo, Long recipientUserId) {
        LambdaQueryWrapper<ParcelEntity> query = new LambdaQueryWrapper<ParcelEntity>()
                .eq(recipientUserId != null, ParcelEntity::getRecipientUserId, recipientUserId)
                .like(StringUtils.hasText(trackingNo), ParcelEntity::getTrackingNo,
                        StringUtils.hasText(trackingNo) ? trackingNo.trim() : null)
                .orderByDesc(ParcelEntity::getCreateTime);
        applyPublicStatusFilter(query, status);

        Page<ParcelEntity> result = parcelMapper.selectPage(new Page<>(page, pageSize), query);
        List<ParcelEntity> parcels = result.getRecords();
        List<Long> parcelIds = parcels.stream().map(ParcelEntity::getId).toList();
        Set<Long> startAvailableIds = parcelIds.isEmpty() ? Set.of() : Set.copyOf(
                deliveryOrderMapper.selectDeliveryStartAvailableParcelIds(parcelIds));
        Map<Long, String> recipientNames = loadRecipientNames(parcels);
        Map<Long, LockerDisplay> lockerDisplays = loadLockerDisplays(parcels);
        List<ParcelVO> records = parcels.stream()
                .map(parcel -> {
                    LockerDisplay locker = lockerDisplays.get(parcel.getTargetLockerId());
                    return toVo(parcel, startAvailableIds.contains(parcel.getId()),
                            recipientNames.get(parcel.getRecipientUserId()),
                            locker == null ? null : locker.lockerName(),
                            locker == null ? null : locker.addressDetail());
                })
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void applyPublicStatusFilter(LambdaQueryWrapper<ParcelEntity> query, String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        String normalized = status.trim().toUpperCase();
        if (!PUBLIC_STATUSES.contains(normalized)) {
            throw invalidStatus("不支持的快递状态筛选条件");
        }
        query.in(ParcelEntity::getCurrentStatus, PUBLIC_TO_INTERNAL_STATUSES.get(normalized));
    }

    private User requireActiveRecipient(String phone, String receiverName) {
        User recipient = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (recipient == null) {
            throw new ResourceNotFoundException("收件用户不存在");
        }
        if (!"ACTIVE".equals(recipient.getStatus())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "收件用户账号不可用");
        }
        if (!receiverName.equals(recipient.getRealName())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "收件人姓名与手机号不匹配", HttpStatus.BAD_REQUEST);
        }
        if (!rolesOf(recipient.getId()).contains(USER_ROLE)) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "收件账号不是普通用户", HttpStatus.BAD_REQUEST);
        }
        return recipient;
    }

    private Locker requireOnlineLocker(Long lockerId) {
        Locker locker = lockerMapper.selectById(lockerId);
        if (locker == null) {
            throw new ResourceNotFoundException("目标快递柜不存在");
        }
        if (!"ONLINE".equals(locker.getOperationStatus())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "目标快递柜当前不可用");
        }
        return locker;
    }

    private ParcelEntity requireParcel(Long parcelId) {
        ParcelEntity parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        return parcel;
    }

    private CampusLocation requireActiveLocation(Long locationId) {
        CampusLocation location = locationMapper.selectById(locationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "快递柜所在位置不存在或已停用");
        }
        return location;
    }

    private boolean isAssignedCourier(Long userId, Long parcelId) {
        Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getUserId, userId));
        if (courier == null) {
            return false;
        }
        Long count = deliveryOrderMapper.selectCount(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getParcelId, parcelId)
                .eq(DeliveryOrder::getAssignedCourierId, courier.getId()));
        return count != null && count > 0;
    }

    private Set<String> requireAnyRole(Long userId, String... requiredRoles) {
        Set<String> roles = rolesOf(userId);
        for (String role : requiredRoles) {
            if (roles.contains(role)) {
                return roles;
            }
        }
        throw new ForbiddenException("没有执行该操作的权限");
    }

    private Set<String> rolesOf(Long userId) {
        List<String> roles = userMapper.selectRoleCodes(userId);
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    private BusinessException invalidStatus(String message) {
        return new BusinessException(ErrorCodes.PARCEL_STATUS_INVALID, message, HttpStatus.BAD_REQUEST);
    }

    private ParcelVO toVo(ParcelEntity parcel) {
        Long orderId = deliveryOrderMapper.selectLatestIdByParcelId(parcel.getId());
        DeliveryOrder order = orderId == null ? null : deliveryOrderMapper.selectById(orderId);
        boolean startAvailable = order != null && ("CREATED".equals(order.getStatus())
                || ("ASSIGNING".equals(order.getStatus())
                    && Integer.valueOf(1).equals(order.getManualDispatchRequired()))
                || ("FAILED".equals(order.getStatus())
                    && "SORTING".equals(parcel.getCurrentStatus())));
        String recipientName = null;
        if (parcel.getRecipientUserId() != null) {
            User recipient = userMapper.selectById(parcel.getRecipientUserId());
            recipientName = recipient == null ? null : recipient.getRealName();
        }
        String lockerName = null;
        String lockerAddress = null;
        if (parcel.getTargetLockerId() != null) {
            Locker locker = lockerMapper.selectById(parcel.getTargetLockerId());
            if (locker != null) {
                lockerName = locker.getLockerName();
                if (locker.getLocationId() != null) {
                    CampusLocation location = locationMapper.selectById(locker.getLocationId());
                    lockerAddress = location == null ? null : location.getAddressDetail();
                }
            }
        }
        return toVo(parcel, startAvailable, recipientName, lockerName, lockerAddress);
    }

    private ParcelVO toVo(ParcelEntity parcel, boolean deliveryStartAvailable,
                          String recipientName, String targetLockerName,
                          String targetLockerAddress) {
        return new ParcelVO(parcel.getId(), parcel.getTrackingNo(), parcel.getCarrierCode(),
                parcel.getRecipientUserId(), recipientName, parcel.getSenderName(),
                parcel.getSenderPhoneMasked(), parcel.getDestinationLocationId(),
                parcel.getTargetLockerId(), targetLockerName, targetLockerAddress,
                parcel.getSizeClass(), parcel.getWeightKg(),
                toPublicStatus(parcel.getCurrentStatus()), deliveryStartAvailable,
                parcel.getArriveTime(), parcel.getCreateTime(), parcel.getUpdateTime());
    }

    private Map<Long, String> loadRecipientNames(List<ParcelEntity> parcels) {
        List<Long> ids = parcels.stream()
                .map(ParcelEntity::getRecipientUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (User user : userMapper.selectBatchIds(ids)) {
            names.put(user.getId(), user.getRealName());
        }
        return names;
    }

    private Map<Long, LockerDisplay> loadLockerDisplays(List<ParcelEntity> parcels) {
        List<Long> lockerIds = parcels.stream()
                .map(ParcelEntity::getTargetLockerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (lockerIds.isEmpty()) {
            return Map.of();
        }
        List<Locker> lockers = lockerMapper.selectBatchIds(lockerIds);
        List<Long> locationIds = lockers.stream()
                .map(Locker::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> addresses = new HashMap<>();
        if (!locationIds.isEmpty()) {
            for (CampusLocation location : locationMapper.selectBatchIds(locationIds)) {
                addresses.put(location.getId(), location.getAddressDetail());
            }
        }
        Map<Long, LockerDisplay> displays = new HashMap<>();
        for (Locker locker : lockers) {
            displays.put(locker.getId(), new LockerDisplay(locker.getLockerName(),
                    addresses.get(locker.getLocationId())));
        }
        return displays;
    }

    private record LockerDisplay(String lockerName, String addressDetail) {
    }

    private String toPublicStatus(String internalStatus) {
        return switch (internalStatus) {
            case "REGISTERED", "SORTING", "WAITING_ASSIGNMENT" -> "ARRIVED";
            case "DELIVERING" -> "DELIVERING";
            case "STORED", "READY_PICKUP" -> "STORED";
            case "PICKED_UP" -> "PICKED";
            case "COMPLETED" -> "COMPLETED";
            default -> internalStatus;
        };
    }
}
