package com.tamabee.api_hr.config;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String token = getToken(request);
        System.out.println(">>> JwtAuthenticationFilter: path=" + path + ", token present=" + (token != null));
        log.info("Processing request: {}, token present: {}", path, token != null);

        if (token != null) {
            Map<String, Object> claims = jwtUtil.validateToken(token);

            if (claims != null) {
                String email = (String) claims.get("sub");
                String role = (String) claims.get("role");
                String tenantDomain = (String) claims.get("tenantDomain");

                log.info("JWT claims - email: {}, role: {}, tenantDomain: {}", email, role, tenantDomain);

                // Set TenantContext từ JWT cho tất cả authenticated requests (trừ master
                // endpoints)
                boolean isMaster = isMasterEndpoint(request);
                log.info("Is master endpoint: {}", isMaster);

                if (!isMaster && tenantDomain != null && !tenantDomain.isEmpty()) {
                    TenantContext.setCurrentTenant(tenantDomain);
                    log.info("Set tenant context from JWT: {} for user: {}", tenantDomain, email);
                }

                // Tạo authority từ role (Spring Security yêu cầu prefix ROLE_)
                var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                        null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("JWT validation failed for token");
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra có phải endpoint query master DB.
     * Các endpoint này không cần tenant context.
     * Lưu ý: /api/auth/me cần tenant context vì query users table
     */
    private boolean isMasterEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();

        // /api/auth/me cần tenant context
        if (path.equals("/api/auth/me")) {
            return false;
        }

        return path.startsWith("/api/plans/") ||
                path.startsWith("/api/admin/") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/");
    }

    /**
     * Lấy token từ Authorization header hoặc cookie
     */
    private String getToken(HttpServletRequest request) {
        // Ưu tiên Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback to cookie
        return getTokenFromCookie(request);
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
