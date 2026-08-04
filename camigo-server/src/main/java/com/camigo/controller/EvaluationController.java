package com.camigo.controller;

import com.camigo.common.Result;
import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.EvaluationService;
import com.camigo.vo.EvaluationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户评价接口。
 */
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
}
