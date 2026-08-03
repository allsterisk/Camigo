package com.camigo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * 校园标准地点实体。
 */
@Getter
@Setter
@TableName("campus_locations")
public class CampusLocation extends BaseEntity {

    private String locationCode;
    private String locationName;
    private String locationType;
    private Long parentId;
    private String addressDetail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String status;
}
