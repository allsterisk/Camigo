package com.camigo.routing;

import java.util.Objects;

/** 尚未完成的一次取件或送达任务。 */
public record RouteStopTask(long deliveryOrderId, long locationId, RouteStopType stopType) {

    public RouteStopTask {
        if (deliveryOrderId <= 0 || locationId <= 0) {
            throw new IllegalArgumentException("路线任务的订单和地点编号必须有效");
        }
        Objects.requireNonNull(stopType, "停靠类型不能为空");
    }
}
