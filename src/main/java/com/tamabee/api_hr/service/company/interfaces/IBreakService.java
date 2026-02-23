package com.tamabee.api_hr.service.company.interfaces;

import java.time.LocalDate;
import java.util.List;

import com.tamabee.api_hr.dto.request.attendance.EndBreakRequest;
import com.tamabee.api_hr.dto.request.attendance.StartBreakRequest;
import com.tamabee.api_hr.dto.response.attendance.BreakRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.BreakSummaryResponse;

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
     * @param request    thông tin bắt đầu giải lao
     * @return bản ghi giải lao
     */
    BreakRecordResponse startBreak(Long employeeId, StartBreakRequest request);

    /**
     * Kết thúc giờ giải lao
     *
     * @param employeeId    ID nhân viên
     * @param breakRecordId ID bản ghi giải lao
     * @param request       thông tin kết thúc giải lao (location)
     * @return bản ghi giải lao đã cập nhật
     */
    BreakRecordResponse endBreak(Long employeeId, Long breakRecordId, EndBreakRequest request);

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
     * @param breakMinutes số phút giải lao
     */
    void validateBreakDuration(Integer breakMinutes);

    /**
     * Lấy thời gian giải lao tối thiểu theo quy định pháp luật
     *
     * @param region       region code (ja, vi)
     * @param workingHours số giờ làm việc
     * @return số phút giải lao tối thiểu
     */
    Integer getLegalMinimumBreak(String region, Integer workingHours);

    /**
     * Lấy thời gian giải lao tối thiểu hiệu lực (max của legal và company config)
     *
     * @param workingHours số giờ làm việc
     * @return số phút giải lao tối thiểu hiệu lực
     */
    Integer getEffectiveMinimumBreak(Integer workingHours);

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
