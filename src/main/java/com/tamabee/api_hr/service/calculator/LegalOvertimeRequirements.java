package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.OvertimeMultipliers;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Yêu cầu tăng ca theo quy định pháp luật
 * Hỗ trợ: Japanese labor law, Vietnamese labor law, và default
 */
@Component
public class LegalOvertimeRequirements {

    /**
     * Lấy hệ số tăng ca tối thiểu theo quy định pháp luật
     *
     * @param locale Locale code (ja, vi, en, ...)
     * @return Hệ số tăng ca tối thiểu
     */
    public OvertimeMultipliers getMinimumMultipliers(String locale) {
        if (locale == null) {
            return getDefaultMinimumMultipliers();
        }

        return switch (locale.toLowerCase()) {
            case "ja" -> getJapaneseMinimumMultipliers();
            case "vi" -> getVietnameseMinimumMultipliers();
            default -> getDefaultMinimumMultipliers();
        };
    }

    /**
     * Japanese Labor Law (労働基準法)
     * - Tăng ca thường: 1.25x (25% tăng thêm)
     * - Làm đêm (22:00-05:00): 1.25x
     * - Tăng ca đêm: 1.50x (25% + 25%)
     * - Tăng ca ngày lễ: 1.35x
     * - Tăng ca đêm ngày lễ: 1.60x (35% + 25%)
     */
    public OvertimeMultipliers getJapaneseMinimumMultipliers() {
        return OvertimeMultipliers.builder()
                .regularOvertime(new BigDecimal("1.25"))
                .nightWork(new BigDecimal("1.25"))
                .nightOvertime(new BigDecimal("1.50"))
                .holidayOvertime(new BigDecimal("1.35"))
                .holidayNightOvertime(new BigDecimal("1.60"))
                .weekendOvertime(new BigDecimal("1.35"))
                .build();
    }

    /**
     * Vietnamese Labor Law (Bộ luật Lao động)
     * - Tăng ca thường: 1.50x (150% lương)
     * - Làm đêm (22:00-06:00): 1.30x
     * - Tăng ca đêm: 1.95x (150% * 130%)
     * - Tăng ca ngày lễ: 2.00x (200% lương)
     * - Tăng ca đêm ngày lễ: 2.60x (200% * 130%)
     */
    public OvertimeMultipliers getVietnameseMinimumMultipliers() {
        return OvertimeMultipliers.builder()
                .regularOvertime(new BigDecimal("1.50"))
                .nightWork(new BigDecimal("1.30"))
                .nightOvertime(new BigDecimal("1.95"))
                .holidayOvertime(new BigDecimal("2.00"))
                .holidayNightOvertime(new BigDecimal("2.60"))
                .weekendOvertime(new BigDecimal("2.00"))
                .build();
    }

    /**
     * Default/Other Locales
     * Sử dụng mức tối thiểu phổ biến
     */
    public OvertimeMultipliers getDefaultMinimumMultipliers() {
        return OvertimeMultipliers.builder()
                .regularOvertime(new BigDecimal("1.25"))
                .nightWork(new BigDecimal("1.25"))
                .nightOvertime(new BigDecimal("1.50"))
                .holidayOvertime(new BigDecimal("1.50"))
                .holidayNightOvertime(new BigDecimal("1.75"))
                .weekendOvertime(new BigDecimal("1.35"))
                .build();
    }

    /**
     * Kiểm tra xem multiplier có đạt legal minimum không
     *
     * @param multiplier Hệ số cần kiểm tra
     * @param minimum    Hệ số tối thiểu
     * @return true nếu multiplier >= minimum
     */
    public boolean isMultiplierCompliant(BigDecimal multiplier, BigDecimal minimum) {
        if (multiplier == null || minimum == null) {
            return false;
        }
        return multiplier.compareTo(minimum) >= 0;
    }

    /**
     * Validate tất cả multipliers trong config
     *
     * @param multipliers Hệ số cần validate
     * @param locale      Locale để lấy legal minimum
     * @return true nếu tất cả multipliers đều compliant
     */
    public boolean validateMultipliers(OvertimeMultipliers multipliers, String locale) {
        if (multipliers == null) {
            return false;
        }

        OvertimeMultipliers minimum = getMinimumMultipliers(locale);

        return isMultiplierCompliant(multipliers.getRegularOvertime(), minimum.getRegularOvertime())
                && isMultiplierCompliant(multipliers.getNightWork(), minimum.getNightWork())
                && isMultiplierCompliant(multipliers.getNightOvertime(), minimum.getNightOvertime())
                && isMultiplierCompliant(multipliers.getHolidayOvertime(), minimum.getHolidayOvertime())
                && isMultiplierCompliant(multipliers.getHolidayNightOvertime(), minimum.getHolidayNightOvertime())
                && isMultiplierCompliant(multipliers.getWeekendOvertime(), minimum.getWeekendOvertime());
    }
}
