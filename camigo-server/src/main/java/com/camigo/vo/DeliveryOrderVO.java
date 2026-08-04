package com.camigo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 配送订单响应。
 */
@Schema(description = "配送任务详情")
public record DeliveryOrderVO(
        @Schema(description = "配送任务编号") Long id,
        @Schema(description = "配送业务单号") String orderNo,
        @Schema(description = "快递编号") Long parcelId,
        @Schema(description = "配送目的地编号") Long destinationLocationId,
        @Schema(description = "配送员档案编号", nullable = true) Long assignedCourierId,
        @Schema(description = "优先级") String priority,
        @Schema(description = "公共配送状态：WAITING、PROCESSING或DELIVERED") String status,
        @Schema(description = "预计配送分钟数", nullable = true) Integer predictedMinutes,
        @Schema(description = "接单时间", nullable = true) LocalDateTime acceptedTime,
        @Schema(description = "送达时间", nullable = true) LocalDateTime completedTime,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "更新时间") LocalDateTime updateTime) {
}
