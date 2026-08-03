package com.camigo.exception;

import org.springframework.http.HttpStatus;

/**
 * 未登录、登录过期或令牌无效。
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(40100, message, HttpStatus.UNAUTHORIZED);
    }
}
