package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.ParcelStatusLog;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.ParcelStatusLogMapper;
import com.camigo.service.ParcelLifecycleService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 使用条件更新保证包裹状态不会被并发请求静默覆盖。
 */
@Service
public class ParcelLifecycleServiceImpl implements ParcelLifecycleService {

    private static final Set<String> ALL_STATUSES = Set.of(
            "REGISTERED", "SORTING", "WAITING_ASSIGNMENT", "DELIVERING",
            "STORED", "READY_PICKUP", "PICKED_UP", "COMPLETED", "EXCEPTION", "RETURNED");

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "REGISTERED", Set.of("SORTING", "WAITING_ASSIGNMENT", "EXCEPTION"),
            "SORTING", Set.of("WAITING_ASSIGNMENT", "EXCEPTION"),
            "WAITING_ASSIGNMENT", Set.of("DELIVERING", "EXCEPTION", "RETURNED"),
            "DELIVERING", Set.of("STORED", "READY_PICKUP", "EXCEPTION"),
            "STORED", Set.of("READY_PICKUP", "PICKED_UP", "EXCEPTION"),
            "READY_PICKUP", Set.of("PICKED_UP", "EXCEPTION"),
            "PICKED_UP", Set.of("COMPLETED"),
            "COMPLETED", Set.of(),
            "EXCEPTION", Set.of("SORTING", "DELIVERING", "RETURNED"),
            "RETURNED", Set.of());

    private final ParcelMapper parcelMapper;
    private final ParcelStatusLogMapper statusLogMapper;

    public ParcelLifecycleServiceImpl(ParcelMapper parcelMapper, ParcelStatusLogMapper statusLogMapper) {
        this.parcelMapper = parcelMapper;
        this.statusLogMapper = statusLogMapper;
    }

    @Override
    public void recordInitialStatus(ParcelEntity parcel, Long operatorUserId, String sourceType) {
        insertLog(parcel.getId(), null, parcel.getCurrentStatus(), operatorUserId, sourceType, "快递登记");
    }

    @Override
    public void changeStatus(ParcelEntity parcel, String targetStatus, Long operatorUserId,
                             String sourceType, String remark) {
        String normalizedStatus = targetStatus == null ? "" : targetStatus.trim().toUpperCase();
        if (!ALL_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ErrorCodes.PARCEL_STATUS_INVALID,
                    "不支持的快递状态", HttpStatus.BAD_REQUEST);
        }
        String currentStatus = parcel.getCurrentStatus();
        if (normalizedStatus.equals(currentStatus)) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(normalizedStatus)) {
            throw new BusinessException(ErrorCodes.PARCEL_STATUS_INVALID,
                    "快递状态不能从 " + currentStatus + " 变更为 " + normalizedStatus,
                    HttpStatus.BAD_REQUEST);
        }

        int updated = parcelMapper.update(null, new LambdaUpdateWrapper<ParcelEntity>()
                .eq(ParcelEntity::getId, parcel.getId())
                .eq(ParcelEntity::getCurrentStatus, currentStatus)
                .set(ParcelEntity::getCurrentStatus, normalizedStatus));
        if (updated != 1) {
            throw new BusinessException(ErrorCodes.PARCEL_CONFLICT, "快递状态已被其他请求修改");
        }
        insertLog(parcel.getId(), currentStatus, normalizedStatus, operatorUserId, sourceType, remark);
        parcel.setCurrentStatus(normalizedStatus);
    }

    private void insertLog(Long parcelId, String fromStatus, String toStatus, Long operatorUserId,
                           String sourceType, String remark) {
        ParcelStatusLog log = new ParcelStatusLog();
        log.setParcelId(parcelId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorUserId(operatorUserId);
        log.setSourceType(sourceType);
        log.setOccurredTime(LocalDateTime.now());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }
}
