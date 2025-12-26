package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 401 Unauthorized
 * Sử dụng khi chưa đăng nhập hoặc token không hợp lệ
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }

    public UnauthorizedException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(HttpStatus.UNAUTHORIZED, errorCode);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.INVALID_TOKEN);
    }

    public static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    public static UnauthorizedException notAuthenticated() {
        return new UnauthorizedException(ErrorCode.NOT_AUTHENTICATED);
    }
}
