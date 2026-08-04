package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.service.AdminManagementService;
import com.camigo.vo.AdminCourierVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 配送员管理接口。 */
@Validated
@RestController
@RequestMapping("/api/couriers")
public class CourierController {
    private final AdminManagementService adminManagementService;

    public CourierController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @RequireRoles("ADMIN")
    @GetMapping
    public Result<PageResult<AdminCourierVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String employmentStatus) {
        return Result.success(adminManagementService.listCouriers(page, pageSize, keyword, employmentStatus));
    }
}
