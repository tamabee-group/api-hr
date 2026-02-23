package com.tamabee.api_hr.config.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tamabee.api_hr.exception.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * AOP Aspect xử lý rate limiting cho các method có @RateLimited annotation.
 * Sử dụng Bucket4j (token bucket algorithm) — in-memory, không cần Redis.
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private static final int MAX_CACHE_SIZE = 10_000;

    /**
     * Cache buckets theo key (IP hoặc user+endpoint).
     * ConcurrentHashMap đảm bảo thread-safe.
     */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = resolveKey(joinPoint, rateLimited);
        Bucket bucket = resolveBucket(key, rateLimited);

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }

        // Rate limit exceeded
        String methodName = joinPoint.getSignature().getName();
        log.warn("Rate limit exceeded: key={}, method={}, limit={}/{} giây",
                key, methodName, rateLimited.requests(), rateLimited.durationSeconds());

        throw new RateLimitExceededException(
                "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau " + rateLimited.durationSeconds() + " giây."
        );
    }

    /**
     * Tạo key duy nhất cho mỗi rate limit bucket.
     * Format: "ratelimit:{endpoint}:{IP hoặc user}"
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
        String endpointName = rateLimited.name().isEmpty()
                ? joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName()
                : rateLimited.name();

        String identifier;
        if (rateLimited.keyType() == RateLimited.KeyType.USER) {
            identifier = getCurrentUser();
        } else {
            identifier = getClientIp();
        }

        return "ratelimit:" + endpointName + ":" + identifier;
    }

    /**
     * Tạo hoặc lấy bucket từ cache.
     */
    private Bucket resolveBucket(String key, RateLimited rateLimited) {
        return bucketCache.computeIfAbsent(key, k -> createBucket(rateLimited));
    }

    /**
     * Tạo bucket mới với giới hạn từ annotation.
     * Sử dụng Bandwidth.builder (API mới).
     */
    private Bucket createBucket(RateLimited rateLimited) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimited.requests())
                .refillGreedy(rateLimited.requests(), Duration.ofSeconds(rateLimited.durationSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Lấy IP từ request, hỗ trợ qua Nginx proxy (X-Forwarded-For).
     */
    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }

        HttpServletRequest request = attrs.getRequest();

        // Nginx set X-Real-IP header
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }

        // Fallback to X-Forwarded-For
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Lấy username từ JWT authentication context.
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        // Fallback to IP nếu chưa authenticated
        return getClientIp();
    }

    /**
     * Dọn dẹp cache định kỳ mỗi 10 phút để tránh memory leak.
     * Khi bị tấn công từ nhiều IP, cache có thể phình to.
     */
    @Scheduled(fixedRate = 600_000) // 10 phút
    public void cleanupCache() {
        int size = bucketCache.size();
        if (size > MAX_CACHE_SIZE) {
            log.warn("Rate limit cache quá lớn ({}), xóa toàn bộ", size);
            bucketCache.clear();
        }
    }
}
