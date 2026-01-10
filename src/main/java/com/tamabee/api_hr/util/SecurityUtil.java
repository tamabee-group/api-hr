package com.tamabee.api_hr.util;

import com.tamabee.api_hr.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Utility class để lấy thông tin từ JWT token trong SecurityContext.
 * Dùng cho các admin services cần lấy companyId từ JWT thay vì từ UserEntity.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;

    /**
     * Lấy companyId từ JWT token của user hiện tại.
     * Dùng cho admin services chạy trên master DB.
     */
    public Long getCurrentUserCompanyId() {
        Map<String, Object> claims = getCurrentUserClaims();
        Object companyIdObj = claims.get("companyId");
        if (companyIdObj == null) {
            throw UnauthorizedException.notAuthenticated();
        }
        return ((Number) companyIdObj).longValue();
    }

    /**
     * Lấy email từ JWT token của user hiện tại.
     */
    public String getCurrentUserEmail() {
        Map<String, Object> claims = getCurrentUserClaims();
        return (String) claims.get("email");
    }

    /**
     * Lấy role từ JWT token của user hiện tại.
     */
    public String getCurrentUserRole() {
        Map<String, Object> claims = getCurrentUserClaims();
        return (String) claims.get("role");
    }

    /**
     * Lấy tenantDomain từ JWT token của user hiện tại.
     */
    public String getCurrentUserTenantDomain() {
        Map<String, Object> claims = getCurrentUserClaims();
        return (String) claims.get("tenantDomain");
    }

    /**
     * Lấy userId từ JWT token của user hiện tại.
     */
    public Long getCurrentUserId() {
        Map<String, Object> claims = getCurrentUserClaims();
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw UnauthorizedException.notAuthenticated();
        }
        return ((Number) userIdObj).longValue();
    }

    /**
     * Lấy tất cả claims từ JWT token của user hiện tại.
     */
    private Map<String, Object> getCurrentUserClaims() {
        String token = getTokenFromRequest();
        if (token == null) {
            throw UnauthorizedException.notAuthenticated();
        }

        Map<String, Object> claims = jwtUtil.validateToken(token);
        if (claims == null) {
            throw UnauthorizedException.notAuthenticated();
        }

        return claims;
    }

    /**
     * Lấy JWT token từ request hiện tại.
     */
    private String getTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();

        // Ưu tiên Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback to cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
