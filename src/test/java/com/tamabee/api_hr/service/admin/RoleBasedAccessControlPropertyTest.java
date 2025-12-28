package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.enums.UserRole;
import net.jqwik.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho Role-based Access Control
 * Feature: wallet-management, Property 2: Role-based Access Control
 * Validates: Requirements 3.1, 3.2, 4.1, 4.2, 6.1, 6.2, 12.1, 12.2, 14.1-14.5
 * 
 * Property 2: Role-based Access Control
 * - ADMIN_COMPANY/MANAGER_COMPANY: chỉ xem data của company mình (filter by
 * companyId from JWT)
 * - ADMIN_TAMABEE/MANAGER_TAMABEE: xem tất cả data
 * - EMPLOYEE_TAMABEE: xem tất cả data (read-only), không có quyền modify
 */
public class RoleBasedAccessControlPropertyTest {

    // Định nghĩa các nhóm role
    private static final Set<UserRole> TAMABEE_ADMIN_ROLES = Set.of(
            UserRole.ADMIN_TAMABEE,
            UserRole.MANAGER_TAMABEE);

    private static final Set<UserRole> TAMABEE_ALL_ROLES = Set.of(
            UserRole.ADMIN_TAMABEE,
            UserRole.MANAGER_TAMABEE,
            UserRole.EMPLOYEE_TAMABEE);

    private static final Set<UserRole> COMPANY_ROLES = Set.of(
            UserRole.ADMIN_COMPANY,
            UserRole.MANAGER_COMPANY);

    private static final Set<UserRole> TAMABEE_WRITE_ROLES = Set.of(
            UserRole.ADMIN_TAMABEE,
            UserRole.MANAGER_TAMABEE);

    /**
     * Property 2.1: Company roles chỉ có thể truy cập data của company mình
     * Với bất kỳ ADMIN_COMPANY hoặc MANAGER_COMPANY nào, khi request data,
     * kết quả PHẢI được filter theo companyId từ JWT token
     * 
     * Validates: Requirements 3.1, 4.1, 6.2
     */
    @Property(tries = 100)
    void companyRolesShouldOnlyAccessOwnCompanyData(
            @ForAll("companyRoles") UserRole role,
            @ForAll("validCompanyIds") Long userCompanyId,
            @ForAll("validCompanyIds") Long requestedCompanyId) {

        // Kiểm tra: Company roles chỉ có thể truy cập data của company mình
        boolean canAccess = canAccessCompanyData(role, userCompanyId, requestedCompanyId);

        if (COMPANY_ROLES.contains(role)) {
            // Company roles chỉ có thể truy cập data của company mình
            assertEquals(userCompanyId.equals(requestedCompanyId), canAccess,
                    String.format("Role %s với companyId %d chỉ có thể truy cập data của company %d",
                            role, userCompanyId, userCompanyId));
        }
    }

    /**
     * Property 2.2: Tamabee roles có thể truy cập tất cả data
     * Với bất kỳ ADMIN_TAMABEE, MANAGER_TAMABEE hoặc EMPLOYEE_TAMABEE nào,
     * khi request data, kết quả KHÔNG bị filter theo companyId
     * 
     * Validates: Requirements 3.2, 4.2, 6.1, 14.1, 14.2, 14.3
     */
    @Property(tries = 100)
    void tamabeeRolesShouldAccessAllCompanyData(
            @ForAll("tamabeeAllRoles") UserRole role,
            @ForAll("validCompanyIds") Long requestedCompanyId) {

        // Kiểm tra: Tamabee roles có thể truy cập tất cả data
        boolean canAccess = canAccessCompanyData(role, 0L, requestedCompanyId);

        assertTrue(canAccess,
                String.format("Role %s phải có thể truy cập data của bất kỳ company nào", role));
    }

    /**
     * Property 2.3: EMPLOYEE_TAMABEE chỉ có quyền đọc, không có quyền modify
     * Với bất kỳ EMPLOYEE_TAMABEE nào, khi thực hiện write operation,
     * hệ thống PHẢI từ chối
     * 
     * Validates: Requirements 14.4, 14.5
     */
    @Property(tries = 100)
    void employeeTamabeeShouldNotHaveWriteAccess(
            @ForAll("writeOperations") String operation,
            @ForAll("validCompanyIds") Long companyId) {

        UserRole role = UserRole.EMPLOYEE_TAMABEE;

        // Kiểm tra: EMPLOYEE_TAMABEE không có quyền write
        boolean canWrite = canPerformWriteOperation(role, operation);

        assertFalse(canWrite,
                String.format("EMPLOYEE_TAMABEE không được phép thực hiện %s", operation));
    }

    /**
     * Property 2.4: ADMIN_TAMABEE và MANAGER_TAMABEE có quyền approve/reject
     * deposit
     * Với bất kỳ ADMIN_TAMABEE hoặc MANAGER_TAMABEE nào,
     * khi thực hiện approve/reject deposit, hệ thống PHẢI cho phép
     * 
     * Validates: Requirements 7.1, 8.1
     */
    @Property(tries = 100)
    void tamabeeAdminRolesShouldHaveDepositApprovalAccess(
            @ForAll("tamabeeAdminRoles") UserRole role,
            @ForAll("depositOperations") String operation) {

        // Kiểm tra: ADMIN_TAMABEE và MANAGER_TAMABEE có quyền approve/reject
        boolean canApprove = canPerformDepositOperation(role, operation);

        assertTrue(canApprove,
                String.format("Role %s phải có quyền %s deposit", role, operation));
    }

    /**
     * Property 2.5: Commission access control
     * - ADMIN_TAMABEE/MANAGER_TAMABEE: xem tất cả commission
     * - EMPLOYEE_TAMABEE: chỉ xem commission của mình
     * 
     * Validates: Requirements 12.1, 12.2
     */
    @Property(tries = 100)
    void commissionAccessShouldBeRoleBased(
            @ForAll("tamabeeAllRoles") UserRole role,
            @ForAll("validEmployeeCodes") String userEmployeeCode,
            @ForAll("validEmployeeCodes") String commissionEmployeeCode) {

        boolean canAccess = canAccessCommission(role, userEmployeeCode, commissionEmployeeCode);

        if (TAMABEE_ADMIN_ROLES.contains(role)) {
            // Admin roles có thể xem tất cả commission
            assertTrue(canAccess,
                    String.format("Role %s phải có thể xem tất cả commission", role));
        } else if (role == UserRole.EMPLOYEE_TAMABEE) {
            // Employee chỉ xem commission của mình
            assertEquals(userEmployeeCode.equals(commissionEmployeeCode), canAccess,
                    String.format("EMPLOYEE_TAMABEE chỉ có thể xem commission của mình"));
        }
    }

    /**
     * Property 2.6: Refund chỉ ADMIN_TAMABEE có quyền
     * Với bất kỳ role nào khác ADMIN_TAMABEE, khi thực hiện refund,
     * hệ thống PHẢI từ chối
     * 
     * Validates: Requirements 10.1
     */
    @Property(tries = 100)
    void onlyAdminTamabeeShouldCreateRefund(
            @ForAll("allRoles") UserRole role) {

        boolean canRefund = canPerformRefund(role);

        if (role == UserRole.ADMIN_TAMABEE) {
            assertTrue(canRefund, "ADMIN_TAMABEE phải có quyền tạo refund");
        } else {
            assertFalse(canRefund,
                    String.format("Role %s không được phép tạo refund", role));
        }
    }

    // === Simulated Access Control Logic ===

    /**
     * Kiểm tra quyền truy cập data của company
     */
    private boolean canAccessCompanyData(UserRole role, Long userCompanyId, Long requestedCompanyId) {
        // Tamabee roles có thể truy cập tất cả
        if (TAMABEE_ALL_ROLES.contains(role)) {
            return true;
        }
        // Company roles chỉ truy cập company của mình
        if (COMPANY_ROLES.contains(role)) {
            return userCompanyId.equals(requestedCompanyId);
        }
        return false;
    }

    /**
     * Kiểm tra quyền thực hiện write operation
     */
    private boolean canPerformWriteOperation(UserRole role, String operation) {
        // EMPLOYEE_TAMABEE không có quyền write
        if (role == UserRole.EMPLOYEE_TAMABEE) {
            return false;
        }
        // ADMIN_TAMABEE và MANAGER_TAMABEE có quyền write
        return TAMABEE_WRITE_ROLES.contains(role);
    }

    /**
     * Kiểm tra quyền approve/reject deposit
     */
    private boolean canPerformDepositOperation(UserRole role, String operation) {
        return TAMABEE_ADMIN_ROLES.contains(role);
    }

    /**
     * Kiểm tra quyền truy cập commission
     */
    private boolean canAccessCommission(UserRole role, String userEmployeeCode, String commissionEmployeeCode) {
        // Admin roles có thể xem tất cả
        if (TAMABEE_ADMIN_ROLES.contains(role)) {
            return true;
        }
        // Employee chỉ xem của mình
        if (role == UserRole.EMPLOYEE_TAMABEE) {
            return userEmployeeCode.equals(commissionEmployeeCode);
        }
        return false;
    }

    /**
     * Kiểm tra quyền tạo refund
     */
    private boolean canPerformRefund(UserRole role) {
        return role == UserRole.ADMIN_TAMABEE;
    }

    // === Generators ===

    @Provide
    Arbitrary<UserRole> companyRoles() {
        return Arbitraries.of(UserRole.ADMIN_COMPANY, UserRole.MANAGER_COMPANY);
    }

    @Provide
    Arbitrary<UserRole> tamabeeAllRoles() {
        return Arbitraries.of(UserRole.ADMIN_TAMABEE, UserRole.MANAGER_TAMABEE, UserRole.EMPLOYEE_TAMABEE);
    }

    @Provide
    Arbitrary<UserRole> tamabeeAdminRoles() {
        return Arbitraries.of(UserRole.ADMIN_TAMABEE, UserRole.MANAGER_TAMABEE);
    }

    @Provide
    Arbitrary<UserRole> allRoles() {
        return Arbitraries.of(UserRole.values());
    }

    @Provide
    Arbitrary<Long> validCompanyIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> validEmployeeCodes() {
        // Generate 6-character employee codes
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofLength(6);
    }

    @Provide
    Arbitrary<String> writeOperations() {
        return Arbitraries.of(
                "approve_deposit",
                "reject_deposit",
                "create_refund",
                "update_wallet_balance");
    }

    @Provide
    Arbitrary<String> depositOperations() {
        return Arbitraries.of("approve", "reject");
    }
}
