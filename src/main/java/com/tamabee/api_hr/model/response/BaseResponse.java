package com.tamabee.api_hr.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String errorCode;
    
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(true, message, data, LocalDateTime.now(), null);
    }
    
    public static <T> BaseResponse<T> error(String message, String errorCode) {
        return new BaseResponse<>(false, message, null, LocalDateTime.now(), errorCode);
    }
}
