package com.tamabee.api_hr.service.core.interfaces;

import java.time.LocalDate;
import java.util.List;

import com.tamabee.api_hr.dto.request.attendance.EmployeeSwapRequest;
import com.tamabee.api_hr.dto.response.attendance.EmployeeScheduleDataResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;

/**
 * Service interface cho employee xem lịch làm việc
 */
public interface IEmployeeScheduleService {

    /**
     * Lấy tất cả dữ liệu lịch làm việc (ca + lịch sử đổi ca) trong 1 API call
     */
    EmployeeScheduleDataResponse getAllScheduleData(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Lấy lịch làm việc của nhân viên trong khoảng thời gian
     */
    List<ShiftAssignmentResponse> getMySchedule(Long employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Lấy lịch sử đổi ca của nhân viên
     */
    List<ShiftSwapRequestResponse> getSwapHistory(Long employeeId);

    /**
     * Lấy danh sách ca có thể đổi từ nhân viên khác
     */
    List<ShiftAssignmentResponse> getAvailableShiftsForSwap(Long employeeId, Long myShiftId);

    /**
     * Tạo yêu cầu đổi ca
     */
    ShiftSwapRequestResponse createSwapRequest(Long employeeId, EmployeeSwapRequest request);
}
