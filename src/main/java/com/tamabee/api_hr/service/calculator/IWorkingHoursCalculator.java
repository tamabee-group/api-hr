package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.result.WorkingHoursResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Interface cho việc tính toán giờ làm việc
 */
public interface IWorkingHoursCalculator {

    /**
     * Tính working hours có tính đến break
     *
     * @param checkIn      Thời gian check-in
     * @param checkOut     Thời gian check-out
     * @param breakRecords Danh sách bản ghi giải lao
     * @param breakConfig  Cấu hình giải lao
     * @param schedule     Lịch làm việc
     * @return Kết quả tính toán working hours
     */
    WorkingHoursResult calculateWorkingHours(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            BreakConfig breakConfig,
            WorkScheduleData schedule);

    /**
     * Tính working hours cho overnight shift (qua đêm)
     *
     * @param checkIn      Thời gian check-in
     * @param checkOut     Thời gian check-out
     * @param breakRecords Danh sách bản ghi giải lao
     * @param breakConfig  Cấu hình giải lao
     * @param schedule     Lịch làm việc
     * @return Kết quả tính toán working hours
     */
    WorkingHoursResult calculateOvernightWorkingHours(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BreakRecordEntity> breakRecords,
            BreakConfig breakConfig,
            WorkScheduleData schedule);

    /**
     * Kiểm tra xem shift có qua đêm không
     *
     * @param startTime Giờ bắt đầu ca
     * @param endTime   Giờ kết thúc ca
     * @return true nếu shift qua đêm
     */
    boolean isOvernightShift(LocalTime startTime, LocalTime endTime);
}
