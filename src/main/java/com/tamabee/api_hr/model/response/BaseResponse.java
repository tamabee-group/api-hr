package com.tamabee.api_hr.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private int status;
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String errorCode;

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(200, true, message, data, LocalDateTime.now(), null);
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, true, "Success", data, LocalDateTime.now(), null);
    }

    public static <T> BaseResponse<T> created(T data, String message) {
        return new BaseResponse<>(201, true, message, data, LocalDateTime.now(), null);
    }

    public static <T> BaseResponse<T> error(String message, String errorCode) {
        return new BaseResponse<>(400, false, message, null, LocalDateTime.now(), errorCode);
    }

    public static <T> BaseResponse<T> error(int status, String message, String errorCode) {
        return new BaseResponse<>(status, false, message, null, LocalDateTime.now(), errorCode);
    }

    public static <T> BaseResponse<T> unauthorized(String message) {
        return new BaseResponse<>(401, false, message, null, LocalDateTime.now(), "UNAUTHORIZED");
    }

    public static <T> BaseResponse<T> forbidden(String message) {
        return new BaseResponse<>(403, false, message, null, LocalDateTime.now(), "FORBIDDEN");
    }

    public static <T> BaseResponse<T> notFound(String message) {
        return new BaseResponse<>(404, false, message, null, LocalDateTime.now(), "NOT_FOUND");
    }

    public static <T> BaseResponse<T> serverError(String message) {
        return new BaseResponse<>(500, false, message, null, LocalDateTime.now(), "INTERNAL_SERVER_ERROR");
    }
}
