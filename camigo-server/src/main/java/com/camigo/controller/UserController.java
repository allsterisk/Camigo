package com.camigo.controller;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.PageResult;
import com.camigo.common.Result;
import com.camigo.dto.UserLoginDTO;
import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.UserService;
import com.camigo.vo.LoginVO;
import com.camigo.vo.AdminUserVO;
import com.camigo.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户注册、登录和当前用户接口。
 */
@Tag(name = "User API", description = "用户注册、登录与资料管理")
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "注册普通用户", description = "公开注册仅允许提交 USER 角色")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数不合法"),
            @ApiResponse(responseCode = "403", description = "尝试注册非 USER 角色"),
            @ApiResponse(responseCode = "409", description = "用户名、手机号或学号已存在")
    })
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Valid @RequestBody UserRegisterDTO request) {
        return Result.success(userService.register(request));
    }

    @Operation(summary = "用户登录", description = "账号支持用户名、手机号或学号")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功并返回 JWT"),
            @ApiResponse(responseCode = "400", description = "参数不合法"),
            @ApiResponse(responseCode = "401", description = "账号或密码错误"),
            @ApiResponse(responseCode = "403", description = "账号不可用")
    })
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "获取当前用户信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 已失效"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/info")
    public Result<UserInfoVO> info(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "修改用户信息",
            description = "普通用户只能修改本人资料；管理员可修改其他用户及账号状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "参数不合法"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 已失效"),
            @ApiResponse(responseCode = "403", description = "没有修改权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "409", description = "手机号或学号已存在")
    })
    @PutMapping("/{id}")
    public Result<UserInfoVO> update(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long currentUserId,
            @Parameter(description = "目标用户编号", required = true)
            @PathVariable @Positive(message = "用户编号必须为正数") Long id,
            @Valid @RequestBody UserUpdateDTO request) {
        return Result.success(userService.updateUser(currentUserId, id, request));
    }

    @Operation(summary = "管理员分页查询用户")
    @RequireRoles("ADMIN")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long currentUserId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        return Result.success(userService.listUsers(currentUserId, page, pageSize, keyword, status, role));
    }

    @Operation(summary = "管理员删除无业务数据的用户")
    @RequireRoles("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestAttribute(LoginInterceptor.CURRENT_USER_ID) Long currentUserId,
            @PathVariable @Positive(message = "用户编号必须为正数") Long id) {
        userService.deleteUser(currentUserId, id);
        return Result.success();
    }
}
