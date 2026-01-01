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
            "ja", "Asia/Tokyo",
            "en", "UTC");

    // Mapping từ timezone sang locale code
    private static final Map<String, String> TIMEZONE_TO_LOCALE = Map.of(
            "Asia/Ho_Chi_Minh", "vi",
            "Asia/Tokyo", "ja",
            "UTC", "en");

    // Timezone mặc định
    private static final String DEFAULT_TIMEZONE = "Asia/Tokyo";
    private static final String DEFAULT_LOCALE = "en";

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
     * Chuyển đổi timezone sang locale code
     *
     * @param timezone timezone (Asia/Ho_Chi_Minh, Asia/Tokyo)
     * @return locale code tương ứng (vi, ja, en)
     */
    public static String timezoneToLocale(String timezone) {
        if (timezone == null) {
            return DEFAULT_LOCALE;
        }
        return TIMEZONE_TO_LOCALE.getOrDefault(timezone, DEFAULT_LOCALE);
    }

    /**
     * Lấy timezone mặc định
     */
    public static String getDefaultTimezone() {
        return DEFAULT_TIMEZONE;
    }

    /**
     * Lấy locale mặc định
     */
    public static String getDefaultLocale() {
        return DEFAULT_LOCALE;
    }
}
