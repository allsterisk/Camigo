package com.camigo.controller;

import com.camigo.common.Result;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.DeliveryOrderService;
import com.camigo.vo.DeliveryOrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配送任务接口。
 */
@RestController
@RequestMapping("/api/delivery/orders")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @PostMapping
    public Result<DeliveryOrderVO> create(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody DeliveryOrderCreateDTO request) {
        return Result.success(deliveryOrderService.create(userId, request));
    }

    @PutMapping("/{id}/accept")
    public Result<DeliveryOrderVO> accept(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable Long id) {
        return Result.success(deliveryOrderService.accept(userId, id));
    }

    @PutMapping("/status")
    public Result<DeliveryOrderVO> updateStatus(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody DeliveryOrderStatusDTO request) {
        return Result.success(deliveryOrderService.updateStatus(userId, request));
    }
}
