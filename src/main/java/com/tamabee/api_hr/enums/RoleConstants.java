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
    public static final String HAS_ADMIN_COMPANY = "hasRole('ADMIN_COMPANY')";
    public static final String HAS_COMPANY_ACCESS = "hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY')";
    public static final String HAS_TAMABEE_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')";
    public static final String HAS_EMPLOYEE_TAMABEE = "hasRole('EMPLOYEE_TAMABEE')";
    public static final String HAS_ALL_TAMABEE_ACCESS = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE')";

    // Admin-only operations - Chỉ Admin Tamabee có quyền thao tác wallet trực tiếp
    public static final String HAS_ADMIN_TAMABEE_ONLY = "hasRole('ADMIN_TAMABEE')";

    // Tamabee Staff - Admin + Manager có quyền xử lý deposit requests
    public static final String HAS_TAMABEE_STAFF = "hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')";
}
