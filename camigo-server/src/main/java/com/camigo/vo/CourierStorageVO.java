package com.camigo.vo;

/** 入柜完成结果；刻意不向配送员返回取件码。 */
public record CourierStorageVO(
        Long storageRecordId,
        Long orderId,
        String orderStatus,
        String parcelStatus,
        Long lockerId,
        Long lockerSlotId,
        String slotNo,
        Long currentLocationId,
        CourierRouteVO route) {
}
