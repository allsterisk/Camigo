package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.dto.ParcelExceptionResolutionDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.ParcelExceptionService;
import com.camigo.vo.ParcelExceptionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequireRoles("ADMIN")
@RequestMapping("/api/admin/exceptions")
public class AdminExceptionController {
    private final ParcelExceptionService exceptionService;

    public AdminExceptionController(ParcelExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @GetMapping
    public Result<PageResult<ParcelExceptionVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(exceptionService.listAdmin(page, pageSize, status, keyword));
    }

    @PostMapping("/{id}/resolution")
    public Result<ParcelExceptionVO> resolve(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ParcelExceptionResolutionDTO request) {
        return Result.success(exceptionService.resolve(userId, id, request));
    }
}
