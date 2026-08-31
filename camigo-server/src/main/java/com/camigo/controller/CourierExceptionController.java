package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dto.ParcelExceptionReportDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.ParcelExceptionService;
import com.camigo.vo.ParcelExceptionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequireRoles("COURIER")
@RequestMapping("/api/courier-execution")
public class CourierExceptionController {
    private final ParcelExceptionService exceptionService;

    public CourierExceptionController(ParcelExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @PostMapping("/orders/{orderId}/exceptions")
    public Result<ParcelExceptionVO> report(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId,
            @Valid @RequestBody ParcelExceptionReportDTO request) {
        return Result.success(exceptionService.report(userId, orderId, request));
    }
}
