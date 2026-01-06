package com.tamabee.api_hr.filter;

import com.tamabee.api_hr.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Filter đọc tenantDomain từ JWT và lưu vào TenantContext.
 * Chạy trước tất cả controllers.
 * Tamabee users có tenantDomain = "tamabee".
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            String tenantDomain = extractTenantFromJwt(request);
            if (tenantDomain != null) {
                TenantContext.setCurrentTenant(tenantDomain);
                log.debug("Set tenant context: {}", tenantDomain);
            }
            chain.doFilter(request, response);
        } finally {
            // Luôn clear để tránh memory leak
            TenantContext.clear();
        }
    }

    /**
     * Đọc JWT từ cookie và extract tenantDomain claim.
     * Tamabee users: tenantDomain = "tamabee"
     * Company users: tenantDomain = company.tenantDomain
     */
    private String extractTenantFromJwt(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) {
            return null;
        }

        Map<String, Object> claims = jwtUtil.validateToken(token);
        if (claims == null) {
            return null;
        }

        Object tenantDomain = claims.get("tenantDomain");
        return tenantDomain != null ? tenantDomain.toString() : null;
    }

    private String getTokenFromCookie(HttpServletRequest request) {
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
