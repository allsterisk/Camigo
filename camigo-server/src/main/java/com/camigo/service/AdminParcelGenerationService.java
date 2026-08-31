package com.camigo.service;

import com.camigo.common.ErrorCodes;
import com.camigo.dispatch.IntelligentDispatchService;
import com.camigo.dto.AdminParcelGenerateDTO;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Locker;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.User;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.vo.AdminGeneratedParcelVO;
import com.camigo.vo.AdminParcelGenerationOptionsVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员生成联调包裹的正式领域事务。 */
@Service
public class AdminParcelGenerationService {
    private static final int TRACKING_RETRIES = 12;
    private final UserMapper userMapper;
    private final CampusLocationMapper locationMapper;
    private final LockerMapper lockerMapper;
    private final ParcelMapper parcelMapper;
    private final DeliveryOrderMapper orderMapper;
    private final ParcelLifecycleService lifecycleService;
    private final IntelligentDispatchService dispatchService;
    private final ParcelRandomDataGenerator randomDataGenerator;

    public AdminParcelGenerationService(UserMapper userMapper,
                                        CampusLocationMapper locationMapper,
                                        LockerMapper lockerMapper,
                                        ParcelMapper parcelMapper,
                                        DeliveryOrderMapper orderMapper,
                                        ParcelLifecycleService lifecycleService,
                                        IntelligentDispatchService dispatchService,
                                        ParcelRandomDataGenerator randomDataGenerator) {
        this.userMapper = userMapper;
        this.locationMapper = locationMapper;
        this.lockerMapper = lockerMapper;
        this.parcelMapper = parcelMapper;
        this.orderMapper = orderMapper;
        this.lifecycleService = lifecycleService;
        this.dispatchService = dispatchService;
        this.randomDataGenerator = randomDataGenerator;
    }

    @Transactional(readOnly = true)
    public AdminParcelGenerationOptionsVO options(long adminUserId) {
        requireAdmin(adminUserId);
        return new AdminParcelGenerationOptionsVO(
                userMapper.selectActiveStudentOptions(),
                locationMapper.selectPickupOptions(),
                locationMapper.selectDeliveryOptions(),
                List.of("S", "M", "L", "XL"), 10);
    }

    /** 同一批次完全成功或完全回滚，并为每个包裹立即启动智能派单。 */
    @Transactional
    public List<AdminGeneratedParcelVO> generate(long adminUserId, AdminParcelGenerateDTO request) {
        requireAdmin(adminUserId);
        User recipient = requireStudent(request.recipientUserId());
        CampusLocation pickup = requirePickup(request.pickupLocationId());
        CampusLocation destination = requireDestination(request.destinationLocationId());
        String sizeClass = request.sizeClass().trim().toUpperCase(Locale.ROOT);
        Locker locker = lockerMapper.selectCompatibleOnlineAtLocation(destination.getId(), sizeClass);
        if (locker == null) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "配送目的地没有在线且尺寸兼容的快递柜", HttpStatus.CONFLICT);
        }
        LocalDateTime arriveTime = request.arriveTime() == null
                ? LocalDateTime.now() : request.arriveTime();

        List<AdminGeneratedParcelVO> results = new ArrayList<>(request.count());
        for (int index = 0; index < request.count(); index++) {
            ParcelEntity parcel = insertParcelWithRetry(recipient, destination, locker,
                    sizeClass, request, arriveTime);
            lifecycleService.recordInitialStatus(parcel, adminUserId, "ADMIN");
            lifecycleService.changeStatus(parcel, "WAITING_ASSIGNMENT", adminUserId,
                    "ADMIN", "管理员生成包裹并进入智能派单");

            DeliveryOrder order = new DeliveryOrder();
            order.setOrderNo(generateOrderNo());
            order.setParcelId(parcel.getId());
            order.setPickupLocationId(pickup.getId());
            order.setDestinationLocationId(destination.getId());
            order.setPriority("NORMAL");
            order.setStatus("CREATED");
            order.setDispatchRound(0);
            order.setManualDispatchRequired(0);
            orderMapper.insert(order);
            dispatchService.startDispatch(order.getId());
            DeliveryOrder dispatched = orderMapper.selectById(order.getId());
            results.add(new AdminGeneratedParcelVO(parcel.getId(), parcel.getTrackingNo(),
                    parcel.getCarrierCode(), order.getId(), order.getOrderNo(),
                    parcel.getCurrentStatus(), dispatched.getStatus(),
                    dispatched.getDispatchRound(), locker.getId()));
        }
        return List.copyOf(results);
    }

    private ParcelEntity insertParcelWithRetry(User recipient, CampusLocation destination,
                                                Locker locker, String sizeClass,
                                                AdminParcelGenerateDTO request,
                                                LocalDateTime arriveTime) {
        for (int attempt = 0; attempt < TRACKING_RETRIES; attempt++) {
            ParcelRandomDataGenerator.RandomParcelData random = randomDataGenerator.next();
            ParcelEntity parcel = new ParcelEntity();
            parcel.setTrackingNo(random.trackingNo());
            parcel.setCarrierCode(random.carrierCode());
            parcel.setRecipientUserId(recipient.getId());
            parcel.setSenderName(random.senderName());
            parcel.setSenderPhoneMasked(random.senderPhoneMasked());
            parcel.setDestinationLocationId(destination.getId());
            parcel.setTargetLockerId(locker.getId());
            parcel.setSizeClass(sizeClass);
            parcel.setWeightKg(request.weightKg());
            parcel.setCurrentStatus("REGISTERED");
            parcel.setArriveTime(arriveTime);
            try {
                parcelMapper.insert(parcel);
                return parcel;
            } catch (DuplicateKeyException ex) {
                if (attempt == TRACKING_RETRIES - 1) {
                    throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                            "随机运单号连续冲突，请重试", HttpStatus.CONFLICT);
                }
            }
        }
        throw new IllegalStateException("无法生成运单号");
    }

    private User requireStudent(long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new ResourceNotFoundException("收件学生不存在");
        if (!"ACTIVE".equals(user.getStatus())
                || !userMapper.selectRoleCodes(userId).contains("USER")) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "收件用户不是有效学生用户", HttpStatus.BAD_REQUEST);
        }
        return user;
    }

    private CampusLocation requirePickup(long locationId) {
        CampusLocation location = locationMapper.selectById(locationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())
                || !Integer.valueOf(1).equals(location.getPickupEnabled())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "取件服务站不存在或不可取件", HttpStatus.BAD_REQUEST);
        }
        return location;
    }

    private CampusLocation requireDestination(long locationId) {
        CampusLocation location = locationMapper.selectById(locationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())
                || !Integer.valueOf(1).equals(location.getDeliveryEnabled())
                || Integer.valueOf(1).equals(location.getPickupEnabled())) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT,
                    "配送目的地不存在、不可配送或属于服务站", HttpStatus.BAD_REQUEST);
        }
        return location;
    }

    private void requireAdmin(long userId) {
        List<String> roles = userMapper.selectRoleCodes(userId);
        if (roles == null || !roles.contains("ADMIN")) {
            throw new ForbiddenException("只有管理员可以生成包裹");
        }
    }

    private String generateOrderNo() {
        return "DO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
