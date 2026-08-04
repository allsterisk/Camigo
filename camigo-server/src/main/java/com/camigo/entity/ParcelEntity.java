package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 快递主数据实体，对应 parcels 表。
 */
@Getter
@Setter
@TableName("parcels")
public class ParcelEntity extends BaseEntity {

    private String trackingNo;
    private String carrierCode;
    private Long recipientUserId;
    private String senderName;
    private String senderPhoneMasked;
    private Long destinationLocationId;
    private Long targetLockerId;
    private String sizeClass;
    private BigDecimal weightKg;
    private String currentStatus;
    private LocalDateTime arriveTime;
}
