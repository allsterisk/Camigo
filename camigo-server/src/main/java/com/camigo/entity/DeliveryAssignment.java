package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 一轮智能派单中的候选评分、邀请与响应审计记录。 */
@Getter
@Setter
@TableName("delivery_assignments")
public class DeliveryAssignment extends BaseEntity {

    private Long deliveryOrderId;
    private Long courierId;
    private Integer assignmentRound;
    private String algorithmVersion;
    private Integer routeIncrementMeters;
    private Integer activeOrderCount;
    private BigDecimal distanceScore;
    private BigDecimal workloadScore;
    private BigDecimal serviceScore;
    private BigDecimal totalScore;
    private String decision;
    private String decisionReason;
    private LocalDateTime offerTime;
    private LocalDateTime offerExpireTime;
    private LocalDateTime responseTime;
    private String rejectReasonCode;
    private String rejectReasonDetail;
    private LocalDateTime decidedTime;
}
