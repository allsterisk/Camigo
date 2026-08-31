package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 校园地点间的带权道路。 */
@Getter
@Setter
@TableName("campus_route_edges")
public class CampusRouteEdge extends BaseEntity {

    private Long fromLocationId;
    private Long toLocationId;
    private Integer distanceMeters;
    private Integer bidirectional;
    private String status;
}
