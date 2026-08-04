package com.camigo.common;

/**
 * 统一 API 响应结构。
 *
 * @param code    与 HTTP 语义一致的状态码，成功为 200
 * @param message 面向调用方的稳定提示
 * @param data    响应数据
 * @param <T>     数据类型
 */
public record Result<T>(int code, String message, T data) {

    private static final int SUCCESS_CODE = 200;

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
