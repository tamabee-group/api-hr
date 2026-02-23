package com.tamabee.api_hr.dto.common;

import java.time.LocalDateTime;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        return new BaseResponse<>(200, true, message, data, LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())), null);
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, true, "Success", data, LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())), null);
    }

    public static <T> BaseResponse<T> created(T data, String message) {
        return new BaseResponse<>(201, true, message, data, LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())), null);
    }

    public static <T> BaseResponse<T> error(String message, String errorCode) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(400, false, message, null, now, errorCode);
    }

    public static <T> BaseResponse<T> error(int status, String message, String errorCode) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(status, false, message, null, now, errorCode);
    }

    public static <T> BaseResponse<T> unauthorized(String message) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(401, false, message, null, now, "UNAUTHORIZED");
    }

    public static <T> BaseResponse<T> forbidden(String message) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(403, false, message, null, now, "FORBIDDEN");
    }

    public static <T> BaseResponse<T> notFound(String message) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(404, false, message, null, now, "NOT_FOUND");
    }

    public static <T> BaseResponse<T> serverError(String message) {
        LocalDateTime now = LocalDateTime.now(
                RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return new BaseResponse<>(500, false, message, null, now, "INTERNAL_SERVER_ERROR");
    }
}
