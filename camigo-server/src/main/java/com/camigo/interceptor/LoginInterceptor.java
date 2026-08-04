package com.camigo.interceptor;

import com.camigo.common.Result;
import com.camigo.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.cors.CorsUtils;

/**
 * 基础 JWT 登录拦截器。Phase 4 后续可扩展 Redis 会话撤销和 RBAC 权限校验。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ID = "currentUserId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // CORS preflight requests carry no JWT and are validated by Spring's CORS handler.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }

        request.setAttribute(CURRENT_USER_ID, jwtUtil.getUserId(token));
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(HttpStatus.UNAUTHORIZED.value(), message));
    }
}
