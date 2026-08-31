package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 综合路线中的业务停靠点。 */
@Getter
@Setter
@TableName("route_stops")
public class RouteStop extends BaseEntity {

    private Long deliveryRouteId;
    private Long deliveryOrderId;
    private Long locationId;
    private Integer sequenceNo;
    private String stopType;
    private String stopStatus;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualArrivalTime;
}
