package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dto.CourierStorageDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.CourierExecutionService;
import com.camigo.service.LockerService;
import com.camigo.vo.CourierExecutionVO;
import com.camigo.vo.CourierStorageVO;
import com.camigo.vo.CourierRouteVO;
import com.camigo.vo.LockerSlotOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 配送员位置确认、路线查询与配送节点执行。 */
@Tag(name = "Courier Execution API", description = "配送执行、位置更新与剩余路线")
@Validated
@RestController
@RequireRoles("COURIER")
@RequestMapping("/api/courier-execution")
public class CourierExecutionController {

    private final CourierExecutionService executionService;
    private final LockerService lockerService;

    public CourierExecutionController(CourierExecutionService executionService,
                                      LockerService lockerService) {
        this.executionService = executionService;
        this.lockerService = lockerService;
    }

    @Operation(summary = "查询当前综合配送路线")
    @GetMapping("/route")
    public Result<CourierRouteVO> route(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        return Result.success(executionService.currentRoute(userId));
    }

    @Operation(summary = "在服务站确认取件")
    @PostMapping("/orders/{orderId}/pickup")
    public Result<CourierExecutionVO> confirmPickup(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId) {
        return Result.success(executionService.confirmPickup(userId, orderId));
    }

    @Operation(summary = "在配送目的地确认到达")
    @PostMapping("/orders/{orderId}/arrival")
    public Result<CourierExecutionVO> confirmArrival(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId) {
        return Result.success(executionService.confirmArrival(userId, orderId));
    }

    @Operation(summary = "查询订单可用的尺寸兼容格口")
    @GetMapping("/orders/{orderId}/locker-slots")
    public Result<List<LockerSlotOptionVO>> compatibleSlots(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId) {
        return Result.success(lockerService.listCompatibleSlots(userId, orderId));
    }

    @Operation(summary = "选择格口并完成入柜")
    @PostMapping("/orders/{orderId}/storage")
    public Result<CourierStorageVO> store(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId,
            @Valid @RequestBody CourierStorageDTO request) {
        return Result.success(lockerService.storeParcel(userId, orderId, request.lockerSlotId()));
    }
}
