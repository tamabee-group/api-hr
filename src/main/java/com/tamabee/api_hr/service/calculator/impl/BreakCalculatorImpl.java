package com.tamabee.api_hr.service.calculator.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.service.calculator.interfaces.IBreakCalculator;

import lombok.RequiredArgsConstructor;

/**
 * Calculator tính toán giờ giải lao.
 * Logic đơn giản: thời gian giải lao luôn bị trừ khỏi giờ làm việc, không tính lương.
 */
@Component
@RequiredArgsConstructor
public class BreakCalculatorImpl implements IBreakCalculator {

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

        // Đơn giản: effective = actual, không capping
        return actualBreakMinutes;
    }

    @Override
    public int calculateNetWorkingMinutes(
            int grossWorkingMinutes,
            int breakMinutes,
            BreakConfig config) {

        if (config == null || !Boolean.TRUE.equals(config.getBreakEnabled())) {
            return grossWorkingMinutes;
        }

        // Luôn trừ break khỏi giờ làm việc
        return Math.max(0, grossWorkingMinutes - breakMinutes);
    }

    @Override
    public boolean isNightShift(LocalTime shiftStart, LocalTime shiftEnd, BreakConfig config) {
        if (shiftStart == null || shiftEnd == null) {
            return false;
        }

        // Shift qua đêm (start > end)
        if (shiftStart.isAfter(shiftEnd) || shiftStart.equals(shiftEnd)) {
            return true;
        }

        // Kiểm tra overlap với 22:00 - 05:00
        LocalTime nightStart = LocalTime.of(22, 0);
        LocalTime nightEnd = LocalTime.of(5, 0);

        return shiftEnd.isAfter(nightStart) || shiftStart.isBefore(nightEnd);
    }

    @Override
    public int calculateWorkingMinutesForOvernightShift(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null || checkOut.isBefore(checkIn)) {
            return 0;
        }
        return (int) Duration.between(checkIn, checkOut).toMinutes();
    }

    @Override
    public int calculateBreakMinutes(LocalDateTime breakStart, LocalDateTime breakEnd) {
        if (breakStart == null || breakEnd == null || breakEnd.isBefore(breakStart)) {
            return 0;
        }
        return (int) Duration.between(breakStart, breakEnd).toMinutes();
    }
}
