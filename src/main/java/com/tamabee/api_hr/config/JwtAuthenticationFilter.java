package com.tamabee.api_hr.config;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.UserRepository;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TAMABEE_TENANT = "tamabee";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromCookie(request);

        if (token != null) {
            Map<String, Object> claims = jwtUtil.validateToken(token);

            if (claims != null) {
                String email = (String) claims.get("sub");
                String role = (String) claims.get("role");
                String tenantDomain = (String) claims.get("tenantDomain");

                // Validate tenantDomain trong JWT match với user's company
                if (!validateTenantDomain(email, tenantDomain)) {
                    log.warn("Tenant domain mismatch for user: {}", email);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Tạo authority từ role (Spring Security yêu cầu prefix ROLE_)
                var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                        null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validate tenantDomain trong JWT match với user's company.
     * - Tamabee users (companyId = 0): tenantDomain phải là "tamabee"
     * - Company users: tenantDomain phải match với company.tenantDomain
     */
    private boolean validateTenantDomain(String email, String tenantDomain) {
        if (tenantDomain == null || tenantDomain.isEmpty()) {
            log.debug("No tenantDomain in JWT for user: {}", email);
            return false;
        }

        Optional<UserEntity> userOpt = userRepository.findByEmailAndDeletedFalse(email);
        if (userOpt.isEmpty()) {
            log.debug("User not found: {}", email);
            return false;
        }

        UserEntity user = userOpt.get();
        Long companyId = user.getCompanyId();

        // Tamabee users (companyId = 0 hoặc null)
        if (companyId == null || companyId == 0) {
            boolean valid = TAMABEE_TENANT.equals(tenantDomain);
            if (!valid) {
                log.warn("Tamabee user {} has invalid tenantDomain: {}", email, tenantDomain);
            }
            return valid;
        }

        // Company users: validate với company.tenantDomain
        Optional<CompanyEntity> companyOpt = companyRepository.findByIdAndDeletedFalse(companyId);
        if (companyOpt.isEmpty()) {
            log.warn("Company not found for user: {}, companyId: {}", email, companyId);
            return false;
        }

        String expectedTenantDomain = companyOpt.get().getTenantDomain();
        boolean valid = tenantDomain.equals(expectedTenantDomain);
        if (!valid) {
            log.warn("Tenant domain mismatch for user {}: expected={}, actual={}",
                    email, expectedTenantDomain, tenantDomain);
        }
        return valid;
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
