package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.dto.common.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler - Xử lý tất cả exception trong hệ thống
 * Trả về response thống nhất với HTTP status code đúng
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý các custom exception kế thừa từ BaseException
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException ex) {
        log.error("BaseException: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(BaseResponse.error(ex.getStatus().value(), ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Xử lý lỗi validation từ @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        // Lấy message đầu tiên để hiển thị
        String firstError = errors.values().stream().findFirst().orElse("Dữ liệu không hợp lệ");

        BaseResponse<Map<String, String>> response = new BaseResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                false,
                firstError,
                errors,
                java.time.LocalDateTime.now(),
                "VALIDATION_ERROR");

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi xác thực (đăng nhập sai)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.unauthorized("Email hoặc mật khẩu không đúng"));
    }

    /**
     * Xử lý lỗi authentication chung
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.unauthorized("Chưa xác thực"));
    }

    /**
     * Xử lý lỗi không có quyền truy cập
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.forbidden("Không có quyền truy cập"));
    }

    /**
     * Xử lý lỗi file upload quá lớn
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        log.error("File too large: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error("File tải lên quá lớn", "FILE_TOO_LARGE"));
    }

    /**
     * Xử lý IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    /**
     * Xử lý tất cả các exception không được handle riêng
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.serverError("Có lỗi xảy ra, vui lòng thử lại sau"));
    }
}
