package com.camigo.routing;

/** 路线规划只依赖该接口，便于纯算法测试。 */
@FunctionalInterface
public interface ShortestPathProvider {

    ShortestPath shortestPath(long fromLocationId, long toLocationId);
}
