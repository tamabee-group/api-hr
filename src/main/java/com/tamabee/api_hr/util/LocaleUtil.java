package com.tamabee.api_hr.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Utility class để xử lý locale và timezone
 */
public final class LocaleUtil {

    private LocaleUtil() {
        // Prevent instantiation
    }

    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

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

    /**
     * Lấy locale hiện tại từ Accept-Language header của request.
     * Nếu không có request hoặc header, trả về locale mặc định.
     * 
     * @return locale code (vi, ja, en)
     */
    public static String getCurrentLocale() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER);
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    // Parse Accept-Language header (e.g., "vi", "ja", "en-US")
                    String locale = acceptLanguage.split(",")[0].split("-")[0].toLowerCase();
                    if (LOCALE_TO_TIMEZONE.containsKey(locale)) {
                        return locale;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exception, return default
        }
        return DEFAULT_LOCALE;
    }
}
