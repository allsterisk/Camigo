package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 配送员综合路线版本。旧的一单一路线字段仅在数据库中保留用于兼容。 */
@Getter
@Setter
@TableName("delivery_routes")
public class DeliveryRoute extends BaseEntity {

    private Long courierId;
    private Integer versionNo;
    private String status;
    private Integer totalDistanceMeters;
    private Integer estimatedDurationSeconds;
    private String algorithmVersion;
}
