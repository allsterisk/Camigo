package com.camigo.vo;

import java.time.LocalDateTime;

/** 配送员接受派单后的订单摘要。 */
public record DispatchAcceptanceVO(Long deliveryOrderId, String orderNo, Long courierId,
                                   String status, LocalDateTime acceptedTime) {
}
