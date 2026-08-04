package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 快递柜格口实体。
 */
@Getter
@Setter
@TableName("locker_slots")
public class LockerSlot extends BaseEntity {

    private Long lockerId;
    private String slotNo;
    private String sizeClass;
    private String status;
}
