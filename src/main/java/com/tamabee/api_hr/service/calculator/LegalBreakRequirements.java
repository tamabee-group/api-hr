package com.tamabee.api_hr.service.calculator;

import org.springframework.stereotype.Component;

/**
 * Yêu cầu giờ giải lao theo quy định pháp luật
 * Hỗ trợ: Japanese labor law, Vietnamese labor law, và default
 */
@Component
public class LegalBreakRequirements {

    /**
     * Lấy thời gian giải lao tối thiểu theo quy định pháp luật
     *
     * @param locale       Locale code (ja, vi, en, ...)
     * @param workingHours Số giờ làm việc
     * @param isNightShift Có phải ca đêm không
     * @return Số phút giải lao tối thiểu
     */
    public int getMinimumBreak(String locale, int workingHours, boolean isNightShift) {
        if (locale == null) {
            return getDefaultMinimumBreak(workingHours);
        }

        return switch (locale.toLowerCase()) {
            case "ja" -> getJapaneseMinimumBreak(workingHours);
            case "vi" -> getVietnameseMinimumBreak(workingHours, isNightShift);
            default -> getDefaultMinimumBreak(workingHours);
        };
    }

    /**
     * Japanese Labor Law (労働基準法)
     * - Làm 6-8 giờ: tối thiểu 45 phút
     * - Làm trên 8 giờ: tối thiểu 60 phút
     */
    public int getJapaneseMinimumBreak(int workingHours) {
        if (workingHours <= 6) {
            return 0;
        } else if (workingHours <= 8) {
            return 45;
        } else {
            return 60;
        }
    }

    /**
     * Vietnamese Labor Law (Bộ luật Lao động)
     * - Làm trên 6 giờ ca ngày: tối thiểu 30 phút
     * - Làm trên 6 giờ ca đêm: tối thiểu 45 phút
     */
    public int getVietnameseMinimumBreak(int workingHours, boolean isNightShift) {
        if (workingHours <= 6) {
            return 0;
        }

        if (isNightShift) {
            return 45;
        }

        return 30;
    }

    /**
     * Default/Other Locales
     * - Làm trên 6 giờ: tối thiểu 30 phút
     */
    public int getDefaultMinimumBreak(int workingHours) {
        if (workingHours <= 6) {
            return 0;
        }
        return 30;
    }
}
