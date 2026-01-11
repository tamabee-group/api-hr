package com.tamabee.api_hr.util;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Tạo access token với đầy đủ thông tin multi-tenant.
     * 
     * @param userId       ID của user
     * @param email        Email của user
     * @param role         Role của user
     * @param companyId    ID của company (0 = Tamabee)
     * @param tenantDomain Domain của tenant ("tamabee" cho Tamabee users)
     * @param planId       ID của plan (null cho Tamabee users = all features)
     * @param employeeCode Mã nhân viên
     * @param name         Tên nhân viên
     * @param language     Ngôn ngữ của user (vi, en, ja)
     */
    public String generateAccessToken(Long userId, String email, String role,
            Long companyId, String tenantDomain, Long planId, String employeeCode, String name, String language) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .claim("companyId", companyId)
                .claim("tenantDomain", tenantDomain)
                .claim("planId", planId)
                .claim("employeeCode", employeeCode)
                .claim("name", name)
                .claim("language", language)
                .claim("type", "access")
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId, String email) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("type", "refresh")
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Map<String, Object> validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
