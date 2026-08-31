package com.camigo.routing;

import java.time.LocalDateTime;
import java.util.Objects;

/** 带预计到达时间的路线任务。 */
public record ScheduledRouteStop(RouteStopTask task, LocalDateTime estimatedArrivalTime) {

    public ScheduledRouteStop {
        Objects.requireNonNull(task, "路线任务不能为空");
        Objects.requireNonNull(estimatedArrivalTime, "预计到达时间不能为空");
    }
}
