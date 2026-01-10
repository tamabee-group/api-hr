package com.tamabee.api_hr.service.calculator.impl;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.result.WorkingHoursResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.service.calculator.interfaces.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.interfaces.IWorkingHoursCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Calculator tính toán giờ làm việc
 * Tích hợp BreakCalculator để tính working hours có tính đến break policy
 * Hỗ trợ overnight shift (qua đêm)
 */
@Component
@RequiredArgsConstructor
public class WorkingHoursCalculatorImpl implements IWorkingHoursCalculator {

    private final IBreakCalculator breakCalculator;

    @Override
    public WorkingHoursResult calculateWorkingHours(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            BreakConfig breakConfig,
            WorkScheduleData schedule) {

        if (checkIn == null || checkOut == null) {
            return WorkingHoursResult.builder().build();
        }

        // Kiểm tra xem có phải overnight shift không
        boolean isOvernight = isOvernightShift(checkIn.toLocalTime(), checkOut.toLocalTime());
        if (isOvernight && checkOut.isBefore(checkIn)) {
            return calculateOvernightWorkingHours(checkIn, checkOut, breakRecords, breakConfig, schedule);
        }

        // Tính gross working minutes
        int grossMinutes = (int) Duration.between(checkIn, checkOut).toMinutes();
        if (grossMinutes < 0) {
            grossMinutes = 0;
        }

        // Tính working hours (giờ)
        int workingHours = grossMinutes / 60;

        // Kiểm tra night shift
        boolean isNightShift = breakConfig != null &&
                breakCalculator.isNightShift(checkIn.toLocalTime(), checkOut.toLocalTime(), breakConfig);

        // Tính break minutes
        int totalBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);

        // Nếu không có break records và break tracking disabled, sử dụng default
        if (totalBreakMinutes == 0 && breakConfig != null &&
                !Boolean.TRUE.equals(breakConfig.getBreakTrackingEnabled())) {
            if (isNightShift && breakConfig.getNightShiftDefaultBreakMinutes() != null) {
                totalBreakMinutes = breakConfig.getNightShiftDefaultBreakMinutes();
            } else if (breakConfig.getDefaultBreakMinutes() != null) {
                totalBreakMinutes = breakConfig.getDefaultBreakMinutes();
            }
        }

        // Tính effective break minutes
        int effectiveBreakMinutes = breakConfig != null
                ? breakCalculator.calculateEffectiveBreakMinutes(
                        totalBreakMinutes, breakConfig, workingHours, isNightShift)
                : totalBreakMinutes;

        // Kiểm tra break compliance
        boolean breakCompliant = checkBreakCompliance(
                totalBreakMinutes, breakConfig, workingHours, isNightShift);

        // Tính net working minutes
        int netMinutes = breakConfig != null
                ? breakCalculator.calculateNetWorkingMinutes(grossMinutes, effectiveBreakMinutes, breakConfig)
                : grossMinutes;

        // Tính night minutes và regular minutes
        int nightMinutes = 0;
        int regularMinutes = netMinutes;
        if (breakConfig != null) {
            nightMinutes = calculateNightMinutes(checkIn, checkOut, breakConfig);
            regularMinutes = Math.max(0, netMinutes - nightMinutes);
        }

        BreakType breakType = breakConfig != null ? breakConfig.getBreakType() : null;

        return WorkingHoursResult.builder()
                .grossWorkingMinutes(grossMinutes)
                .netWorkingMinutes(netMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .effectiveBreakMinutes(effectiveBreakMinutes)
                .breakType(breakType)
                .breakCompliant(breakCompliant)
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
            BreakConfig breakConfig,
            WorkScheduleData schedule) {

        if (checkIn == null || checkOut == null) {
            return WorkingHoursResult.builder().build();
        }

        // Với overnight shift, checkOut có thể là ngày hôm sau
        // Nếu checkOut trước checkIn về mặt thời gian, cộng thêm 1 ngày
        LocalDateTime adjustedCheckOut = checkOut;
        if (checkOut.isBefore(checkIn)) {
            adjustedCheckOut = checkOut.plusDays(1);
        }

        // Tính gross working minutes
        int grossMinutes = (int) Duration.between(checkIn, adjustedCheckOut).toMinutes();
        if (grossMinutes < 0) {
            grossMinutes = 0;
        }

        int workingHours = grossMinutes / 60;

        // Overnight shift luôn là night shift
        boolean isNightShift = true;

        // Tính break minutes
        int totalBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);

        // Nếu không có break records và break tracking disabled, sử dụng default
        if (totalBreakMinutes == 0 && breakConfig != null &&
                !Boolean.TRUE.equals(breakConfig.getBreakTrackingEnabled())) {
            if (breakConfig.getNightShiftDefaultBreakMinutes() != null) {
                totalBreakMinutes = breakConfig.getNightShiftDefaultBreakMinutes();
            } else if (breakConfig.getDefaultBreakMinutes() != null) {
                totalBreakMinutes = breakConfig.getDefaultBreakMinutes();
            }
        }

        // Tính effective break minutes
        int effectiveBreakMinutes = breakConfig != null
                ? breakCalculator.calculateEffectiveBreakMinutes(
                        totalBreakMinutes, breakConfig, workingHours, isNightShift)
                : totalBreakMinutes;

        // Kiểm tra break compliance
        boolean breakCompliant = checkBreakCompliance(
                totalBreakMinutes, breakConfig, workingHours, isNightShift);

        // Tính net working minutes
        int netMinutes = breakConfig != null
                ? breakCalculator.calculateNetWorkingMinutes(grossMinutes, effectiveBreakMinutes, breakConfig)
                : grossMinutes;

        // Tính night minutes cho overnight shift
        int nightMinutes = 0;
        int regularMinutes = netMinutes;
        if (breakConfig != null) {
            nightMinutes = calculateNightMinutesForOvernight(checkIn, adjustedCheckOut, breakConfig);
            regularMinutes = Math.max(0, netMinutes - nightMinutes);
        }

        BreakType breakType = breakConfig != null ? breakConfig.getBreakType() : null;

        return WorkingHoursResult.builder()
                .grossWorkingMinutes(grossMinutes)
                .netWorkingMinutes(netMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .effectiveBreakMinutes(effectiveBreakMinutes)
                .breakType(breakType)
                .breakCompliant(breakCompliant)
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
        // Overnight shift: start time > end time (ví dụ 17:00 - 07:00)
        return startTime.isAfter(endTime);
    }

    /**
     * Kiểm tra xem break có tuân thủ minimum không
     */
    private boolean checkBreakCompliance(
            int actualBreakMinutes,
            BreakConfig config,
            int workingHours,
            boolean isNightShift) {

        if (config == null || !Boolean.TRUE.equals(config.getBreakEnabled())) {
            return true;
        }

        // Lấy minimum break
        int minimumBreak = getEffectiveMinimumBreak(config, workingHours, isNightShift);

        return actualBreakMinutes >= minimumBreak;
    }

    /**
     * Lấy effective minimum break
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

        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            String locale = config.getLocale() != null ? config.getLocale() : "ja";
            int legalMinimum = breakCalculator.getLegalMinimumBreak(locale, workingHours, isNightShift);
            return Math.max(configMinimum, legalMinimum);
        }

        return configMinimum;
    }

    /**
     * Tính số phút làm trong giờ đêm (22:00-05:00)
     */
    private int calculateNightMinutes(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            BreakConfig config) {

        LocalTime nightStart = config.getNightShiftStartTime() != null
                ? config.getNightShiftStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightShiftEndTime() != null
                ? config.getNightShiftEndTime()
                : LocalTime.of(5, 0);

        LocalTime startTime = checkIn.toLocalTime();
        LocalTime endTime = checkOut.toLocalTime();

        // Trường hợp đơn giản: shift trong ngày
        if (!isOvernightShift(startTime, endTime)) {
            // Kiểm tra overlap với night hours
            if (nightStart.isAfter(nightEnd)) {
                // Night hours qua đêm (22:00 - 05:00)
                // Shift trong ngày chỉ có thể overlap với phần 22:00-24:00 hoặc 00:00-05:00
                int nightMinutes = 0;

                // Overlap với 22:00-24:00
                if (endTime.isAfter(nightStart)) {
                    LocalTime overlapStart = startTime.isAfter(nightStart) ? startTime : nightStart;
                    LocalTime overlapEnd = endTime;
                    nightMinutes += (int) Duration.between(overlapStart, overlapEnd).toMinutes();
                }

                // Overlap với 00:00-05:00
                if (startTime.isBefore(nightEnd)) {
                    LocalTime overlapStart = startTime;
                    LocalTime overlapEnd = endTime.isBefore(nightEnd) ? endTime : nightEnd;
                    nightMinutes += (int) Duration.between(overlapStart, overlapEnd).toMinutes();
                }

                return Math.max(0, nightMinutes);
            }
        }

        return 0;
    }

    /**
     * Tính số phút làm trong giờ đêm cho overnight shift
     */
    private int calculateNightMinutesForOvernight(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            BreakConfig config) {

        LocalTime nightStart = config.getNightShiftStartTime() != null
                ? config.getNightShiftStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightShiftEndTime() != null
                ? config.getNightShiftEndTime()
                : LocalTime.of(5, 0);

        int nightMinutes = 0;

        // Overnight shift: ví dụ 17:00 ngày 1 đến 07:00 ngày 2
        // Night hours: 22:00 - 05:00

        LocalTime startTime = checkIn.toLocalTime();
        LocalTime endTime = checkOut.toLocalTime();

        // Phần 1: Từ checkIn đến 24:00 ngày 1
        // Tính overlap với 22:00-24:00
        if (startTime.isBefore(LocalTime.MIDNIGHT) || startTime.equals(LocalTime.MIDNIGHT)) {
            if (startTime.isBefore(nightStart)) {
                // Bắt đầu trước 22:00, tính từ 22:00 đến 24:00
                nightMinutes += (int) Duration.between(nightStart, LocalTime.MAX).toMinutes() + 1;
            } else {
                // Bắt đầu sau 22:00, tính từ startTime đến 24:00
                nightMinutes += (int) Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
            }
        }

        // Phần 2: Từ 00:00 đến checkOut ngày 2
        // Tính overlap với 00:00-05:00
        if (endTime.isAfter(LocalTime.MIDNIGHT) || endTime.equals(LocalTime.MIDNIGHT)) {
            if (endTime.isAfter(nightEnd)) {
                // Kết thúc sau 05:00, tính từ 00:00 đến 05:00
                nightMinutes += (int) Duration.between(LocalTime.MIDNIGHT, nightEnd).toMinutes();
            } else {
                // Kết thúc trước 05:00, tính từ 00:00 đến endTime
                nightMinutes += (int) Duration.between(LocalTime.MIDNIGHT, endTime).toMinutes();
            }
        }

        return Math.max(0, nightMinutes);
    }
}
