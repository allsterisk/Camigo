package com.camigo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快递接口响应。
 */
@Schema(description = "快递详情")
public record ParcelVO(
        @Schema(description = "快递编号") Long id,
        @Schema(description = "快递单号") String trackingNo,
        @Schema(description = "承运商编码") String carrierCode,
        @Schema(description = "收件用户编号") Long recipientUserId,
        @Schema(description = "收件人姓名", nullable = true) String recipientName,
        @Schema(description = "寄件人姓名") String senderName,
        @Schema(description = "脱敏寄件人联系方式", nullable = true) String senderPhoneMasked,
        @Schema(description = "目标校园位置编号") Long destinationLocationId,
        @Schema(description = "目标快递柜编号") Long targetLockerId,
        @Schema(description = "目标智能柜名称", nullable = true) String targetLockerName,
        @Schema(description = "包裹尺寸") String sizeClass,
        @Schema(description = "包裹重量（千克）", nullable = true) BigDecimal weightKg,
        @Schema(description = "公共快递状态") String currentStatus,
        @Schema(description = "管理员当前是否可以开始首轮或下一轮配送") boolean deliveryStartAvailable,
        @Schema(description = "到校时间") LocalDateTime arriveTime,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "更新时间") LocalDateTime updateTime) {
}
