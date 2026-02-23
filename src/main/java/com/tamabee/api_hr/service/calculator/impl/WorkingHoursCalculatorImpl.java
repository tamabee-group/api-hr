package com.tamabee.api_hr.service.calculator.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.result.WorkingHoursResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.service.calculator.interfaces.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.interfaces.IWorkingHoursCalculator;

import lombok.RequiredArgsConstructor;

/**
 * Calculator tính toán giờ làm việc.
 * Tích hợp BreakCalculator để tính working hours có tính đến break.
 * Hỗ trợ overnight shift (qua đêm).
 * Logic đơn giản: thời gian giải lao luôn bị trừ khỏi giờ làm việc.
 */
@Component
@RequiredArgsConstructor
public class WorkingHoursCalculatorImpl implements IWorkingHoursCalculator {

    private final IBreakCalculator breakCalculator;

    // Giờ đêm mặc định: 22:00 - 05:00
    private static final LocalTime DEFAULT_NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_NIGHT_END = LocalTime.of(5, 0);

    @Override
    public WorkingHoursResult calculateWorkingHours(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            BreakConfig breakConfig) {

        if (checkIn == null || checkOut == null) {
            return WorkingHoursResult.builder().build();
        }

        // Kiểm tra xem có phải overnight shift không
        boolean isOvernight = isOvernightShift(checkIn.toLocalTime(), checkOut.toLocalTime());
        if (isOvernight && checkOut.isBefore(checkIn)) {
            return calculateOvernightWorkingHours(checkIn, checkOut, breakRecords, breakConfig);
        }

        // Tính gross working minutes
        int grossMinutes = (int) Duration.between(checkIn, checkOut).toMinutes();
        if (grossMinutes < 0) {
            grossMinutes = 0;
        }

        int workingHours = grossMinutes / 60;

        // Kiểm tra night shift
        boolean isNightShift = breakConfig != null &&
                breakCalculator.isNightShift(checkIn.toLocalTime(), checkOut.toLocalTime(), breakConfig);

        // Tính break minutes
        int totalBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);

        // Tính effective break minutes
        int effectiveBreakMinutes = breakConfig != null
                ? breakCalculator.calculateEffectiveBreakMinutes(
                        totalBreakMinutes, breakConfig, workingHours, isNightShift)
                : totalBreakMinutes;

        // Tính net working minutes (luôn trừ break)
        int netMinutes = breakConfig != null
                ? breakCalculator.calculateNetWorkingMinutes(grossMinutes, effectiveBreakMinutes, breakConfig)
                : grossMinutes;

        // Tính night minutes và regular minutes
        int nightMinutes = calculateNightMinutes(checkIn, checkOut);
        int regularMinutes = Math.max(0, netMinutes - nightMinutes);

        return WorkingHoursResult.builder()
                .grossWorkingMinutes(grossMinutes)
                .netWorkingMinutes(netMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .effectiveBreakMinutes(effectiveBreakMinutes)
                .isNightShift(isNightShift)
                .isOvernightShift(isOvernight)
                .nightMinutes(nightMinutes)
                .regularMinutes(regularMinutes)
                .build();
    }

    @Override
    public WorkingHoursResult calculateOvernightWorkingHours(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            BreakConfig breakConfig) {

        if (checkIn == null || checkOut == null) {
            return WorkingHoursResult.builder().build();
        }

        // Với overnight shift, checkOut có thể là ngày hôm sau
        LocalDateTime adjustedCheckOut = checkOut;
        if (checkOut.isBefore(checkIn)) {
            adjustedCheckOut = checkOut.plusDays(1);
        }

        int grossMinutes = (int) Duration.between(checkIn, adjustedCheckOut).toMinutes();
        if (grossMinutes < 0) {
            grossMinutes = 0;
        }

        int workingHours = grossMinutes / 60;

        // Overnight shift luôn là night shift
        boolean isNightShift = true;

        // Tính break minutes
        int totalBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);

        int effectiveBreakMinutes = breakConfig != null
                ? breakCalculator.calculateEffectiveBreakMinutes(
                        totalBreakMinutes, breakConfig, workingHours, isNightShift)
                : totalBreakMinutes;

        // Tính net working minutes (luôn trừ break)
        int netMinutes = breakConfig != null
                ? breakCalculator.calculateNetWorkingMinutes(grossMinutes, effectiveBreakMinutes, breakConfig)
                : grossMinutes;

        // Tính night minutes cho overnight shift
        int nightMinutes = calculateNightMinutesForOvernight(checkIn, adjustedCheckOut);
        int regularMinutes = Math.max(0, netMinutes - nightMinutes);

        return WorkingHoursResult.builder()
                .grossWorkingMinutes(grossMinutes)
                .netWorkingMinutes(netMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .effectiveBreakMinutes(effectiveBreakMinutes)
                .isNightShift(isNightShift)
                .isOvernightShift(true)
                .nightMinutes(nightMinutes)
                .regularMinutes(regularMinutes)
                .build();
    }

    @Override
    public boolean isOvernightShift(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return startTime.isAfter(endTime);
    }

    /**
     * Tính số phút làm trong giờ đêm (22:00-05:00)
     */
    private int calculateNightMinutes(LocalDateTime checkIn, LocalDateTime checkOut) {
        LocalTime startTime = checkIn.toLocalTime();
        LocalTime endTime = checkOut.toLocalTime();

        // Trường hợp đơn giản: shift trong ngày
        if (!isOvernightShift(startTime, endTime)) {
            int nightMinutes = 0;

            // Overlap với 22:00-24:00
            if (endTime.isAfter(DEFAULT_NIGHT_START)) {
                LocalTime overlapStart = startTime.isAfter(DEFAULT_NIGHT_START) ? startTime : DEFAULT_NIGHT_START;
                nightMinutes += (int) Duration.between(overlapStart, endTime).toMinutes();
            }

            // Overlap với 00:00-05:00
            if (startTime.isBefore(DEFAULT_NIGHT_END)) {
                LocalTime overlapEnd = endTime.isBefore(DEFAULT_NIGHT_END) ? endTime : DEFAULT_NIGHT_END;
                nightMinutes += (int) Duration.between(startTime, overlapEnd).toMinutes();
            }

            return Math.max(0, nightMinutes);
        }

        return 0;
    }

    /**
     * Tính số phút làm trong giờ đêm cho overnight shift
     */
    private int calculateNightMinutesForOvernight(
            LocalDateTime checkIn,
            LocalDateTime checkOut) {

        int nightMinutes = 0;
        LocalTime startTime = checkIn.toLocalTime();
        LocalTime endTime = checkOut.toLocalTime();

        // Phần 1: Từ checkIn đến 24:00 ngày 1 — overlap với 22:00-24:00
        if (startTime.isBefore(LocalTime.MIDNIGHT) || startTime.equals(LocalTime.MIDNIGHT)) {
            if (startTime.isBefore(DEFAULT_NIGHT_START)) {
                nightMinutes += (int) Duration.between(DEFAULT_NIGHT_START, LocalTime.MAX).toMinutes() + 1;
            } else {
                nightMinutes += (int) Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
            }
        }

        // Phần 2: Từ 00:00 đến checkOut ngày 2 — overlap với 00:00-05:00
        if (endTime.isAfter(LocalTime.MIDNIGHT) || endTime.equals(LocalTime.MIDNIGHT)) {
            if (endTime.isAfter(DEFAULT_NIGHT_END)) {
                nightMinutes += (int) Duration.between(LocalTime.MIDNIGHT, DEFAULT_NIGHT_END).toMinutes();
            } else {
                nightMinutes += (int) Duration.between(LocalTime.MIDNIGHT, endTime).toMinutes();
            }
        }

        return Math.max(0, nightMinutes);
    }
}
