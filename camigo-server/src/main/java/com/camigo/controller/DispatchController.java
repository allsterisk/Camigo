package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dispatch.IntelligentDispatchService;
import com.camigo.dto.DispatchRejectDTO;
import com.camigo.entity.DeliveryOrder;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.vo.DispatchAcceptanceVO;
import com.camigo.vo.DispatchAssignmentDetailVO;
import com.camigo.vo.DispatchOfferVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 智能派单邀请响应和管理员审计接口。 */
@Tag(name = "Dispatch API", description = "智能派单、限时确认、拒绝和评分审计")
@Validated
@RestController
@RequestMapping("/api/delivery-assignments")
public class DispatchController {

    private final IntelligentDispatchService dispatchService;

    public DispatchController(IntelligentDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "查询配送员全部有效邀请")
    @RequireRoles("COURIER")
    @GetMapping("/current")
    public Result<List<DispatchOfferVO>> current(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        return Result.success(dispatchService.currentOffers(userId));
    }

    @Operation(summary = "接受派单邀请", description = "数据库过期时间为最终依据")
    @RequireRoles("COURIER")
    @PutMapping("/{id}/accept")
    public Result<DispatchAcceptanceVO> accept(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive Long id) {
        DeliveryOrder order = dispatchService.acceptAssignment(userId, id);
        return Result.success(new DispatchAcceptanceVO(order.getId(), order.getOrderNo(),
                order.getAssignedCourierId(), order.getStatus(), order.getAcceptedTime()));
    }

    @Operation(summary = "拒绝派单邀请")
    @RequireRoles("COURIER")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody DispatchRejectDTO request) {
        dispatchService.rejectAssignment(userId, id, request);
        return Result.success();
    }

    @Operation(summary = "管理员启动订单智能派单")
    @RequireRoles("ADMIN")
    @PostMapping("/orders/{orderId}/start")
    public Result<Void> start(@PathVariable @Positive Long orderId) {
        dispatchService.startDispatch(orderId);
        return Result.success();
    }

    @Operation(summary = "管理员按快递启动首轮或下一轮智能派单")
    @RequireRoles("ADMIN")
    @PostMapping("/parcels/{parcelId}/start")
    public Result<Void> startByParcel(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable @Positive Long parcelId) {
        dispatchService.startDispatchByParcel(userId, parcelId);
        return Result.success();
    }

    @Operation(summary = "管理员查看订单完整派单依据")
    @RequireRoles("ADMIN")
    @GetMapping("/orders/{orderId}")
    public Result<List<DispatchAssignmentDetailVO>> details(
            @PathVariable @Positive Long orderId) {
        return Result.success(dispatchService.adminDetails(orderId));
    }

    @Operation(summary = "管理员按包裹查看最新配送单的完整派单依据")
    @RequireRoles("ADMIN")
    @GetMapping("/parcels/{parcelId}")
    public Result<List<DispatchAssignmentDetailVO>> detailsByParcel(
            @PathVariable @Positive Long parcelId) {
        return Result.success(dispatchService.adminDetailsByParcel(parcelId));
    }
}
