package com.tamabee.api_hr.util;

import java.util.Map;

/**
 * Utility class để xử lý locale và timezone
 */
public final class LocaleUtil {

    private LocaleUtil() {
        // Prevent instantiation
    }

    // Mapping từ locale code sang timezone
    private static final Map<String, String> LOCALE_TO_TIMEZONE = Map.of(
            "vi", "Asia/Ho_Chi_Minh",
            "ja", "Asia/Tokyo");

    // Timezone mặc định
    private static final String DEFAULT_TIMEZONE = "Asia/Tokyo";

    /**
     * Chuyển đổi locale code (vi, ja) sang timezone (Asia/Ho_Chi_Minh, Asia/Tokyo)
     * 
     * @param localeCode mã locale (vi, ja)
     * @return timezone tương ứng
     */
    public static String toTimezone(String localeCode) {
        if (localeCode == null) {
            return DEFAULT_TIMEZONE;
        }
        return LOCALE_TO_TIMEZONE.getOrDefault(localeCode, DEFAULT_TIMEZONE);
    }

    /**
     * Lấy timezone mặc định
     */
    public static String getDefaultTimezone() {
        return DEFAULT_TIMEZONE;
    }
}
