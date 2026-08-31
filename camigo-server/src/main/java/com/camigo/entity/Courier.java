package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 配送员扩展档案。
 */
@Getter
@Setter
@TableName("couriers")
public class Courier extends BaseEntity {

    private Long userId;
    private String courierNo;
    private String employmentStatus;
    private String availabilityStatus;
    private Integer maxActiveOrders;
    private BigDecimal ratingAvg;
    private Long currentLocationId;
    private LocalDateTime locationUpdateTime;
}
