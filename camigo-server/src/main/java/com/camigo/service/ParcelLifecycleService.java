package com.camigo.service;

import com.camigo.entity.ParcelEntity;

/**
 * 包裹状态流转与日志写入的内部服务。
 */
public interface ParcelLifecycleService {

    void recordInitialStatus(ParcelEntity parcel, Long operatorUserId, String sourceType);

    void changeStatus(ParcelEntity parcel, String targetStatus, Long operatorUserId,
                      String sourceType, String remark);
}
