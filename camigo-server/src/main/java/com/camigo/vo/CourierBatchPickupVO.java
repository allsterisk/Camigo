package com.camigo.vo;

import java.util.List;

/** 同站批量取件后的一致性快照。 */
public record CourierBatchPickupVO(
        Long pickupLocationId,
        String pickupLocationName,
        int pickedCount,
        List<Long> orderIds,
        List<String> orderNos,
        CourierRouteVO route) {
}
