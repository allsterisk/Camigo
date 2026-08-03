package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 校园配送订单实体。
 */
@Getter
@Setter
@TableName("delivery_orders")
public class DeliveryOrder extends BaseEntity {

    private String orderNo;
    private Long parcelId;
    private Long destinationLocationId;
    private Long assignedCourierId;
    private String priority;
    private String status;
    private Integer predictedMinutes;
    private LocalDateTime acceptedTime;
    private LocalDateTime completedTime;

    /** 数据库生成列，只读。 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long activeParcelId;
}
