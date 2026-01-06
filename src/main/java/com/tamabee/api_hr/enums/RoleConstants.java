package com.tamabee.api_hr.enums;

/**
 * Constants cho role names sử dụng trong @PreAuthorize
 * Giá trị phải khớp với UserRole enum
 */
public final class RoleConstants {

    private RoleConstants() {
        // Prevent instantiation
    }

    // Tamabee roles
    public static final String ADMIN_TAMABEE = "ADMIN_TAMABEE";
    public static final String MANAGER_TAMABEE = "MANAGER_TAMABEE";
    public static final String EMPLOYEE_TAMABEE = "EMPLOYEE_TAMABEE";

    // Company roles
    public static final String ADMIN_COMPANY = "ADMIN_COMPANY";
    public static final String MANAGER_COMPANY = "MANAGER_COMPANY";
    public static final String EMPLOYEE_COMPANY = "EMPLOYEE_COMPANY";

    // PreAuthorize expressions
    public static final String HAS_ADMIN_TAMABEE = "hasRole('ADMIN_TAMABEE')";
    public static final String HAS_ADMIN_COMPANY = "hasAnyRole('ADMIN_COMPANY', 'ADMIN_TAMABEE')";
    public static final String HAS_COMPANY_ACCESS = "hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'ADMIN_TAMABEE', 'MANAGER_TAMABEE')";
    public static final String HAS_TAMABEE_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')";
    public static final String HAS_EMPLOYEE_TAMABEE = "hasRole('EMPLOYEE_TAMABEE')";
    public static final String HAS_ALL_TAMABEE_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE')";

    // Admin-only operations - Chỉ Admin Tamabee có quyền thao tác wallet trực tiếp
    public static final String HAS_ADMIN_TAMABEE_ONLY = "hasRole('ADMIN_TAMABEE')";

    // Tamabee Staff - Admin + Manager có quyền xử lý deposit requests
    public static final String HAS_TAMABEE_STAFF = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')";

    // Employee Company - Nhân viên công ty có quyền truy cập các tính năng cơ bản
    public static final String HAS_EMPLOYEE_COMPANY = "hasRole('EMPLOYEE_COMPANY')";

    // All Company Access - Tất cả nhân viên (Admin, Manager, Employee) của cả
    // Tamabee và Company
    public static final String HAS_ALL_COMPANY_ACCESS = "hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'EMPLOYEE_COMPANY', 'ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE')";

    // ========== Multi-Tenant Access (Tamabee + Company) ==========
    // Cho phép Tamabee admins access cả platform management và HR features

    // Admin access cho cả Tamabee và Company (HR admin features)
    public static final String HAS_ADMIN_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'ADMIN_COMPANY')";

    // Manager access cho cả Tamabee và Company (HR management features)
    public static final String HAS_MANAGER_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'ADMIN_COMPANY', 'MANAGER_COMPANY')";

    // All HR access - Tất cả users có thể sử dụng HR features
    public static final String HAS_HR_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE', 'ADMIN_COMPANY', 'MANAGER_COMPANY', 'EMPLOYEE_COMPANY')";
}
