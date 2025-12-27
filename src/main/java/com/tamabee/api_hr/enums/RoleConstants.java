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
    public static final String USER_COMPANY = "USER_COMPANY";

    // PreAuthorize expressions
    public static final String HAS_ADMIN_TAMABEE = "hasRole('ADMIN_TAMABEE')";
    public static final String HAS_ADMIN_COMPANY = "hasRole('ADMIN_COMPANY')";
    public static final String HAS_COMPANY_ACCESS = "hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY')";
}
