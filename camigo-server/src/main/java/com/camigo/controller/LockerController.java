package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.dto.ParcelPickupDTO;
import com.camigo.dto.ParcelStorageDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.LockerService;
import com.camigo.service.AdminManagementService;
import com.camigo.vo.AdminLockerVO;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.PickupRecordVO;
import com.camigo.vo.StorageRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 智能快递柜接口。
 */
@RestController
@Validated
@RequestMapping("/api/lockers")
public class LockerController {

    private final LockerService lockerService;
    private final AdminManagementService adminManagementService;

    public LockerController(LockerService lockerService, AdminManagementService adminManagementService) {
        this.lockerService = lockerService;
        this.adminManagementService = adminManagementService;
    }

    @PostMapping("/storage")
    public Result<StorageRecordVO> store(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody ParcelStorageDTO request) {
        return Result.success(lockerService.storeParcel(userId, request));
    }

    @PostMapping("/pickup")
    public Result<PickupRecordVO> pickup(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody ParcelPickupDTO request) {
        return Result.success(lockerService.pickupParcel(userId, request));
    }

    @GetMapping("/{id}/status")
    public Result<LockerStatusVO> status(@PathVariable Long id) {
        return Result.success(lockerService.getLockerStatus(id));
    }

    @RequireRoles("ADMIN")
    @GetMapping
    public Result<PageResult<AdminLockerVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationStatus) {
        return Result.success(adminManagementService.listLockers(page, pageSize, keyword, operationStatus));
    }
}
