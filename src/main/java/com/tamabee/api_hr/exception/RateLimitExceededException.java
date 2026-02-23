package com.tamabee.api_hr.exception;

/**
 * Exception khi client vượt quá giới hạn rate limit.
 * Trả về HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
