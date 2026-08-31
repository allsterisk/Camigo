package com.camigo.routing;

import java.util.List;

/** 两个校园地点之间的最短距离和完整经过节点。 */
public record ShortestPath(int distanceMeters, List<Long> locationIds) {

    public ShortestPath {
        if (distanceMeters < 0 || locationIds == null || locationIds.isEmpty()) {
            throw new IllegalArgumentException("最短路径结果无效");
        }
        locationIds = List.copyOf(locationIds);
    }
}
