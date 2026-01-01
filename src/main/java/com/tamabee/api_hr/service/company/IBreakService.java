package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.BreakRecordResponse;
import com.tamabee.api_hr.dto.response.BreakSummaryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Service quản lý giờ giải lao của nhân viên.
 * Hỗ trợ ghi nhận break start/end, validation, và tính toán legal minimum.
 */
public interface IBreakService {

    // ==================== Break Recording ====================

    /**
     * Bắt đầu giờ giải lao
     *
     * @param employeeId ID nhân viên
     * @param companyId  ID công ty
     * @param request    thông tin bắt đầu giải lao
     * @return bản ghi giải lao
     */
    BreakRecordResponse startBreak(Long employeeId, Long companyId, StartBreakRequest request);

    /**
     * Kết thúc giờ giải lao
     *
     * @param employeeId    ID nhân viên
     * @param breakRecordId ID bản ghi giải lao
     * @return bản ghi giải lao đã cập nhật
     */
    BreakRecordResponse endBreak(Long employeeId, Long breakRecordId);

    // ==================== Query Operations ====================

    /**
     * Lấy danh sách bản ghi giải lao theo bản ghi chấm công
     *
     * @param attendanceRecordId ID bản ghi chấm công
     * @return danh sách bản ghi giải lao
     */
    List<BreakRecordResponse> getBreakRecordsByAttendance(Long attendanceRecordId);

    /**
     * Lấy tổng hợp giờ giải lao của nhân viên theo ngày
     *
     * @param employeeId ID nhân viên
     * @param date       ngày làm việc
     * @return tổng hợp giờ giải lao
     */
    BreakSummaryResponse getBreakSummary(Long employeeId, LocalDate date);

    // ==================== Validation ====================

    /**
     * Validate thời gian giải lao theo cấu hình công ty
     *
     * @param companyId    ID công ty
     * @param breakMinutes số phút giải lao
     */
    void validateBreakDuration(Long companyId, Integer breakMinutes);

    /**
     * Lấy thời gian giải lao tối thiểu theo quy định pháp luật
     *
     * @param locale       locale code (ja, vi, en)
     * @param workingHours số giờ làm việc
     * @return số phút giải lao tối thiểu
     */
    Integer getLegalMinimumBreak(String locale, Integer workingHours);

    /**
     * Lấy thời gian giải lao tối thiểu hiệu lực (max của legal và company config)
     *
     * @param companyId    ID công ty
     * @param workingHours số giờ làm việc
     * @return số phút giải lao tối thiểu hiệu lực
     */
    Integer getEffectiveMinimumBreak(Long companyId, Integer workingHours);

    // ==================== Calculation ====================

    /**
     * Tính tổng thời gian giải lao từ tất cả break sessions của một attendance
     * record
     *
     * @param attendanceRecordId ID bản ghi chấm công
     * @return tổng số phút giải lao (actualBreakMinutes)
     */
    Integer calculateTotalBreakMinutes(Long attendanceRecordId);
}
