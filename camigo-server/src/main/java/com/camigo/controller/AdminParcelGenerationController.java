package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.dto.AdminParcelGenerateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.AdminParcelGenerationService;
import com.camigo.vo.AdminGeneratedParcelVO;
import com.camigo.vo.AdminParcelGenerationOptionsVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequireRoles("ADMIN")
@RequestMapping("/api/admin/parcel-generation")
public class AdminParcelGenerationController {
    private final AdminParcelGenerationService generationService;

    public AdminParcelGenerationController(AdminParcelGenerationService generationService) {
        this.generationService = generationService;
    }

    @GetMapping("/options")
    public Result<AdminParcelGenerationOptionsVO> options(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        return Result.success(generationService.options(userId));
    }

    @PostMapping
    public Result<List<AdminGeneratedParcelVO>> generate(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody AdminParcelGenerateDTO request) {
        return Result.success(generationService.generate(userId, request));
    }
}
