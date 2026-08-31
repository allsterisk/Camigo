package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 配送异常上报及管理员处理审计记录。 */
@Getter
@Setter
@TableName("parcel_exception_reports")
public class ParcelExceptionReport extends BaseEntity {
    private Long parcelId;
    private Long deliveryOrderId;
    private Long courierId;
    private String exceptionType;
    private String description;
    private Integer requiresAdmin;
    private String status;
    private LocalDateTime reportedTime;
    private Long handledByUserId;
    private LocalDateTime handledTime;
    private String resolutionType;
    private String resolutionNote;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long pendingParcelId;
}
