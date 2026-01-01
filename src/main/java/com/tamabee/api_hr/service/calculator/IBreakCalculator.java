package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Interface cho việc tính toán giờ giải lao
 */
public interface IBreakCalculator {

    /**
     * Tính tổng thời gian giải lao từ danh sách break records
     *
     * @param breakRecords Danh sách bản ghi giải lao
     * @return Tổng số phút giải lao
     */
    int calculateTotalBreakMinutes(List<BreakRecordEntity> breakRecords);

    /**
     * Tính thời gian giải lao hiệu lực (sau khi áp dụng min/max capping)
     *
     * @param actualBreakMinutes Thời gian giải lao thực tế
     * @param config             Cấu hình giải lao
     * @param workingHours       Số giờ làm việc
     * @param isNightShift       Có phải ca đêm không
     * @return Thời gian giải lao hiệu lực (phút)
     */
    int calculateEffectiveBreakMinutes(
            int actualBreakMinutes,
            BreakConfig config,
            int workingHours,
            boolean isNightShift);

    /**
     * Tính working hours sau khi trừ break
     *
     * @param grossWorkingMinutes Tổng số phút làm việc (chưa trừ break)
     * @param breakMinutes        Số phút giải lao
     * @param config              Cấu hình giải lao
     * @return Số phút làm việc thực tế (sau khi trừ break nếu unpaid)
     */
    int calculateNetWorkingMinutes(
            int grossWorkingMinutes,
            int breakMinutes,
            BreakConfig config);

    /**
     * Lấy legal minimum break theo locale
     *
     * @param locale       Locale code (ja, vi, en, ...)
     * @param workingHours Số giờ làm việc
     * @param isNightShift Có phải ca đêm không
     * @return Số phút giải lao tối thiểu theo quy định pháp luật
     */
    int getLegalMinimumBreak(String locale, int workingHours, boolean isNightShift);

    /**
     * Kiểm tra xem shift có phải là night shift không
     *
     * @param shiftStart Giờ bắt đầu ca
     * @param shiftEnd   Giờ kết thúc ca
     * @param config     Cấu hình giải lao
     * @return true nếu là night shift
     */
    boolean isNightShift(LocalTime shiftStart, LocalTime shiftEnd, BreakConfig config);

    /**
     * Tính working minutes cho shift qua đêm
     *
     * @param checkIn  Thời gian check-in
     * @param checkOut Thời gian check-out
     * @return Số phút làm việc
     */
    int calculateWorkingMinutesForOvernightShift(LocalDateTime checkIn, LocalDateTime checkOut);

    /**
     * Tính số phút giải lao từ thời gian bắt đầu và kết thúc
     *
     * @param breakStart Thời gian bắt đầu giải lao
     * @param breakEnd   Thời gian kết thúc giải lao
     * @return Số phút giải lao
     */
    int calculateBreakMinutes(LocalDateTime breakStart, LocalDateTime breakEnd);
}
