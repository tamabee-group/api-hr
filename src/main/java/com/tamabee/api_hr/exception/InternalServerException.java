package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 500 Internal Server Error
 * Sử dụng khi có lỗi hệ thống không xác định
 */
public class InternalServerException extends BaseException {

    public InternalServerException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }

    public InternalServerException(String message, String errorCode) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
    }

    public InternalServerException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
    }

    public InternalServerException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, errorCode, cause);
    }

    public InternalServerException(ErrorCode errorCode) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
    }

    public static InternalServerException fileUploadFailed(Throwable cause) {
        return new InternalServerException("Lỗi khi tải file lên", ErrorCode.FILE_UPLOAD_FAILED, cause);
    }

    public static InternalServerException emailSendFailed(Throwable cause) {
        return new InternalServerException("Không thể gửi email", ErrorCode.EMAIL_SEND_FAILED, cause);
    }
}
