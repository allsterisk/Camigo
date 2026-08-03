package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 智能快递柜实体。
 */
@Getter
@Setter
@TableName("lockers")
public class Locker extends BaseEntity {

    private String lockerCode;
    private String lockerName;
    private Long locationId;
    private Integer totalSlots;
    private String operationStatus;
    private LocalDateTime lastHeartbeatTime;
}
