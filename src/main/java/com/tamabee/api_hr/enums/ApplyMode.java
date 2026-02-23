package com.tamabee.api_hr.enums;

/**
 * Chế độ áp dụng nguyện vọng thành phân ca
 */
public enum ApplyMode {
    EXISTING_TEMPLATES, // Áp dụng shift templates có sẵn
    CREATE_NEW,         // Tạo shift template mới
    HYBRID              // Kết hợp templates có sẵn + tạo mới cho phần thiếu
}
