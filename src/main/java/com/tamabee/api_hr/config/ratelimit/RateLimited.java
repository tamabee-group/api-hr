package com.tamabee.api_hr.config.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để giới hạn số lần gọi API.
 * Tương đương rate-limiter middleware ở frontend.
 *
 * Cách sử dụng:
 * <pre>
 * // Giới hạn 5 request / 60 giây theo IP
 * {@literal @}RateLimited(requests = 5, durationSeconds = 60)
 *
 * // Giới hạn 10 request / 60 giây theo User (JWT)
 * {@literal @}RateLimited(requests = 10, durationSeconds = 60, keyType = RateLimited.KeyType.USER)
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Số request tối đa trong khoảng thời gian
     */
    int requests() default 30;

    /**
     * Khoảng thời gian (giây)
     */
    int durationSeconds() default 60;

    /**
     * Loại key để phân biệt rate limit
     */
    KeyType keyType() default KeyType.IP;

    /**
     * Tên định danh (để phân biệt rate limit giữa các endpoint)
     */
    String name() default "";

    enum KeyType {
        /**
         * Rate limit theo IP address
         */
        IP,
        /**
         * Rate limit theo user (từ JWT authentication)
         */
        USER
    }
}
