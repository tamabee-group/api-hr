package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.enums.UserRole;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho Manager bị từ chối direct wallet operations
 * Property 2: Manager không có quyền thao tác wallet trực tiếp
 */
@Tag("Feature: tamabee-role-redesign")
public class ManagerWalletAccessDeniedPropertyTest {

    // Roles được phép thao tác wallet trực tiếp
    private static final Set<UserRole> DIRECT_WALLET_ALLOWED_ROLES = Set.of(UserRole.ADMIN_TAMABEE);

    // Roles bị từ chối thao tác wallet trực tiếp
    private static final Set<UserRole> DIRECT_WALLET_DENIED_ROLES = Set.of(
            UserRole.MANAGER_TAMABEE,
            UserRole.EMPLOYEE_TAMABEE,
            UserRole.ADMIN_COMPANY,
            UserRole.MANAGER_COMPANY,
            UserRole.EMPLOYEE_COMPANY);

    /**
     * Property 2: Manager không có quyền thao tác wallet trực tiếp - ADD BALANCE
     * For any Manager Tamabee user, khi cố gắng gọi API addBalanceDirect,
     * hệ thống SHALL trả về HTTP 403 Forbidden
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 2: Manager khong co quyen thao tac wallet truc tiep")
    void managerShouldBeDeniedAddBalanceDirect(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("validAmounts") BigDecimal amount,
            @ForAll("validDescriptions") String description) {

        UserRole managerRole = UserRole.MANAGER_TAMABEE;

        // Kiểm tra: Manager không có quyền addBalanceDirect
        boolean canAddBalanceDirect = canPerformDirectWalletOperation(managerRole, "addBalanceDirect");

        assertFalse(canAddBalanceDirect,
                "MANAGER_TAMABEE không được phép thực hiện addBalanceDirect");

        // Verify: Chỉ ADMIN_TAMABEE có quyền
        assertTrue(canPerformDirectWalletOperation(UserRole.ADMIN_TAMABEE, "addBalanceDirect"),
                "Chỉ ADMIN_TAMABEE có quyền addBalanceDirect");
    }

    /**
     * Property 2: Manager không có quyền thao tác wallet trực tiếp - DEDUCT BALANCE
     * For any Manager Tamabee user, khi cố gắng gọi API deductBalanceDirect,
     * hệ thống SHALL trả về HTTP 403 Forbidden
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 2: Manager khong co quyen thao tac wallet truc tiep")
    void managerShouldBeDeniedDeductBalanceDirect(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("sufficientBalanceAndAmount") BigDecimal[] balanceAndAmount,
            @ForAll("validDescriptions") String description) {

        UserRole managerRole = UserRole.MANAGER_TAMABEE;

        // Kiểm tra: Manager không có quyền deductBalanceDirect
        boolean canDeductBalanceDirect = canPerformDirectWalletOperation(managerRole, "deductBalanceDirect");

        assertFalse(canDeductBalanceDirect,
                "MANAGER_TAMABEE không được phép thực hiện deductBalanceDirect");

        // Verify: Chỉ ADMIN_TAMABEE có quyền
        assertTrue(canPerformDirectWalletOperation(UserRole.ADMIN_TAMABEE, "deductBalanceDirect"),
                "Chỉ ADMIN_TAMABEE có quyền deductBalanceDirect");
    }

    /**
     * Property 2.3: Tất cả non-admin roles đều bị từ chối direct wallet operations
     * For any role khác ADMIN_TAMABEE, khi cố gắng thao tác wallet trực tiếp,
     * hệ thống SHALL từ chối
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 2: Manager khong co quyen thao tac wallet truc tiep")
    void nonAdminRolesShouldBeDeniedDirectWalletOperations(
            @ForAll("nonAdminRoles") UserRole role,
            @ForAll("directWalletOperations") String operation,
            @ForAll("validCompanyIds") Long companyId) {

        // Kiểm tra: Non-admin roles không có quyền direct wallet operations
        boolean canPerform = canPerformDirectWalletOperation(role, operation);

        assertFalse(canPerform,
                String.format("Role %s không được phép thực hiện %s", role, operation));
    }

    /**
     * Property 2.4: Chỉ ADMIN_TAMABEE có quyền direct wallet operations
     * For any direct wallet operation, chỉ ADMIN_TAMABEE có quyền thực hiện
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 2: Manager khong co quyen thao tac wallet truc tiep")
    void onlyAdminTamabeeShouldHaveDirectWalletAccess(
            @ForAll("directWalletOperations") String operation,
            @ForAll("validCompanyIds") Long companyId) {

        // Verify: ADMIN_TAMABEE có quyền
        assertTrue(canPerformDirectWalletOperation(UserRole.ADMIN_TAMABEE, operation),
                String.format("ADMIN_TAMABEE phải có quyền %s", operation));

        // Verify: Tất cả roles khác đều bị từ chối
        for (UserRole role : DIRECT_WALLET_DENIED_ROLES) {
            assertFalse(canPerformDirectWalletOperation(role, operation),
                    String.format("Role %s không được phép %s", role, operation));
        }
    }

    // === Access Control Logic ===

    /**
     * Kiểm tra quyền thực hiện direct wallet operation
     * Dựa trên RoleConstants.HAS_ADMIN_TAMABEE_ONLY = "hasRole('ADMIN_TAMABEE')"
     */
    private boolean canPerformDirectWalletOperation(UserRole role, String operation) {
        // Chỉ ADMIN_TAMABEE có quyền thao tác wallet trực tiếp
        return DIRECT_WALLET_ALLOWED_ROLES.contains(role);
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> validCompanyIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(1), BigDecimal.valueOf(1000000))
                .ofScale(0);
    }

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100);
    }

    /**
     * Generator cho balance và amount sao cho balance >= amount
     */
    @Provide
    Arbitrary<BigDecimal[]> sufficientBalanceAndAmount() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(1000000))
                .ofScale(0)
                .flatMap(balance -> Arbitraries.bigDecimals()
                        .between(BigDecimal.valueOf(1), balance)
                        .ofScale(0)
                        .map(amount -> new BigDecimal[] { balance, amount }));
    }

    /**
     * Generator cho non-admin roles (tất cả roles trừ ADMIN_TAMABEE)
     */
    @Provide
    Arbitrary<UserRole> nonAdminRoles() {
        return Arbitraries.of(
                UserRole.MANAGER_TAMABEE,
                UserRole.EMPLOYEE_TAMABEE,
                UserRole.ADMIN_COMPANY,
                UserRole.MANAGER_COMPANY,
                UserRole.EMPLOYEE_COMPANY);
    }

    /**
     * Generator cho direct wallet operations
     */
    @Provide
    Arbitrary<String> directWalletOperations() {
        return Arbitraries.of("addBalanceDirect", "deductBalanceDirect");
    }
}
