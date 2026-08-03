package com.camigo.exception;

import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常，携带稳定业务码和 HTTP 状态。
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(int code, String message) {
        this(code, message, HttpStatus.CONFLICT);
    }

    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
