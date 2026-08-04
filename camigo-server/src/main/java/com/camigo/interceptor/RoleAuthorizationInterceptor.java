package com.camigo.interceptor;

import com.camigo.annotation.RequireRoles;
import com.camigo.common.Result;
import com.camigo.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于数据库实时角色的接口授权拦截器。
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ROLES = "currentUserRoles";

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public RoleAuthorizationInterceptor(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRoles requirement = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (requirement == null) {
            requirement = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        if (requirement == null || requirement.value().length == 0) {
            return true;
        }

        Long userId = (Long) request.getAttribute(LoginInterceptor.CURRENT_USER_ID);
        if (userId == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return false;
        }

        List<String> roles = userMapper.selectRoleCodes(userId);
        boolean permitted = roles != null && Arrays.stream(requirement.value()).anyMatch(roles::contains);
        if (!permitted) {
            writeError(response, HttpStatus.FORBIDDEN, "没有接口访问权限");
            return false;
        }
        request.setAttribute(CURRENT_USER_ROLES, List.copyOf(roles));
        return true;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(status.value(), message));
    }
}
