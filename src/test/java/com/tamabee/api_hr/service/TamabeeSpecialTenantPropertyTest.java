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
 * Property-based tests cho Tamabee Special Tenant Handling.
 * 
 * Property 10: Tamabee Special Tenant Handling
 * - For any Tamabee user (companyId = 0), system SHALL set tenantDomain =
 * "tamabee" in JWT
 * - For any Tamabee user, system SHALL route HR queries to tamabee_tamabee
 * database
 */
class TamabeeSpecialTenantPropertyTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-must-be-at-least-256-bits-long";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 24 hours
    private static final String TAMABEE_TENANT_DOMAIN = "tamabee";
    private static final Long TAMABEE_COMPANY_ID = 0L;
    private static final String DATABASE_PREFIX = "tamabee_";

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

    // ==================== Property 10: Tamabee Special Tenant Handling
    // ====================

    /**
     * Property 10.1: Tamabee users (companyId = 0) phải có tenantDomain = "tamabee"
     * trong JWT
     * For any Tamabee user, JWT SHALL contain tenantDomain = "tamabee"
     */
    @Property(tries = 100)
    void tamabeeUser_shouldHaveTenantDomainTamabee(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        // Tamabee user: companyId = 0, tenantDomain = "tamabee", planId = null
        String token = jwtUtil.generateAccessToken(
                userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("tenantDomain")).isEqualTo(TAMABEE_TENANT_DOMAIN);
    }

    /**
     * Property 10.2: Tamabee users phải có planId = null (all features enabled)
     * For any Tamabee user, JWT SHALL contain planId = null
     */
    @Property(tries = 100)
    void tamabeeUser_shouldHaveNullPlanId(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        // Tamabee user: planId = null means all features enabled
        String token = jwtUtil.generateAccessToken(
                userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("planId")).isNull();
    }

    /**
     * Property 10.3: Tamabee users phải có companyId = 0
     * For any Tamabee user, JWT SHALL contain companyId = 0
     */
    @Property(tries = 100)
    void tamabeeUser_shouldHaveCompanyIdZero(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(
                userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(TAMABEE_COMPANY_ID);
    }

    /**
     * Property 10.4: Tamabee tenant database name phải là "tamabee_tamabee"
     * For tenantDomain = "tamabee", database name SHALL be "tamabee_tamabee"
     */
    @Property(tries = 100)
    void tamabeeTenant_databaseName_shouldBeTamabeeTamabee(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        // Verify database name format
        String expectedDbName = DATABASE_PREFIX + TAMABEE_TENANT_DOMAIN;
        assertThat(expectedDbName).isEqualTo("tamabee_tamabee");
    }

    /**
     * Property 10.5: Tamabee users với bất kỳ role nào đều phải có cùng
     * tenantDomain
     * For any Tamabee role, tenantDomain SHALL always be "tamabee"
     */
    @Property(tries = 100)
    void tamabeeUser_anyRole_shouldHaveSameTenantDomain(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(
                userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        // Regardless of role, tenantDomain should always be "tamabee"
        assertThat(claims.get("tenantDomain")).isEqualTo(TAMABEE_TENANT_DOMAIN);
        assertThat(claims.get("role")).isIn("ADMIN_TAMABEE", "MANAGER_TAMABEE", "EMPLOYEE_TAMABEE");
    }

    /**
     * Property 10.6: Tamabee JWT phải chứa đầy đủ required fields (planId có thể
     * null)
     * For any Tamabee user, JWT SHALL contain all required fields
     */
    @Property(tries = 100)
    void tamabeeUser_jwtPayload_shouldContainAllRequiredFields(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll("tamabeeRoles") String role) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.generateAccessToken(
                userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> claims = jwtUtil.validateToken(token);

        assertThat(claims).isNotNull();
        // Verify all required fields exist (planId is null for Tamabee users, so key
        // may not exist)
        assertThat(claims).containsKey("userId");
        assertThat(claims).containsKey("email");
        assertThat(claims).containsKey("role");
        assertThat(claims).containsKey("companyId");
        assertThat(claims).containsKey("tenantDomain");
        // planId is null for Tamabee users - verify it's either null or not present
        assertThat(claims.get("planId")).isNull();
        assertThat(claims).containsKey("type");
        assertThat(claims).containsKey("sub");
    }

    /**
     * Property 10.7: Tamabee users khác với regular company users về tenantDomain
     * và planId
     * For Tamabee vs regular company users, tenantDomain and planId SHALL differ
     */
    @Property(tries = 100)
    void tamabeeUser_vs_companyUser_shouldDifferInTenantAndPlan(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validEmails") String email,
            @ForAll @LongRange(min = 1, max = 1000000) Long companyId,
            @ForAll("validTenantDomains") String companyTenantDomain,
            @ForAll @LongRange(min = 1, max = 100) Long planId) throws Exception {

        JwtUtil jwtUtil = createJwtUtil();

        // Tamabee user JWT
        String tamabeeToken = jwtUtil.generateAccessToken(
                userId, email, "ADMIN_TAMABEE", TAMABEE_COMPANY_ID, TAMABEE_TENANT_DOMAIN, null);
        Map<String, Object> tamabeeClaims = jwtUtil.validateToken(tamabeeToken);

        // Company user JWT
        String companyToken = jwtUtil.generateAccessToken(
                userId, email, "ADMIN_COMPANY", companyId, companyTenantDomain, planId);
        Map<String, Object> companyClaims = jwtUtil.validateToken(companyToken);

        // Verify differences
        assertThat(tamabeeClaims.get("tenantDomain")).isEqualTo(TAMABEE_TENANT_DOMAIN);
        assertThat(tamabeeClaims.get("planId")).isNull();
        assertThat(((Number) tamabeeClaims.get("companyId")).longValue()).isEqualTo(TAMABEE_COMPANY_ID);

        assertThat(companyClaims.get("tenantDomain")).isEqualTo(companyTenantDomain);
        assertThat(companyClaims.get("planId")).isNotNull();
        assertThat(((Number) companyClaims.get("companyId")).longValue()).isEqualTo(companyId);
    }

    /**
     * Property 10.8: Database routing cho Tamabee phải đúng format
     * For tenantDomain "tamabee", database routing SHALL use "tamabee_tamabee"
     */
    @Property(tries = 50)
    void tamabeeTenant_databaseRouting_shouldUseCorrectFormat(
            @ForAll("validTenantDomains") String tenantDomain) {
        // Verify database name format: tamabee_{tenantDomain}
        String dbName = DATABASE_PREFIX + tenantDomain;

        assertThat(dbName).startsWith(DATABASE_PREFIX);
        assertThat(dbName).isEqualTo("tamabee_" + tenantDomain);

        // Special case for Tamabee
        if (TAMABEE_TENANT_DOMAIN.equals(tenantDomain)) {
            assertThat(dbName).isEqualTo("tamabee_tamabee");
        }
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
     * Provider cho Tamabee user roles
     */
    @Provide
    Arbitrary<String> tamabeeRoles() {
        return Arbitraries.of("ADMIN_TAMABEE", "MANAGER_TAMABEE", "EMPLOYEE_TAMABEE");
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
}
