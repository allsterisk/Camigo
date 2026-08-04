package com.camigo.exception;

import org.springframework.http.HttpStatus;

/**
 * 请求的业务资源不存在。
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(40400, message, HttpStatus.NOT_FOUND);
    }
}
