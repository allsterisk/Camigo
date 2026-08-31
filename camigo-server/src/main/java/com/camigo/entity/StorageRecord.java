package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 包裹入柜与格口占用周期。
 */
@Getter
@Setter
@TableName("storage_records")
public class StorageRecord extends BaseEntity {

    private Long parcelId;
    private Long lockerSlotId;
    private Long storedByUserId;
    private String status;
    private LocalDateTime storedTime;
    private LocalDateTime releasedTime;
    private String pickupCode;
    private String pickupCodeHash;
    private LocalDateTime codeExpireTime;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long activeSlotId;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long activeParcelId;
}
