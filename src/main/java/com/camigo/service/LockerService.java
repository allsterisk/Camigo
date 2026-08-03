package com.camigo.service;

import com.camigo.dto.ParcelPickupDTO;
import com.camigo.dto.ParcelStorageDTO;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.PickupRecordVO;
import com.camigo.vo.StorageRecordVO;

/**
 * 智能柜入柜、取件和状态查询服务。
 */
public interface LockerService {

    StorageRecordVO storeParcel(Long userId, ParcelStorageDTO request);

    PickupRecordVO pickupParcel(Long userId, ParcelPickupDTO request);

    LockerStatusVO getLockerStatus(Long lockerId);
}
