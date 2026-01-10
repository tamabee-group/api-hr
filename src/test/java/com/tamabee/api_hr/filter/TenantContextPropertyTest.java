package com.tamabee.api_hr.filter;

import com.tamabee.api_hr.datasource.TenantContext;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests cho TenantContext lifecycle.
 * Validates Property 3: Tenant Context Lifecycle
 * - TenantContext SHALL contain correct tenantDomain during request processing
 * - TenantContext SHALL be cleared after request completes
 */
class TenantContextPropertyTest {

    /**
     * Property: Bất kỳ tenant domain nào được set vào TenantContext
     * đều phải được trả về chính xác khi gọi getCurrentTenant()
     */
    @Property(tries = 100)
    void setAndGetTenantDomain_shouldReturnSameValue(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String tenantDomain) {
        try {
            TenantContext.setCurrentTenant(tenantDomain);

            String result = TenantContext.getCurrentTenant();

            assertThat(result).isEqualTo(tenantDomain);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Property: Sau khi clear(), getCurrentTenant() phải trả về null
     */
    @Property(tries = 100)
    void clearTenantContext_shouldReturnNull(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String tenantDomain) {
        TenantContext.setCurrentTenant(tenantDomain);

        TenantContext.clear();

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    /**
     * Property: Tenant domain với format hợp lệ (lowercase, numbers, hyphens)
     * phải được lưu và trả về chính xác
     */
    @Property(tries = 100)
    void validTenantDomainFormat_shouldBeStoredCorrectly(
            @ForAll("validTenantDomains") String tenantDomain) {
        try {
            TenantContext.setCurrentTenant(tenantDomain);

            assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenantDomain);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Property: Nhiều lần set liên tiếp, chỉ giá trị cuối cùng được giữ lại
     */
    @Property(tries = 50)
    void multipleSetOperations_shouldKeepLastValue(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String first,
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String second,
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String third) {
        try {
            TenantContext.setCurrentTenant(first);
            TenantContext.setCurrentTenant(second);
            TenantContext.setCurrentTenant(third);

            assertThat(TenantContext.getCurrentTenant()).isEqualTo(third);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Example: Set null phải được chấp nhận và trả về null
     */
    @Example
    void setNullTenant_shouldReturnNull() {
        try {
            TenantContext.setCurrentTenant(null);

            assertThat(TenantContext.getCurrentTenant()).isNull();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Property: Clear nhiều lần không gây lỗi
     */
    @Property(tries = 50)
    void multipleClearOperations_shouldNotThrowException(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String tenantDomain) {
        TenantContext.setCurrentTenant(tenantDomain);

        // Clear nhiều lần
        TenantContext.clear();
        TenantContext.clear();
        TenantContext.clear();

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    /**
     * Property: "tamabee" tenant domain (special tenant) phải được xử lý đúng
     */
    @Example
    void tamabeeTenant_shouldBeHandledCorrectly() {
        try {
            TenantContext.setCurrentTenant("tamabee");

            assertThat(TenantContext.getCurrentTenant()).isEqualTo("tamabee");
        } finally {
            TenantContext.clear();
        }
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
                .filter(s -> !s.startsWith("-") && !s.endsWith("-"));
    }
}
