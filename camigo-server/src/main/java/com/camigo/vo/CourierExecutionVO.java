package com.camigo.vo;

/** 取件或到达操作后的一致性快照。 */
public record CourierExecutionVO(
        Long orderId,
        String orderNo,
        String orderStatus,
        String parcelStatus,
        Long currentLocationId,
        String currentLocationName,
        CourierRouteVO route) {
}
