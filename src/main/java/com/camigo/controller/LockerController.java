package com.camigo.controller;

import com.camigo.common.Result;
import com.camigo.dto.ParcelPickupDTO;
import com.camigo.dto.ParcelStorageDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.LockerService;
import com.camigo.vo.LockerStatusVO;
import com.camigo.vo.PickupRecordVO;
import com.camigo.vo.StorageRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能快递柜接口。
 */
@RestController
@RequestMapping("/api/lockers")
public class LockerController {

    private final LockerService lockerService;

    public LockerController(LockerService lockerService) {
        this.lockerService = lockerService;
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
}
