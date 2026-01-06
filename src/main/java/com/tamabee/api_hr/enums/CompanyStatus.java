package com.tamabee.api_hr.enums;

/**
 * Trạng thái của công ty
 */
public enum CompanyStatus {
    ACTIVE, // Đang hoạt động
    INACTIVE, // Tạm ngưng (do không đủ số dư để billing)
    PROVISIONING, // Đang tạo tenant database
    FAILED // Tạo tenant database thất bại
}
