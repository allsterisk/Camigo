package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.dto.ParcelCreateDTO;
import com.camigo.dto.ParcelStatusUpdateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.ParcelService;
import com.camigo.service.LockerService;
import com.camigo.vo.ParcelPickupAccessVO;
import com.camigo.vo.ParcelVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 快递 CRUD 与状态接口。
 */
@Tag(name = "Parcel API", description = "快递录入、查询、状态流转与删除")
@Validated
@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final ParcelService parcelService;
    private final LockerService lockerService;

    public ParcelController(ParcelService parcelService, LockerService lockerService) {
        this.parcelService = parcelService;
        this.lockerService = lockerService;
    }

    @Operation(summary = "管理员录入快递")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "录入成功"),
            @ApiResponse(responseCode = "400", description = "参数或收件信息不正确"),
            @ApiResponse(responseCode = "403", description = "需要管理员权限"),
            @ApiResponse(responseCode = "404", description = "收件用户或目标快递柜不存在"),
            @ApiResponse(responseCode = "409", description = "快递单号已存在")
    })
    @RequireRoles("ADMIN")
    @PostMapping
    public Result<ParcelVO> create(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody ParcelCreateDTO request) {
        return Result.success(parcelService.create(userId, request));
    }

    @Operation(summary = "管理员分页查询快递")
    @RequireRoles("ADMIN")
    @GetMapping
    public Result<PageResult<ParcelVO>> listAll(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trackingNo) {
        return Result.success(parcelService.listAll(userId, page, pageSize, status, trackingNo));
    }

    @Operation(summary = "查询我的快递", description = "只返回当前登录用户的快递")
    @RequireRoles("USER")
    @GetMapping("/my")
    public Result<PageResult<ParcelVO>> listMy(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trackingNo) {
        return Result.success(parcelService.listMy(userId, page, pageSize, status, trackingNo));
    }

    @Operation(summary = "查询快递详情", description = "用户限本人快递，配送员限本人任务，管理员可查询全部")
    @RequireRoles({"USER", "COURIER", "ADMIN"})
    @GetMapping("/{id}")
    public Result<ParcelVO> detail(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Parameter(description = "快递编号", required = true)
            @PathVariable @Positive(message = "快递编号必须为正数") Long id) {
        return Result.success(parcelService.getById(userId, id));
    }

    @Operation(summary = "查询本人快递的活动取件码")
    @RequireRoles("USER")
    @GetMapping("/{id}/pickup-access")
    public Result<ParcelPickupAccessVO> pickupAccess(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive(message = "快递编号必须为正数") Long id) {
        return Result.success(lockerService.getPickupAccess(userId, id));
    }

    @Operation(summary = "更新快递状态",
            description = "配送员可更新本人任务；用户只能确认本人已取件快递完成；入柜和取件必须使用快递柜接口")
    @RequireRoles({"USER", "COURIER", "ADMIN"})
    @PutMapping("/{id}/status")
    public Result<ParcelVO> updateStatus(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Parameter(description = "快递编号", required = true)
            @PathVariable @Positive(message = "快递编号必须为正数") Long id,
            @Valid @RequestBody ParcelStatusUpdateDTO request) {
        return Result.success(parcelService.updateStatus(userId, id, request));
    }

    @Operation(summary = "删除已登记快递", description = "仅管理员可删除尚未进入配送流程的快递")
    @RequireRoles("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Parameter(description = "快递编号", required = true)
            @PathVariable @Positive(message = "快递编号必须为正数") Long id) {
        parcelService.delete(userId, id);
        return Result.success();
    }
}
