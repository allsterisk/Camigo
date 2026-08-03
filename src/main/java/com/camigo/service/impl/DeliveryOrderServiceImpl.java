package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.entity.CampusLocation;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.service.DeliveryOrderService;
import com.camigo.service.ParcelLifecycleService;
import com.camigo.vo.DeliveryOrderVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 配送订单服务实现。
 */
@Service
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "ACCEPTED", Set.of("PICKING_UP", "CANCELLED", "FAILED"),
            "PICKING_UP", Set.of("IN_TRANSIT", "CANCELLED", "FAILED"),
            "IN_TRANSIT", Set.of("ARRIVED", "CANCELLED", "FAILED"),
            "ARRIVED", Set.of("COMPLETED", "FAILED"));

    private final DeliveryOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final CourierMapper courierMapper;
    private final CampusLocationMapper locationMapper;
    private final ParcelLifecycleService parcelLifecycleService;

    public DeliveryOrderServiceImpl(DeliveryOrderMapper orderMapper,
                                    ParcelMapper parcelMapper,
                                    CourierMapper courierMapper,
                                    CampusLocationMapper locationMapper,
                                    ParcelLifecycleService parcelLifecycleService) {
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.courierMapper = courierMapper;
        this.locationMapper = locationMapper;
        this.parcelLifecycleService = parcelLifecycleService;
    }

    @Override
    @Transactional
    public DeliveryOrderVO create(Long userId, DeliveryOrderCreateDTO request) {
        ParcelEntity parcel = parcelMapper.selectOne(new LambdaQueryWrapper<ParcelEntity>()
                .eq(ParcelEntity::getId, request.parcelId())
                .eq(ParcelEntity::getRecipientUserId, userId));
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        if (!Set.of("REGISTERED", "SORTING", "WAITING_ASSIGNMENT").contains(parcel.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID, "当前快递状态不能创建配送任务");
        }

        Long destinationId = request.destinationLocationId() == null
                ? parcel.getDestinationLocationId() : request.destinationLocationId();
        CampusLocation location = locationMapper.selectById(destinationId);
        if (location == null || !"ACTIVE".equals(location.getStatus())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送目的地不存在或已停用");
        }

        DeliveryOrder order = new DeliveryOrder();
        order.setOrderNo(generateOrderNo());
        order.setParcelId(parcel.getId());
        order.setDestinationLocationId(destinationId);
        order.setPriority(StringUtils.hasText(request.priority())
                ? request.priority().trim().toUpperCase() : "NORMAL");
        order.setStatus("CREATED");
        order.setPredictedMinutes(request.predictedMinutes());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "该快递已有活动配送任务");
        }
        parcelLifecycleService.changeStatus(parcel, "WAITING_ASSIGNMENT", userId,
                "USER", "创建配送任务 " + order.getOrderNo());
        return toVo(order);
    }

    @Override
    @Transactional
    public DeliveryOrderVO accept(Long userId, Long orderId) {
        Courier courier = requireAvailableCourier(userId);
        DeliveryOrder order = requireLockedOrder(orderId);
        if ("ACCEPTED".equals(order.getStatus()) && courier.getId().equals(order.getAssignedCourierId())) {
            return toVo(order);
        }
        if (!Set.of("CREATED", "ASSIGNING", "ASSIGNED").contains(order.getStatus())) {
            throw new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID, "当前配送任务不可接单");
        }
        if (order.getAssignedCourierId() != null
                && !courier.getId().equals(order.getAssignedCourierId())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送任务已分配给其他配送员");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setAssignedCourierId(courier.getId());
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(now);
        orderMapper.updateById(order);
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, courier.getId())
                .set(Courier::getAvailabilityStatus, "BUSY"));

        ParcelEntity parcel = requireParcel(order.getParcelId());
        parcelLifecycleService.changeStatus(parcel, "DELIVERING", userId,
                "COURIER", "配送员接单 " + order.getOrderNo());
        return toVo(order);
    }

    @Override
    @Transactional
    public DeliveryOrderVO updateStatus(Long userId, DeliveryOrderStatusDTO request) {
        Courier courier = requireCourier(userId);
        DeliveryOrder order = requireLockedOrder(request.orderId());
        if (!courier.getId().equals(order.getAssignedCourierId())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "不能修改其他配送员的任务");
        }
        String targetStatus = request.status().trim().toUpperCase();
        if (targetStatus.equals(order.getStatus())) {
            return toVo(order);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of()).contains(targetStatus)) {
            throw new BusinessException(ErrorCodes.DELIVERY_STATUS_INVALID,
                    "配送状态不能从 " + order.getStatus() + " 变更为 " + targetStatus);
        }

        String previousStatus = order.getStatus();
        order.setStatus(targetStatus);
        if (Set.of("COMPLETED", "CANCELLED", "FAILED").contains(targetStatus)) {
            order.setCompletedTime(LocalDateTime.now());
        }
        int updated = orderMapper.update(order, new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getId, order.getId())
                .eq(DeliveryOrder::getStatus, previousStatus));
        if (updated != 1) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送状态已被其他请求修改");
        }

        ParcelEntity parcel = requireParcel(order.getParcelId());
        if (Set.of("PICKING_UP", "IN_TRANSIT").contains(targetStatus)
                && "WAITING_ASSIGNMENT".equals(parcel.getCurrentStatus())) {
            parcelLifecycleService.changeStatus(parcel, "DELIVERING", userId,
                    "COURIER", "配送状态更新为 " + targetStatus);
        } else if ("COMPLETED".equals(targetStatus)
                && Set.of("DELIVERING", "STORED").contains(parcel.getCurrentStatus())) {
            parcelLifecycleService.changeStatus(parcel, "READY_PICKUP", userId,
                    "COURIER", "配送任务完成");
        } else if (Set.of("CANCELLED", "FAILED").contains(targetStatus)
                && Set.of("WAITING_ASSIGNMENT", "DELIVERING").contains(parcel.getCurrentStatus())) {
            parcelLifecycleService.changeStatus(parcel, "EXCEPTION", userId,
                    "COURIER", "配送任务终止：" + targetStatus);
        }
        return toVo(order);
    }

    private Courier requireAvailableCourier(Long userId) {
        Courier courier = requireCourier(userId);
        if (!"ACTIVE".equals(courier.getEmploymentStatus())
                || !Set.of("AVAILABLE", "BUSY").contains(courier.getAvailabilityStatus())) {
            throw new BusinessException(ErrorCodes.DELIVERY_CONFLICT, "配送员当前不可接单");
        }
        return courier;
    }

    private Courier requireCourier(Long userId) {
        Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getUserId, userId));
        if (courier == null) {
            throw new BusinessException(ErrorCodes.COURIER_NOT_FOUND, "配送员档案不存在");
        }
        return courier;
    }

    private DeliveryOrder requireLockedOrder(Long orderId) {
        DeliveryOrder order = orderMapper.selectByIdForUpdate(orderId);
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

    private String generateOrderNo() {
        return "DO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private DeliveryOrderVO toVo(DeliveryOrder order) {
        return new DeliveryOrderVO(order.getId(), order.getOrderNo(), order.getParcelId(),
                order.getDestinationLocationId(), order.getAssignedCourierId(), order.getPriority(),
                order.getStatus(), order.getPredictedMinutes(), order.getAcceptedTime(),
                order.getCompletedTime(), order.getCreateTime(), order.getUpdateTime());
    }
}
