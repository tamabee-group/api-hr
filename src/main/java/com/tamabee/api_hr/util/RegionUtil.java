package com.tamabee.api_hr.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * Utility class để xử lý region và timezone.
 * Region xác định vùng hoạt động của company (vi = Việt Nam, ja = Nhật Bản).
 * Thay thế LocaleUtil.java — tách biệt khái niệm "region" (vùng) khỏi "region" (ngôn ngữ giao diện).
 */
public final class RegionUtil {

    private RegionUtil() {
        // Prevent instantiation
    }

    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    // Mapping từ region code sang timezone
    private static final Map<String, ZoneId> REGION_TO_TIMEZONE = Map.of(
            "vi", ZoneId.of("Asia/Ho_Chi_Minh"),
            "ja", ZoneId.of("Asia/Tokyo"));

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");
    private static final Set<String> VALID_REGIONS = Set.of("vi", "ja");
    private static final String DEFAULT_LOCALE = "en";

    /**
     * Lấy ZoneId từ region code.
     *
     * @param region mã region (vi, ja)
     * @return ZoneId tương ứng, hoặc UTC nếu region không hợp lệ
     */
    public static ZoneId getTimezone(String region) {
        if (region == null) {
            return DEFAULT_TIMEZONE;
        }
        return REGION_TO_TIMEZONE.getOrDefault(region, DEFAULT_TIMEZONE);
    }

    /**
     * Lấy timezone string từ region code.
     *
     * @param region mã region (vi, ja)
     * @return timezone string (e.g. "Asia/Ho_Chi_Minh", "Asia/Tokyo", "UTC")
     */
    public static String toTimezoneString(String region) {
        return getTimezone(region).getId();
    }

    /**
     * Kiểm tra region có hợp lệ không.
     * Chỉ "vi" và "ja" là hợp lệ.
     *
     * @param region mã region cần kiểm tra
     * @return true nếu region hợp lệ
     */
    public static boolean isValidRegion(String region) {
        return region != null && VALID_REGIONS.contains(region);
    }

    /**
     * Lấy region hiện tại từ Accept-Language header của request.
     * Giữ lại method này cho Accept-Language header — không liên quan đến region.
     *
     * @return region code (vi, ja, en)
     */
    public static String getCurrentLocale() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER);
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    String region = acceptLanguage.split(",")[0].split("-")[0].toLowerCase();
                    if (VALID_REGIONS.contains(region) || "en".equals(region)) {
                        return region;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exception, return default
        }
        return DEFAULT_LOCALE;
    }
}
