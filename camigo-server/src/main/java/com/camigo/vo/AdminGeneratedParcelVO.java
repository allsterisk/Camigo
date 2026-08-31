package com.camigo.vo;

public record AdminGeneratedParcelVO(
        Long parcelId, String trackingNo, String carrierCode,
        Long deliveryOrderId, String orderNo,
        String parcelStatus, String orderStatus,
        Integer dispatchRound, Long targetLockerId) {
}
