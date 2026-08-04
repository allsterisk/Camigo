package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 实际取件事实。
 */
@Getter
@Setter
@TableName("pickup_records")
public class PickupRecord extends BaseEntity {

    private Long storageRecordId;
    private Long userId;
    private Long appointmentId;
    private String verificationType;
    private String result;
    private LocalDateTime pickupTime;
    private String failureReason;
}
