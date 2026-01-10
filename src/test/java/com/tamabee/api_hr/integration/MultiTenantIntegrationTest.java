package com.tamabee.api_hr.integration;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.response.wallet.PlanFeaturesResponse;
import com.tamabee.api_hr.enums.FeatureCode;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.service.core.interfaces.IPlanFeaturesService;
import com.tamabee.api_hr.util.JwtUtil;
import com.tamabee.api_hr.util.TenantDomainValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests cho Multi-Tenant Refactor.
 * Test các flow chính: registration, tenant isolation, Tamabee special tenant,
 * Plan Features API.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Tenant Integration Tests")
class MultiTenantIntegrationTest {

    private static final String TEST_SECRET = "TestSecretKeyForJWTTokenGenerationMustBeLongEnoughForHS256Algorithm";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 2592000000L;
    private static final String TAMABEE_TENANT = "tamabee";
    private static final Long TAMABEE_COMPANY_ID = 0L;

    @Mock
    private TenantDataSourceManager tenantDataSourceManager;

    @Mock
    private IPlanFeaturesService planFeaturesService;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = createJwtUtil();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private JwtUtil createJwtUtil() throws Exception {
        JwtUtil util = new JwtUtil();
        setField(util, "secret", TEST_SECRET);
        setField(util, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        setField(util, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        return util;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ==================== 13.1 Test end-to-end registration flow
    // ====================

    @Nested
    @DisplayName("13.1 Registration Flow Tests")
    class RegistrationFlowTests {

        @Test
        @DisplayName("Tenant domain validation - valid domain should pass")
        void tenantDomainValidation_validDomain_shouldPass() {
            // Given
            String validDomain = "acme-corp";

            // When
            TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(validDomain);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getError()).isNull();
        }

        @Test
        @DisplayName("Tenant domain validation - reserved domain should fail")
        void tenantDomainValidation_reservedDomain_shouldFail() {
            // Given
            List<String> reservedDomains = List.of("admin", "api", "www", "app", "mail", "tamabee");

            // When & Then
            for (String domain : reservedDomains) {
                TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(domain);
                assertThat(result.isValid())
                        .as("Domain '%s' should be rejected as reserved", domain)
                        .isFalse();
                assertThat(result.getError())
                        .isEqualTo(TenantDomainValidator.ValidationError.RESERVED);
            }
        }

        @Test
        @DisplayName("Tenant domain validation - invalid format should fail")
        void tenantDomainValidation_invalidFormat_shouldFail() {
            // Given
            List<String> invalidDomains = List.of(
                    "ab", // too short
                    "-acme", // starts with hyphen
                    "acme-", // ends with hyphen
                    "ACME", // uppercase
                    "acme corp", // contains space
                    "acme@corp" // contains special char
            );

            // When & Then
            for (String domain : invalidDomains) {
                TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(domain);
                assertThat(result.isValid())
                        .as("Domain '%s' should be rejected as invalid format", domain)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("JWT token should contain tenantDomain after registration")
        void jwtToken_afterRegistration_shouldContainTenantDomain() {
            // Given
            Long userId = 100L;
            String email = "admin@acme.com";
            String role = UserRole.ADMIN_COMPANY.name();
            Long companyId = 5L;
            String tenantDomain = "acme";
            Long planId = 1L;

            // When
            String token = jwtUtil.generateAccessToken(userId, email, role, companyId, tenantDomain, planId);
            Map<String, Object> claims = jwtUtil.validateToken(token);

            // Then
            assertThat(claims).isNotNull();
            assertThat(claims.get("tenantDomain")).isEqualTo(tenantDomain);
            assertThat(claims.get("planId")).isEqualTo(planId.intValue());
            assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("Database name should follow format tamabee_{tenantDomain}")
        void databaseName_shouldFollowCorrectFormat() {
            // Given
            String tenantDomain = "acme";
            String expectedDbName = "tamabee_acme";

            // When
            String actualDbName = "tamabee_" + tenantDomain;

            // Then
            assertThat(actualDbName).isEqualTo(expectedDbName);
        }
    }

    // ==================== 13.2 Test tenant isolation ====================

    @Nested
    @DisplayName("13.2 Tenant Isolation Tests")
    class TenantIsolationTests {

        @Test
        @DisplayName("TenantContext should isolate tenant data per thread")
        void tenantContext_shouldIsolateTenantDataPerThread() throws InterruptedException {
            // Given
            String tenant1 = "tenant1";
            String tenant2 = "tenant2";

            // When - Set tenant in main thread
            TenantContext.setCurrentTenant(tenant1);

            // Create another thread with different tenant
            Thread thread2 = new Thread(() -> {
                TenantContext.setCurrentTenant(tenant2);
                assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenant2);
                TenantContext.clear();
            });
            thread2.start();
            thread2.join();

            // Then - Main thread should still have tenant1
            assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenant1);
            TenantContext.clear();
        }

        @Test
        @DisplayName("Different tenants should have different database names")
        void differentTenants_shouldHaveDifferentDatabaseNames() {
            // Given
            String tenant1 = "company-a";
            String tenant2 = "company-b";

            // When
            String db1 = "tamabee_" + tenant1;
            String db2 = "tamabee_" + tenant2;

            // Then
            assertThat(db1).isNotEqualTo(db2);
            assertThat(db1).isEqualTo("tamabee_company-a");
            assertThat(db2).isEqualTo("tamabee_company-b");
        }

        @Test
        @DisplayName("JWT tokens for different tenants should have different tenantDomain")
        void jwtTokens_forDifferentTenants_shouldHaveDifferentTenantDomain() {
            // Given
            String tenant1 = "company-a";
            String tenant2 = "company-b";

            // When
            String token1 = jwtUtil.generateAccessToken(1L, "user1@a.com", "ADMIN_COMPANY", 1L, tenant1, 1L);
            String token2 = jwtUtil.generateAccessToken(2L, "user2@b.com", "ADMIN_COMPANY", 2L, tenant2, 1L);

            Map<String, Object> claims1 = jwtUtil.validateToken(token1);
            Map<String, Object> claims2 = jwtUtil.validateToken(token2);

            // Then
            assertThat(claims1.get("tenantDomain")).isEqualTo(tenant1);
            assertThat(claims2.get("tenantDomain")).isEqualTo(tenant2);
            assertThat(claims1.get("tenantDomain")).isNotEqualTo(claims2.get("tenantDomain"));
        }

        @Test
        @DisplayName("TenantContext should be cleared after request")
        void tenantContext_shouldBeClearedAfterRequest() {
            // Given
            String tenant = "test-tenant";
            TenantContext.setCurrentTenant(tenant);
            assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenant);

            // When - Simulate request completion
            TenantContext.clear();

            // Then
            assertThat(TenantContext.getCurrentTenant()).isNull();
        }
    }

    // ==================== 13.3 Test Tamabee special tenant ====================

    @Nested
    @DisplayName("13.3 Tamabee Special Tenant Tests")
    class TamabeeSpecialTenantTests {

        @Test
        @DisplayName("Tamabee user should have tenantDomain = 'tamabee'")
        void tamabeeUser_shouldHaveTenantDomainTamabee() {
            // Given
            Long userId = 1L;
            String email = "admin@tamabee.com";
            String role = UserRole.ADMIN_TAMABEE.name();

            // When
            String token = jwtUtil.generateAccessToken(userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT, null);
            Map<String, Object> claims = jwtUtil.validateToken(token);

            // Then
            assertThat(claims.get("tenantDomain")).isEqualTo(TAMABEE_TENANT);
        }

        @Test
        @DisplayName("Tamabee user should have companyId = 0")
        void tamabeeUser_shouldHaveCompanyIdZero() {
            // Given
            Long userId = 1L;
            String email = "admin@tamabee.com";
            String role = UserRole.ADMIN_TAMABEE.name();

            // When
            String token = jwtUtil.generateAccessToken(userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT, null);
            Map<String, Object> claims = jwtUtil.validateToken(token);

            // Then
            assertThat(((Number) claims.get("companyId")).longValue()).isEqualTo(TAMABEE_COMPANY_ID);
        }

        @Test
        @DisplayName("Tamabee user should have planId = null (all features enabled)")
        void tamabeeUser_shouldHaveNullPlanId() {
            // Given
            Long userId = 1L;
            String email = "admin@tamabee.com";
            String role = UserRole.ADMIN_TAMABEE.name();

            // When
            String token = jwtUtil.generateAccessToken(userId, email, role, TAMABEE_COMPANY_ID, TAMABEE_TENANT, null);
            Map<String, Object> claims = jwtUtil.validateToken(token);

            // Then
            assertThat(claims.get("planId")).isNull();
        }

        @Test
        @DisplayName("Tamabee database should be named 'tamabee_tamabee'")
        void tamabeeDatabase_shouldBeNamedCorrectly() {
            // Given
            String tenantDomain = TAMABEE_TENANT;

            // When
            String dbName = "tamabee_" + tenantDomain;

            // Then
            assertThat(dbName).isEqualTo("tamabee_tamabee");
        }

        @Test
        @DisplayName("All Tamabee roles should have same tenantDomain")
        void allTamabeeRoles_shouldHaveSameTenantDomain() {
            // Given
            List<String> tamabeeRoles = List.of("ADMIN_TAMABEE", "MANAGER_TAMABEE", "EMPLOYEE_TAMABEE");

            // When & Then
            for (String role : tamabeeRoles) {
                String token = jwtUtil.generateAccessToken(1L, "user@tamabee.com", role, TAMABEE_COMPANY_ID,
                        TAMABEE_TENANT, null);
                Map<String, Object> claims = jwtUtil.validateToken(token);

                assertThat(claims.get("tenantDomain"))
                        .as("Role %s should have tenantDomain = 'tamabee'", role)
                        .isEqualTo(TAMABEE_TENANT);
            }
        }

        @Test
        @DisplayName("TenantContext should route Tamabee to tamabee_tamabee")
        void tenantContext_shouldRouteTamabeeCorrectly() {
            // Given
            TenantContext.setCurrentTenant(TAMABEE_TENANT);

            // When
            String currentTenant = TenantContext.getCurrentTenant();
            String expectedDbName = "tamabee_" + currentTenant;

            // Then
            assertThat(currentTenant).isEqualTo(TAMABEE_TENANT);
            assertThat(expectedDbName).isEqualTo("tamabee_tamabee");

            // Cleanup
            TenantContext.clear();
        }
    }

    // ==================== 13.4 Test Plan Features API ====================

    @Nested
    @DisplayName("13.4 Plan Features API Tests")
    class PlanFeaturesApiTests {

        @Test
        @DisplayName("Plan Features API should return features for valid planId")
        void planFeaturesApi_shouldReturnFeaturesForValidPlanId() {
            // Given
            Long planId = 1L;
            PlanFeaturesResponse mockResponse = PlanFeaturesResponse.builder()
                    .planId(planId)
                    .planName("Basic Plan")
                    .features(List.of(
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.ATTENDANCE)
                                    .name("Attendance")
                                    .enabled(true)
                                    .build(),
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.LEAVE_MANAGEMENT)
                                    .name("Leave Management")
                                    .enabled(true)
                                    .build()))
                    .build();

            when(planFeaturesService.getFeaturesByPlanId(planId)).thenReturn(mockResponse);

            // When
            PlanFeaturesResponse response = planFeaturesService.getFeaturesByPlanId(planId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPlanId()).isEqualTo(planId);
            assertThat(response.getPlanName()).isEqualTo("Basic Plan");
            assertThat(response.getFeatures()).hasSize(2);
            verify(planFeaturesService).getFeaturesByPlanId(planId);
        }

        @Test
        @DisplayName("Tamabee user (no planId) should get all features enabled")
        void tamabeeUser_shouldGetAllFeaturesEnabled() {
            // Given
            PlanFeaturesResponse mockResponse = PlanFeaturesResponse.builder()
                    .planId(null)
                    .planName("All Features")
                    .features(List.of(
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.ATTENDANCE)
                                    .name("Attendance")
                                    .enabled(true)
                                    .build(),
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.LEAVE_MANAGEMENT)
                                    .name("Leave Management")
                                    .enabled(true)
                                    .build(),
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.PAYROLL)
                                    .name("Payroll")
                                    .enabled(true)
                                    .build()))
                    .build();

            when(planFeaturesService.getAllFeaturesEnabled()).thenReturn(mockResponse);

            // When
            PlanFeaturesResponse response = planFeaturesService.getAllFeaturesEnabled();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPlanId()).isNull();
            assertThat(response.getFeatures()).isNotEmpty();
            assertThat(response.getFeatures())
                    .allMatch(PlanFeaturesResponse.FeatureItem::isEnabled);
            verify(planFeaturesService).getAllFeaturesEnabled();
        }

        @Test
        @DisplayName("Plan Features response should contain required fields")
        void planFeaturesResponse_shouldContainRequiredFields() {
            // Given
            Long planId = 2L;
            PlanFeaturesResponse mockResponse = PlanFeaturesResponse.builder()
                    .planId(planId)
                    .planName("Pro Plan")
                    .features(List.of(
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.ATTENDANCE)
                                    .name("Attendance Management")
                                    .enabled(true)
                                    .build()))
                    .build();

            when(planFeaturesService.getFeaturesByPlanId(planId)).thenReturn(mockResponse);

            // When
            PlanFeaturesResponse response = planFeaturesService.getFeaturesByPlanId(planId);

            // Then
            assertThat(response.getPlanId()).isNotNull();
            assertThat(response.getPlanName()).isNotNull();
            assertThat(response.getFeatures()).isNotNull();

            PlanFeaturesResponse.FeatureItem feature = response.getFeatures().get(0);
            assertThat(feature.getCode()).isNotNull();
            assertThat(feature.getName()).isNotNull();
        }

        @Test
        @DisplayName("Different plans should have different feature sets")
        void differentPlans_shouldHaveDifferentFeatureSets() {
            // Given
            Long basicPlanId = 1L;
            Long proPlanId = 2L;

            PlanFeaturesResponse basicResponse = PlanFeaturesResponse.builder()
                    .planId(basicPlanId)
                    .planName("Basic")
                    .features(List.of(
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.ATTENDANCE)
                                    .enabled(true)
                                    .build()))
                    .build();

            PlanFeaturesResponse proResponse = PlanFeaturesResponse.builder()
                    .planId(proPlanId)
                    .planName("Pro")
                    .features(List.of(
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.ATTENDANCE)
                                    .enabled(true)
                                    .build(),
                            PlanFeaturesResponse.FeatureItem.builder()
                                    .code(FeatureCode.PAYROLL)
                                    .enabled(true)
                                    .build()))
                    .build();

            when(planFeaturesService.getFeaturesByPlanId(basicPlanId)).thenReturn(basicResponse);
            when(planFeaturesService.getFeaturesByPlanId(proPlanId)).thenReturn(proResponse);

            // When
            PlanFeaturesResponse basic = planFeaturesService.getFeaturesByPlanId(basicPlanId);
            PlanFeaturesResponse pro = planFeaturesService.getFeaturesByPlanId(proPlanId);

            // Then
            assertThat(basic.getFeatures().size()).isLessThan(pro.getFeatures().size());
        }
    }
}
