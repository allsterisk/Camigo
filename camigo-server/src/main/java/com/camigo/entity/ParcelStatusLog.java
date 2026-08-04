package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 包裹状态审计日志。
 */
@Getter
@Setter
@TableName("parcel_status_logs")
public class ParcelStatusLog extends BaseEntity {

    private Long parcelId;
    private String fromStatus;
    private String toStatus;
    private Long operatorUserId;
    private String sourceType;
    private LocalDateTime occurredTime;
    private String remark;
}
