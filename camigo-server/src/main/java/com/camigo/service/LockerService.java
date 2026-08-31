package com.camigo.service;

import com.camigo.dto.ParcelPickupDTO;
import com.camigo.vo.CourierStorageVO;
import com.camigo.vo.LockerSlotOptionVO;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.ParcelPickupAccessVO;
import com.camigo.vo.PickupRecordVO;
import java.util.List;

/**
 * 智能柜入柜、取件和状态查询服务。
 */
public interface LockerService {

    List<LockerSlotOptionVO> listCompatibleSlots(Long userId, Long orderId);

    CourierStorageVO storeParcel(Long userId, Long orderId, Long lockerSlotId);

    ParcelPickupAccessVO getPickupAccess(Long userId, Long parcelId);

    PickupRecordVO pickupParcel(Long userId, ParcelPickupDTO request);

    LockerStatusVO getLockerStatus(Long lockerId);
}
