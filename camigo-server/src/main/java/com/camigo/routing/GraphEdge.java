package com.camigo.routing;

/** 图算法使用的不可变道路边。 */
public record GraphEdge(long fromLocationId, long toLocationId,
                        int distanceMeters, boolean bidirectional) {

    public GraphEdge {
        if (fromLocationId <= 0 || toLocationId <= 0 || fromLocationId == toLocationId) {
            throw new IllegalArgumentException("道路端点无效");
        }
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("道路距离必须大于0");
        }
    }
}
