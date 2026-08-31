package com.camigo.vo;

import java.util.List;

/** 配送员当前综合路线的文字展示模型。 */
public record CourierRouteVO(
        Long courierId,
        Long currentLocationId,
        String currentLocationName,
        Integer versionNo,
        Integer totalDistanceMeters,
        Integer estimatedDurationSeconds,
        String algorithmVersion,
        List<CourierRouteStepVO> steps) {
}
