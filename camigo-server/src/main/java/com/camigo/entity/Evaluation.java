package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 配送服务评价实体。
 */
@Getter
@Setter
@TableName("evaluations")
public class Evaluation extends BaseEntity {

    private Long deliveryOrderId;
    private Long userId;
    private Long courierId;
    private Integer deliveryScore;
    private Integer serviceScore;
    private String content;
    private String status;
    private LocalDateTime evaluatedTime;
}
