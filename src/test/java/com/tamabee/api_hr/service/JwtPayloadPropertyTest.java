package com.tamabee.api_hr.service;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import com.tamabee.api_hr.util.JwtUtil;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.LongRange;

/**
 * Property-based tests cho JWT payload.
 * 
 * Property 6: JWT Payload Completeness
 * - JWT SHALL contain all required fields: userId, email, role, tenantDomain,
 * planId, companyId
 * 
 * Property 7: JWT Tenant Validation
 * - System SHALL reject token if tenantDomain does not match user's company
 */
class JwtPayloadPropertyTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-must-be-at-least-256-bits-long";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 24 hours

    /**
     * Tạo JwtUtil instance với test configuration
     */
    private JwtUtil createJwtUtil() throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", TEST_SECRET);
        setField(jwtUtil, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        setField(jwtUtil, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        return jwtUtil;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ==================== Property 6: JWT Payload Completeness
    // ====================

    /**
     * Property 6.1: JWT access token phải chứa userId claim
     * For any valid user data, generated JWT SHALL contain userId claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainUserId(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("userId")).isNotNull();
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(userId);
    }

    /**
     * Property 6.2: JWT access token phải chứa email claim
     * For any valid user data, generated JWT SHALL contain email claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainEmail(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("email")).isEqualTo(email);
        assertThat(claims.get("sub")).isEqualTo(email); // subject cũng là email
    }

    /**
     * Property 6.3: JWT access token phải chứa role claim
     * For any valid user data, generated JWT SHALL contain role claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainRole(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("role")).isEqualTo(role);
    }

    /**
     * Property 6.4: JWT access token phải chứa companyId claim
     * For any valid user data, generated JWT SHALL contain companyId claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainCompanyId(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("companyId")).isNotNull();
        assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(companyId);
    }

    /**
     * Property 6.5: JWT access token phải chứa tenantDomain claim
     * For any valid user data, generated JWT SHALL contain tenantDomain claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainTenantDomain(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("tenantDomain")).isEqualTo(tenantDomain);
    }

    /**
     * Property 6.6: JWT access token phải chứa planId claim
     * For any valid user data, generated JWT SHALL contain planId claim
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainPlanId(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 0, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("planId")).isNotNull();
        assertThat(((Number) claims.get("planId")).longValue()).isEqualTo(planId);
    }

    /**
     * Property 6.7: JWT access token cho Tamabee users phải có tenantDomain =
     * "tamabee" và planId = null
     * For any Tamabee user (companyId = 0), JWT SHALL have tenantDomain = "tamabee"
     * and planId = null
     */
    @Property(tries = 100)
    void jwtPayload_tamabeeUser_shouldHaveCorrectTenantAndNullPlan(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();
        Long companyId = 0L; // Tamabee company
        String tenantDomain = "tamabee";
        Long planId = null; // Tamabee users have all features

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("tenantDomain")).isEqualTo("tamabee");
        assertThat(claims.get("planId")).isNull();
        assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(0L);
    }

    /**
     * Property 6.8: JWT payload phải chứa đầy đủ tất cả required fields
     * For any valid user data, JWT SHALL contain ALL required fields
     */
    @Property(tries = 100)
    void jwtPayload_shouldContainAllRequiredFields(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        // Verify all required fields exist
        assertThat(claims).containsKey("userId");
        assertThat(claims).containsKey("email");
        assertThat(claims).containsKey("role");
        assertThat(claims).containsKey("companyId");
        assertThat(claims).containsKey("tenantDomain");
        assertThat(claims).containsKey("planId");
        assertThat(claims).containsKey("type");
        assertThat(claims).containsKey("sub");
        assertThat(claims).containsKey("iat");
        assertThat(claims).containsKey("exp");
    }

    // ==================== Property 7: JWT Tenant Validation ====================

    /**
     * Property 7.1: Token với invalid signature phải bị reject
     * For any tampered token, validateToken SHALL return null
     */
    @Property(tries = 50)
    void jwtValidation_tamperedToken_shouldBeRejected(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        // Tamper with token by modifying a character
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        Map<String, Object> claims = jwtUtil.validateToken(tamperedToken);

        assertThat(claims).isNull();
    }

    /**
     * Property 7.2: Token với different secret phải bị reject
     * For any token signed with different secret, validateToken SHALL return null
     */
    @Property(tries = 50)
    void jwtValidation_differentSecret_shouldBeRejected(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        // Create token with one secret
        JwtUtil jwtUtil1 = createJwtUtil();
        String token = jwtUtil1.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);

        // Try to validate with different secret
        JwtUtil jwtUtil2 = new JwtUtil();
        setField(jwtUtil2, "secret", "different-secret-key-for-jwt-testing-must-be-at-least-256-bits");
        setField(jwtUtil2, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        setField(jwtUtil2, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        Map<String, Object> claims = jwtUtil2.validateToken(token);

        assertThat(claims).isNull();
    }

    /**
     * Property 7.3: Valid token phải được validate thành công và trả về đúng claims
     * For any valid token, validateToken SHALL return correct claims
     */
    @Property(tries = 100)
    void jwtValidation_validToken_shouldReturnCorrectClaims(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(userId);
        assertThat(claims.get("email")).isEqualTo(email);
        assertThat(claims.get("role")).isEqualTo(role);
        assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(companyId);
        assertThat(claims.get("tenantDomain")).isEqualTo(tenantDomain);
        assertThat(((Number) claims.get("planId")).longValue()).isEqualTo(planId);
    }

    /**
     * Property 7.4: TenantDomain trong JWT phải được preserve chính xác
     * For any tenantDomain, JWT round-trip SHALL preserve the exact value
     */
    @Property(tries = 100)
    void jwtValidation_tenantDomain_shouldBePreservedExactly(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("validRoles") String role,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String tenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        String extractedTenantDomain = (String) claims.get("tenantDomain");
        assertThat(extractedTenantDomain).isEqualTo(tenantDomain);
    }

    // ==================== Providers ====================

    /**
     * Provider cho valid emails
     */
    @Provide
    Arbitrary<String> validEmails() {
        Arbitrary<String> localPart = Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                        '4', '5', '6', '7', '8', '9')
                .ofMinLength(3)
                .ofMaxLength(10);

        Arbitrary<String> domain = Arbitraries.of("example.com", "test.org", "company.vn", "tamabee.vn");

        return Combinators.combine(localPart, domain)
                .as((local, dom) -> local + "@" + dom);
    }

    /**
     * Provider cho valid tenant domains (lowercase, numbers, hyphens, 3-30 chars)
     */
    @Provide
    Arbitrary<String> validTenantDomains() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                        '4', '5', '6', '7', '8', '9', '-')
                .ofMinLength(3)
                .ofMaxLength(30)
                .filter(s -> !s.startsWith("-") && !s.endsWith("-") && !s.isEmpty());
    }

    /**
     * Provider cho valid user roles
     */
    @Provide
    Arbitrary<String> validRoles() {
        return Arbitraries.of(
                "ADMIN_TAMABEE", "MANAGER_TAMABEE", "EMPLOYEE_TAMABEE",
                "ADMIN_COMPANY", "MANAGER_COMPANY", "EMPLOYEE_COMPANY");
    }

    /**
     * Provider cho Tamabee user roles
     */
    @Provide
    Arbitrary<String> tamabeeRoles() {
        return Arbitraries.of("ADMIN_TAMABEE", "MANAGER_TAMABEE", "EMPLOYEE_TAMABEE");
    }
}
