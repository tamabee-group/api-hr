package com.tamabee.api_hr.service.calculator.impl;

import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.OvertimeMultipliers;
import com.tamabee.api_hr.dto.result.DailyOvertimeDetail;
import com.tamabee.api_hr.dto.result.OvertimeResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.service.calculator.interfaces.IOvertimeCalculator;
import com.tamabee.api_hr.service.calculator.LegalOvertimeRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Calculator tính toán tăng ca
 * Hỗ trợ: regular, night, holiday, weekend overtime
 * Áp dụng multiplier rates và cap tại maximum limits
 * Tích hợp break và night shift calculation
 */
@Component
@RequiredArgsConstructor
public class OvertimeCalculatorImpl implements IOvertimeCalculator {

    private static final int MINUTES_PER_HOUR = 60;

    private final LegalOvertimeRequirements legalOvertimeRequirements;

    @Override
    public OvertimeResult calculateOvertime(
            List<DailyOvertimeDetail> dailyDetails,
            OvertimeConfig config,
            BigDecimal hourlyRate) {

        if (dailyDetails == null || dailyDetails.isEmpty() || config == null) {
            return OvertimeResult.builder().build();
        }

        if (!Boolean.TRUE.equals(config.getOvertimeEnabled())) {
            return OvertimeResult.builder().build();
        }

        int totalRegularMinutes = 0;
        int totalNightMinutes = 0;
        int totalHolidayMinutes = 0;
        int totalWeekendMinutes = 0;

        int maxDailyMinutes = config.getMaxOvertimeHoursPerDay() != null
                ? config.getMaxOvertimeHoursPerDay() * MINUTES_PER_HOUR
                : Integer.MAX_VALUE;

        int maxMonthlyMinutes = config.getMaxOvertimeHoursPerMonth() != null
                ? config.getMaxOvertimeHoursPerMonth() * MINUTES_PER_HOUR
                : Integer.MAX_VALUE;

        int accumulatedMinutes = 0;

        for (DailyOvertimeDetail detail : dailyDetails) {
            if (detail == null)
                continue;

            int dailyRegular = detail.getRegularMinutes() != null ? detail.getRegularMinutes() : 0;
            int dailyNight = detail.getNightMinutes() != null ? detail.getNightMinutes() : 0;
            int dailyTotal = dailyRegular + dailyNight;

            // Áp dụng cap theo ngày
            if (dailyTotal > maxDailyMinutes) {
                double ratio = (double) maxDailyMinutes / dailyTotal;
                dailyRegular = (int) Math.floor(dailyRegular * ratio);
                dailyNight = maxDailyMinutes - dailyRegular; // Đảm bảo tổng = maxDailyMinutes
                dailyTotal = maxDailyMinutes;
            }

            // Kiểm tra cap theo tháng
            if (accumulatedMinutes + dailyTotal > maxMonthlyMinutes) {
                int remaining = maxMonthlyMinutes - accumulatedMinutes;
                if (remaining <= 0)
                    break;

                double ratio = (double) remaining / dailyTotal;
                dailyRegular = (int) Math.floor(dailyRegular * ratio);
                dailyNight = remaining - dailyRegular; // Đảm bảo tổng = remaining
                dailyTotal = remaining;
            }

            accumulatedMinutes += dailyTotal;

            // Phân loại theo ngày lễ/cuối tuần
            if (Boolean.TRUE.equals(detail.getIsHoliday())) {
                totalHolidayMinutes += dailyTotal;
            } else if (Boolean.TRUE.equals(detail.getIsWeekend())) {
                totalWeekendMinutes += dailyTotal;
            } else {
                totalRegularMinutes += dailyRegular;
                totalNightMinutes += dailyNight;
            }
        }

        int totalMinutes = totalRegularMinutes + totalNightMinutes + totalHolidayMinutes + totalWeekendMinutes;

        // Tính tiền tăng ca
        BigDecimal safeHourlyRate = hourlyRate != null ? hourlyRate : BigDecimal.ZERO;
        BigDecimal minuteRate = safeHourlyRate.divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 4, RoundingMode.HALF_UP);

        // Tính tiền tăng ca và làm tròn từng loại
        BigDecimal regularPay = calculatePay(totalRegularMinutes, minuteRate, config.getRegularOvertimeRate())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal nightPay = calculatePay(totalNightMinutes, minuteRate, config.getNightOvertimeRate())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal holidayPay = calculatePay(totalHolidayMinutes, minuteRate, config.getHolidayOvertimeRate())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal weekendPay = calculatePay(totalWeekendMinutes, minuteRate, config.getWeekendOvertimeRate())
                .setScale(0, RoundingMode.HALF_UP);

        // Tổng tiền = tổng các loại đã làm tròn (đảm bảo sum invariant)
        BigDecimal totalPay = regularPay.add(nightPay).add(holidayPay).add(weekendPay);

        return OvertimeResult.builder()
                .regularOvertimeMinutes(totalRegularMinutes)
                .nightOvertimeMinutes(totalNightMinutes)
                .holidayOvertimeMinutes(totalHolidayMinutes)
                .weekendOvertimeMinutes(totalWeekendMinutes)
                .totalOvertimeMinutes(totalMinutes)
                .regularOvertimeAmount(regularPay)
                .nightOvertimeAmount(nightPay)
                .holidayOvertimeAmount(holidayPay)
                .weekendOvertimeAmount(weekendPay)
                .totalOvertimeAmount(totalPay)
                .build();
    }

    @Override
    public int calculateNightOvertimeMinutes(
            int totalOvertimeMinutes,
            int checkInHour,
            int checkOutHour,
            OvertimeConfig config) {

        if (totalOvertimeMinutes <= 0 || config == null) {
            return 0;
        }

        LocalTime nightStart = config.getNightStartTime() != null
                ? config.getNightStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightEndTime() != null
                ? config.getNightEndTime()
                : LocalTime.of(5, 0);

        int nightStartHour = nightStart.getHour();
        int nightEndHour = nightEnd.getHour();

        // Tính số giờ làm đêm dựa trên check-in/check-out
        int nightHours = 0;

        // Trường hợp làm qua đêm (22:00 - 05:00)
        if (nightStartHour > nightEndHour) {
            // Đếm giờ từ nightStart đến 24:00
            if (checkOutHour >= nightStartHour || checkOutHour <= nightEndHour) {
                if (checkOutHour >= nightStartHour) {
                    nightHours = checkOutHour - nightStartHour;
                } else {
                    nightHours = (24 - nightStartHour) + checkOutHour;
                }
            }
        } else {
            // Trường hợp bình thường
            if (checkOutHour > nightStartHour && checkInHour < nightEndHour) {
                int start = Math.max(checkInHour, nightStartHour);
                int end = Math.min(checkOutHour, nightEndHour);
                nightHours = Math.max(0, end - start);
            }
        }

        int nightMinutes = nightHours * MINUTES_PER_HOUR;
        return Math.min(nightMinutes, totalOvertimeMinutes);
    }

    /**
     * Tính tiền tăng ca = số phút × rate/phút × multiplier
     */
    private BigDecimal calculatePay(int minutes, BigDecimal minuteRate, BigDecimal multiplier) {
        if (minutes <= 0 || minuteRate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeMultiplier = multiplier != null ? multiplier : BigDecimal.ONE;
        return minuteRate
                .multiply(BigDecimal.valueOf(minutes))
                .multiply(safeMultiplier);
    }

    @Override
    public int calculateNightMinutes(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            OvertimeConfig config) {

        if (checkIn == null || checkOut == null || config == null) {
            return 0;
        }

        LocalTime nightStart = config.getNightStartTime() != null
                ? config.getNightStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightEndTime() != null
                ? config.getNightEndTime()
                : LocalTime.of(5, 0);

        // Tính tổng số phút làm trong giờ đêm
        int totalNightMinutes = calculateNightMinutesInRange(checkIn, checkOut, nightStart, nightEnd);

        // Trừ break minutes nằm trong giờ đêm
        if (breakRecords != null && !breakRecords.isEmpty()) {
            for (BreakRecordEntity breakRecord : breakRecords) {
                if (breakRecord != null && breakRecord.getBreakStart() != null && breakRecord.getBreakEnd() != null) {
                    int breakNightMinutes = calculateNightMinutesInRange(
                            breakRecord.getBreakStart(),
                            breakRecord.getBreakEnd(),
                            nightStart,
                            nightEnd);
                    totalNightMinutes -= breakNightMinutes;
                }
            }
        }

        return Math.max(0, totalNightMinutes);
    }

    @Override
    public OvertimeMultipliers getLegalMinimumMultipliers(String locale) {
        return legalOvertimeRequirements.getMinimumMultipliers(locale);
    }

    @Override
    public boolean validateMultipliers(OvertimeConfig config) {
        if (config == null) {
            return false;
        }

        String locale = config.getLocale() != null ? config.getLocale() : "ja";
        OvertimeMultipliers minimum = legalOvertimeRequirements.getMinimumMultipliers(locale);

        // Kiểm tra từng multiplier
        return isMultiplierValid(config.getRegularOvertimeRate(), minimum.getRegularOvertime())
                && isMultiplierValid(config.getNightWorkRate(), minimum.getNightWork())
                && isMultiplierValid(config.getNightOvertimeRate(), minimum.getNightOvertime())
                && isMultiplierValid(config.getHolidayOvertimeRate(), minimum.getHolidayOvertime())
                && isMultiplierValid(config.getHolidayNightOvertimeRate(), minimum.getHolidayNightOvertime())
                && isMultiplierValid(config.getWeekendOvertimeRate(), minimum.getWeekendOvertime());
    }

    /**
     * Kiểm tra multiplier có >= minimum không
     */
    private boolean isMultiplierValid(BigDecimal multiplier, BigDecimal minimum) {
        if (multiplier == null || minimum == null) {
            return true; // Nếu không set, sử dụng default
        }
        return multiplier.compareTo(minimum) >= 0;
    }

    /**
     * Tính số phút làm trong khoảng giờ đêm
     */
    private int calculateNightMinutesInRange(
            LocalDateTime start,
            LocalDateTime end,
            LocalTime nightStart,
            LocalTime nightEnd) {

        if (start == null || end == null) {
            return 0;
        }

        // Xử lý trường hợp overnight (qua đêm)
        LocalDateTime adjustedEnd = end;
        if (end.isBefore(start)) {
            adjustedEnd = end.plusDays(1);
        }

        int totalNightMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(adjustedEnd)) {
            LocalTime currentTime = current.toLocalTime();
            LocalDateTime nextMidnight = current.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime endOfDay = adjustedEnd.isBefore(nextMidnight) ? adjustedEnd : nextMidnight;

            // Tính night minutes trong ngày hiện tại
            if (nightStart.isAfter(nightEnd)) {
                // Night hours qua đêm (22:00 - 05:00)
                // Phần 1: 22:00 - 24:00
                if (currentTime.isBefore(LocalTime.MIDNIGHT) || currentTime.equals(LocalTime.MIDNIGHT)) {
                    LocalTime dayEnd = endOfDay.toLocalTime();
                    if (dayEnd.equals(LocalTime.MIDNIGHT)) {
                        dayEnd = LocalTime.MAX;
                    }

                    if (currentTime.isBefore(nightStart) && dayEnd.isAfter(nightStart)) {
                        // Overlap với 22:00-24:00
                        LocalTime overlapStart = nightStart;
                        LocalTime overlapEnd = dayEnd.isAfter(LocalTime.MAX) ? LocalTime.MAX : dayEnd;
                        totalNightMinutes += (int) Duration.between(overlapStart, overlapEnd).toMinutes();
                    } else if (!currentTime.isBefore(nightStart)) {
                        // Đang trong khoảng 22:00-24:00
                        LocalTime overlapEnd = dayEnd.isAfter(LocalTime.MAX) ? LocalTime.MAX : dayEnd;
                        totalNightMinutes += (int) Duration.between(currentTime, overlapEnd).toMinutes();
                    }
                }

                // Phần 2: 00:00 - 05:00
                if (currentTime.isBefore(nightEnd)) {
                    LocalTime dayEnd = endOfDay.toLocalTime();
                    LocalTime overlapEnd = dayEnd.isBefore(nightEnd) ? dayEnd : nightEnd;
                    if (overlapEnd.isAfter(currentTime)) {
                        totalNightMinutes += (int) Duration.between(currentTime, overlapEnd).toMinutes();
                    }
                }
            } else {
                // Night hours trong ngày (hiếm gặp)
                LocalTime dayEnd = endOfDay.toLocalTime();
                if (currentTime.isBefore(nightEnd) && dayEnd.isAfter(nightStart)) {
                    LocalTime overlapStart = currentTime.isAfter(nightStart) ? currentTime : nightStart;
                    LocalTime overlapEnd = dayEnd.isBefore(nightEnd) ? dayEnd : nightEnd;
                    if (overlapEnd.isAfter(overlapStart)) {
                        totalNightMinutes += (int) Duration.between(overlapStart, overlapEnd).toMinutes();
                    }
                }
            }

            current = nextMidnight;
        }

        return Math.max(0, totalNightMinutes);
    }
}
