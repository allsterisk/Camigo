package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.DeliveryOrderService;
import com.camigo.vo.DeliveryOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配送任务创建、查询、接单与状态流转接口。
 */
@Tag(name = "Delivery API", description = "配送任务创建、配送员任务查询、接单与状态更新")
@Validated
@RestController
@RequestMapping("/api/delivery-orders")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @Operation(summary = "创建配送任务", description = "普通用户只能为本人的待配送快递创建任务")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数或快递状态不正确"),
            @ApiResponse(responseCode = "403", description = "需要普通用户权限"),
            @ApiResponse(responseCode = "404", description = "快递或配送目的地不存在"),
            @ApiResponse(responseCode = "409", description = "快递已有活动配送任务")
    })
    @RequireRoles("USER")
    @PostMapping
    public Result<DeliveryOrderVO> create(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody DeliveryOrderCreateDTO request) {
        return Result.success(deliveryOrderService.create(userId, request));
    }

    @Operation(summary = "配送员分页查看任务", description = "返回可接任务以及当前配送员自己的任务")
    @RequireRoles("COURIER")
    @GetMapping("/tasks")
    public Result<PageResult<DeliveryOrderVO>> listTasks(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(deliveryOrderService.listTasks(userId, page, pageSize, status));
    }

    @Operation(summary = "配送员接单", description = "使用数据库行锁保证同一任务只能被一名配送员接取")
    @RequireRoles("COURIER")
    @PutMapping("/{id}/accept")
    public Result<DeliveryOrderVO> accept(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Parameter(description = "配送任务编号", required = true)
            @PathVariable @Positive(message = "配送任务编号必须为正数") Long id) {
        return Result.success(deliveryOrderService.accept(userId, id));
    }

    @Operation(summary = "更新配送状态", description = "配送员只能更新本人已接取的任务")
    @RequireRoles("COURIER")
    @PutMapping("/{id}/status")
    public Result<DeliveryOrderVO> updateStatus(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Parameter(description = "配送任务编号", required = true)
            @PathVariable @Positive(message = "配送任务编号必须为正数") Long id,
            @Valid @RequestBody DeliveryOrderStatusDTO request) {
        return Result.success(deliveryOrderService.updateStatus(userId, id, request));
    }
}
