package com.tamabee.api_hr.service.calculator.impl;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.service.calculator.interfaces.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Calculator tính toán giờ giải lao
 * Hỗ trợ: tính tổng break, effective break với min/max capping,
 * net working hours, night shift detection
 */
@Component
@RequiredArgsConstructor
public class BreakCalculatorImpl implements IBreakCalculator {

    private final LegalBreakRequirements legalBreakRequirements;

    @Override
    public int calculateTotalBreakMinutes(List<BreakRecordEntity> breakRecords) {
        if (breakRecords == null || breakRecords.isEmpty()) {
            return 0;
        }

        return breakRecords.stream()
                .filter(record -> record != null && record.getActualBreakMinutes() != null)
                .mapToInt(BreakRecordEntity::getActualBreakMinutes)
                .sum();
    }

    @Override
    public int calculateEffectiveBreakMinutes(
            int actualBreakMinutes,
            BreakConfig config,
            int workingHours,
            boolean isNightShift) {

        if (config == null || !Boolean.TRUE.equals(config.getBreakEnabled())) {
            return 0;
        }

        // Lấy minimum break
        int minimumBreak = getEffectiveMinimumBreak(config, workingHours, isNightShift);

        // Lấy maximum break
        int maximumBreak = config.getMaximumBreakMinutes() != null
                ? config.getMaximumBreakMinutes()
                : Integer.MAX_VALUE;

        // Áp dụng capping: min <= effective <= max
        int effectiveBreak = actualBreakMinutes;

        // Nếu actual < minimum, sử dụng minimum (đảm bảo tuân thủ pháp luật)
        if (effectiveBreak < minimumBreak) {
            effectiveBreak = minimumBreak;
        }

        // Nếu actual > maximum, cap tại maximum
        if (effectiveBreak > maximumBreak) {
            effectiveBreak = maximumBreak;
        }

        return effectiveBreak;
    }

    @Override
    public int calculateNetWorkingMinutes(
            int grossWorkingMinutes,
            int breakMinutes,
            BreakConfig config) {

        if (config == null || !Boolean.TRUE.equals(config.getBreakEnabled())) {
            return grossWorkingMinutes;
        }

        // Nếu break là PAID, không trừ break khỏi working hours
        if (config.getBreakType() == BreakType.PAID) {
            return grossWorkingMinutes;
        }

        // Nếu break là UNPAID, trừ break khỏi working hours
        int netMinutes = grossWorkingMinutes - breakMinutes;
        return Math.max(0, netMinutes);
    }

    @Override
    public int getLegalMinimumBreak(String locale, int workingHours, boolean isNightShift) {
        return legalBreakRequirements.getMinimumBreak(locale, workingHours, isNightShift);
    }

    @Override
    public boolean isNightShift(LocalTime shiftStart, LocalTime shiftEnd, BreakConfig config) {
        if (shiftStart == null || shiftEnd == null || config == null) {
            return false;
        }

        LocalTime nightStart = config.getNightShiftStartTime() != null
                ? config.getNightShiftStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightShiftEndTime() != null
                ? config.getNightShiftEndTime()
                : LocalTime.of(5, 0);

        // Kiểm tra xem shift có overlap với night hours không
        // Night hours thường là 22:00 - 05:00 (qua đêm)

        // Trường hợp 1: Shift qua đêm (start > end, ví dụ 17:00 - 07:00)
        if (shiftStart.isAfter(shiftEnd) || shiftStart.equals(shiftEnd)) {
            // Shift qua đêm luôn overlap với night hours
            return true;
        }

        // Trường hợp 2: Shift trong ngày
        // Kiểm tra overlap với night hours (22:00 - 05:00)
        // Night hours qua đêm nên cần xử lý đặc biệt
        if (nightStart.isAfter(nightEnd)) {
            // Night hours qua đêm (22:00 - 05:00)
            // Shift overlap nếu: shiftEnd > nightStart HOẶC shiftStart < nightEnd
            return shiftEnd.isAfter(nightStart) || shiftStart.isBefore(nightEnd);
        } else {
            // Night hours trong ngày (hiếm gặp)
            return !(shiftEnd.isBefore(nightStart) || shiftEnd.equals(nightStart))
                    && !(shiftStart.isAfter(nightEnd) || shiftStart.equals(nightEnd));
        }
    }

    @Override
    public int calculateWorkingMinutesForOvernightShift(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0;
        }

        // Nếu checkOut trước checkIn, không hợp lệ
        if (checkOut.isBefore(checkIn)) {
            return 0;
        }

        Duration duration = Duration.between(checkIn, checkOut);
        return (int) duration.toMinutes();
    }

    @Override
    public int calculateBreakMinutes(LocalDateTime breakStart, LocalDateTime breakEnd) {
        if (breakStart == null || breakEnd == null) {
            return 0;
        }

        // Nếu breakEnd trước breakStart, không hợp lệ
        if (breakEnd.isBefore(breakStart)) {
            return 0;
        }

        Duration duration = Duration.between(breakStart, breakEnd);
        return (int) duration.toMinutes();
    }

    /**
     * Lấy effective minimum break dựa trên config và legal requirements
     */
    private int getEffectiveMinimumBreak(BreakConfig config, int workingHours, boolean isNightShift) {
        int configMinimum;

        if (isNightShift && config.getNightShiftMinimumBreakMinutes() != null) {
            configMinimum = config.getNightShiftMinimumBreakMinutes();
        } else {
            configMinimum = config.getMinimumBreakMinutes() != null
                    ? config.getMinimumBreakMinutes()
                    : 0;
        }

        // Nếu useLegalMinimum = true, lấy max của (config minimum, legal minimum)
        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            String locale = config.getLocale() != null ? config.getLocale() : "ja";
            int legalMinimum = legalBreakRequirements.getMinimumBreak(locale, workingHours, isNightShift);
            return Math.max(configMinimum, legalMinimum);
        }

        return configMinimum;
    }
}
