package com.tamabee.api_hr.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class để validate tenant domain.
 * Rules:
 * - Chỉ chấp nhận lowercase letters, numbers, hyphens
 * - Độ dài 3-30 ký tự
 * - Không bắt đầu hoặc kết thúc bằng hyphen
 * - Không được là reserved domains
 */
public final class TenantDomainValidator {

    private TenantDomainValidator() {
        // Utility class
    }

    /**
     * Pattern: lowercase letters, numbers, hyphens, 3-30 chars
     * Không bắt đầu hoặc kết thúc bằng hyphen
     * - ^[a-z0-9] : bắt đầu bằng letter hoặc number
     * - [a-z0-9-]{1,28} : middle part (1-28 chars)
     * - [a-z0-9]$ : kết thúc bằng letter hoặc number
     * Tổng: 1 + 1-28 + 1 = 3-30 chars
     */
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$");

    /**
     * Danh sách reserved domains không được phép sử dụng
     */
    public static final Set<String> RESERVED_DOMAINS = Set.of(
            "admin",
            "api",
            "www",
            "app",
            "mail",
            "tamabee");

    /**
     * Validate format của tenant domain.
     * 
     * @param domain tenant domain cần validate
     * @return true nếu format hợp lệ
     */
    public static boolean isValidFormat(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * Kiểm tra domain có phải reserved domain không.
     * 
     * @param domain tenant domain cần kiểm tra
     * @return true nếu là reserved domain
     */
    public static boolean isReserved(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        return RESERVED_DOMAINS.contains(domain.toLowerCase());
    }

    /**
     * Validate toàn bộ tenant domain (format + reserved check).
     * 
     * @param domain tenant domain cần validate
     * @return ValidationResult chứa kết quả và error code nếu có
     */
    public static ValidationResult validate(String domain) {
        if (!isValidFormat(domain)) {
            return ValidationResult.invalid(ValidationError.INVALID_FORMAT);
        }
        if (isReserved(domain)) {
            return ValidationResult.invalid(ValidationError.RESERVED);
        }
        return ValidationResult.valid();
    }

    /**
     * Enum các loại lỗi validation
     */
    public enum ValidationError {
        INVALID_FORMAT,
        RESERVED,
        ALREADY_EXISTS
    }

    /**
     * Kết quả validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final ValidationError error;

        private ValidationResult(boolean valid, ValidationError error) {
            this.valid = valid;
            this.error = error;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(ValidationError error) {
            return new ValidationResult(false, error);
        }

        public boolean isValid() {
            return valid;
        }

        public ValidationError getError() {
            return error;
        }
    }
}
