package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dto.CourierLocationUpdateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.CourierExecutionService;
import com.camigo.vo.CampusLocationOptionVO;
import com.camigo.vo.CourierLocationStateVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 与业务文档一致的配送员本人位置资源。 */
@RestController
@RequireRoles("COURIER")
@RequestMapping("/api/couriers")
public class CourierLocationController {

    private final CourierExecutionService executionService;

    public CourierLocationController(CourierExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping("/locations")
    public Result<List<CampusLocationOptionVO>> locations() {
        return Result.success(executionService.activeLocations());
    }

    @GetMapping("/me/location")
    public Result<CourierLocationStateVO> locationState(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        return Result.success(executionService.locationState(userId));
    }

    @PutMapping("/me/location")
    public Result<CourierLocationStateVO> confirmLocation(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody CourierLocationUpdateDTO request) {
        return Result.success(executionService.confirmLocation(userId, request.locationId()));
    }

    @PutMapping("/me/offline")
    public Result<Void> setOffline(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        executionService.setOffline(userId);
        return Result.success();
    }
}
