package com.camigo.exception;

import org.springframework.http.HttpStatus;

/**
 * 用户已认证但没有操作权限。
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(40300, message, HttpStatus.FORBIDDEN);
    }
}
