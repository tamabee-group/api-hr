package com.tamabee.api_hr.service.calculator.interfaces;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;

/**
 * Interface cho việc tính toán giờ giải lao.
 * Logic đơn giản: thời gian giải lao luôn bị trừ khỏi giờ làm việc, không tính lương.
 */
public interface IBreakCalculator {

    /**
     * Tính tổng thời gian giải lao từ danh sách break records
     */
    int calculateTotalBreakMinutes(List<BreakRecordEntity> breakRecords);

    /**
     * Tính thời gian giải lao hiệu lực
     */
    int calculateEffectiveBreakMinutes(
            int actualBreakMinutes,
            BreakConfig config,
            int workingHours,
            boolean isNightShift);

    /**
     * Tính working hours sau khi trừ break (luôn trừ)
     */
    int calculateNetWorkingMinutes(
            int grossWorkingMinutes,
            int breakMinutes,
            BreakConfig config);

    /**
     * Kiểm tra xem shift có phải là night shift không
     */
    boolean isNightShift(LocalTime shiftStart, LocalTime shiftEnd, BreakConfig config);

    /**
     * Tính working minutes cho shift qua đêm
     */
    int calculateWorkingMinutesForOvernightShift(LocalDateTime checkIn, LocalDateTime checkOut);

    /**
     * Tính số phút giải lao từ thời gian bắt đầu và kết thúc
     */
    int calculateBreakMinutes(LocalDateTime breakStart, LocalDateTime breakEnd);
}
