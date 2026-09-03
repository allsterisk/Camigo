package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.interceptor.RoleAuthorizationInterceptor;
import com.camigo.service.EvaluationService;
import com.camigo.vo.EvaluationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户评价接口。
 */
@Validated
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public Result<EvaluationVO> create(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody EvaluationCreateDTO request) {
        return Result.success(evaluationService.create(userId, request));
    }

    /**
     * 按快递查询其关联配送订单的全部评价（最新一条在前）。用户限本人快递，管理员不限。
     */
    @RequireRoles({"USER", "ADMIN"})
    @GetMapping("/parcel/{parcelId}")
    public Result<List<EvaluationVO>> listByParcel(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ROLES) List<String> roles,
            @PathVariable @Positive(message = "快递编号必须为正数") Long parcelId) {
        return Result.success(evaluationService.listByParcel(userId, roles, parcelId));
    }

    /**
     * 按配送订单查询单条评价，未评价时 data 为 null。配送员限本人订单，管理员不限。
     */
    @RequireRoles({"COURIER", "ADMIN"})
    @GetMapping("/order/{orderId}")
    public Result<EvaluationVO> getByOrder(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ROLES) List<String> roles,
            @PathVariable @Positive(message = "配送订单编号必须为正数") Long orderId) {
        return Result.success(evaluationService.getByOrder(userId, roles, orderId));
    }
}
