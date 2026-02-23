package com.tamabee.api_hr.datasource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter đọc tenantDomain từ JWT hoặc host.
 * - Authenticated requests: lấy tenant từ JWT
 * - Login/register: lấy tenant từ host
 * - Master-only APIs: không set tenant (dùng master DB)
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    /**
     * Các path prefix cần query từ master DB (không set tenant).
     * Bao gồm: plans, settings, register/check-domain, companies, deposits, commissions, schedulers
     */
    private static final Set<String> MASTER_ONLY_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/check-domain",
            "/api/auth/send-verification",
            "/api/auth/verify-email",
            "/api/plans",
            "/api/admin/settings",
            "/api/admin/plans",
            "/api/admin/companies",
            "/api/admin/deposits",
            "/api/admin/commissions",
            "/api/admin/commission-settings",
            "/api/admin/schedulers",
            "/api/admin/feedbacks",
            "/api/admin/system-notifications",
            "/api/admin/pending-counts",
            "/api/employee/commissions",
            "/api/users/me/feedbacks");

    /**
     * Các path cần lấy tenant từ host (không từ JWT).
     * Dùng cho các endpoint mà JWT có thể hết hạn hoặc không có.
     */
    private static final Set<String> HOST_TENANT_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/reset-password");

    /**
     * Các path prefix cần query từ tenant "tamabee" (Tamabee admin endpoints).
     * Dùng cho các endpoint cần query cả users (tenant) và companies (master).
     */
    private static final Set<String> TAMABEE_TENANT_PATHS = Set.of(
            "/api/admin/employees",
            "/api/employee/referrals");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Kiểm tra nếu là master-only path thì không set tenant
        if (isMasterOnlyPath(path)) {
            log.debug("TenantFilter: path={} is master-only, using master DB", path);
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
                RegionContext.clear();
            }
            return;
        }

        // Kiểm tra nếu là Tamabee tenant path thì set tenant = "tamabee"
        if (isTamabeeTenantPath(path)) {
            log.debug("TenantFilter: path={} is tamabee-tenant, using tamabee DB", path);
            TenantContext.setCurrentTenant("tamabee");
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
                RegionContext.clear();
            }
            return;
        }

        // Kiểm tra nếu là host-tenant path (login, refresh-token, etc.)
        // Luôn lấy tenant từ host, không từ JWT (vì JWT có thể hết hạn)
        if (isHostTenantPath(path)) {
            String tenantFromHost = extractTenantFromHost(request);
            log.info("TenantFilter: path={} is host-tenant, using tenant from host: {}", path, tenantFromHost);
            if (tenantFromHost != null && !tenantFromHost.isEmpty()) {
                TenantContext.setCurrentTenant(tenantFromHost);
            }
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
                RegionContext.clear();
            }
            return;
        }

        String tenantDomain = null;

        // Thử lấy tenant từ JWT trước
        tenantDomain = extractTenantFromJwt(request);

        // Nếu không có JWT, lấy từ host (cho login/register)
        if (tenantDomain == null) {
            tenantDomain = extractTenantFromHost(request);
        }

        log.debug("TenantFilter: path={}, tenant={}", path, tenantDomain);

        if (tenantDomain != null && !tenantDomain.isEmpty()) {
            TenantContext.setCurrentTenant(tenantDomain);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            RegionContext.clear();
        }
    }

    /**
     * Kiểm tra path có phải master-only không
     */
    private boolean isMasterOnlyPath(String path) {
        return MASTER_ONLY_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Kiểm tra path có phải Tamabee tenant path không
     */
    private boolean isTamabeeTenantPath(String path) {
        return TAMABEE_TENANT_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Kiểm tra path có phải host-tenant path không (lấy tenant từ host, không từ JWT)
     */
    private boolean isHostTenantPath(String path) {
        return HOST_TENANT_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Extract tenantDomain từ JWT token (Authorization header hoặc cookie)
     */
    private String extractTenantFromJwt(HttpServletRequest request) {
        // Từ Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String tenant = extractTenantFromToken(authHeader.substring(7));
            if (tenant != null) {
                return tenant;
            }
        }

        // Từ cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String tenant = extractTenantFromToken(cookie.getValue());
                    if (tenant != null) {
                        return tenant;
                    }
                }
            }
        }

        // Từ X-Tenant-Domain header (cho login từ frontend)
        String tenantHeader = request.getHeader("X-Tenant-Domain");
        if (tenantHeader != null && !tenantHeader.isEmpty()) {
            log.info("TenantFilter: using X-Tenant-Domain header: {}", tenantHeader);
            return tenantHeader;
        }

        return null;
    }

    /**
     * Extract tenantDomain từ JWT token payload
     */
    private String extractTenantFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                // Simple JSON parsing for tenantDomain
                if (payload.contains("\"tenantDomain\"")) {
                    int start = payload.indexOf("\"tenantDomain\"") + 16;
                    int end = payload.indexOf("\"", start);
                    if (end > start) {
                        return payload.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract tenant from JWT: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract tenantDomain từ host.
     * Ưu tiên X-Forwarded-Host (từ proxy) trước, sau đó mới dùng ServerName
     */
    private String extractTenantFromHost(HttpServletRequest request) {
        // Ưu tiên X-Forwarded-Host từ proxy
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) {
            host = request.getServerName();
        }

        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return "tamabee";
        }

        String[] parts = host.split("\\.");
        if (parts.length >= 3) {
            return parts[0];
        } else if (parts.length == 2) {
            return parts[0];
        }

        return "tamabee";
    }
}
