package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.request.schedule.AssignScheduleRequest;
import com.tamabee.api_hr.dto.request.schedule.CreateWorkScheduleRequest;
import com.tamabee.api_hr.dto.request.schedule.UpdateWorkScheduleRequest;
import com.tamabee.api_hr.dto.response.schedule.WorkScheduleAssignmentResponse;
import com.tamabee.api_hr.dto.response.schedule.WorkScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service quản lý lịch làm việc của công ty.
 * Hỗ trợ 3 loại lịch: FIXED (cố định), FLEXIBLE (linh hoạt), SHIFT (theo ca).
 */
public interface IWorkScheduleService {

        // ==================== CRUD Operations ====================

        /**
         * Tạo lịch làm việc mới
         *
         * @param request thông tin lịch làm việc
         * @return lịch làm việc đã tạo
         */
        WorkScheduleResponse createSchedule(CreateWorkScheduleRequest request);

        /**
         * Cập nhật lịch làm việc
         *
         * @param scheduleId ID lịch làm việc
         * @param request    thông tin cập nhật
         * @return lịch làm việc đã cập nhật
         */
        WorkScheduleResponse updateSchedule(Long scheduleId, UpdateWorkScheduleRequest request);

        /**
         * Xóa lịch làm việc (soft delete)
         *
         * @param scheduleId ID lịch làm việc
         */
        void deleteSchedule(Long scheduleId);

        // ==================== Query Operations ====================

        /**
         * Lấy danh sách lịch làm việc của công ty (phân trang)
         *
         * @param pageable thông tin phân trang
         * @return danh sách lịch làm việc
         */
        Page<WorkScheduleResponse> getSchedules(Pageable pageable);

        /**
         * Lấy thông tin lịch làm việc theo ID
         *
         * @param scheduleId ID lịch làm việc
         * @return thông tin lịch làm việc
         */
        WorkScheduleResponse getScheduleById(Long scheduleId);

        /**
         * Lấy lịch làm việc mặc định của công ty
         *
         * @return lịch làm việc mặc định (null nếu chưa có)
         */
        WorkScheduleResponse getDefaultSchedule();

        // ==================== Employee Schedule ====================

        /**
         * Lấy lịch làm việc hiệu lực của nhân viên tại một ngày cụ thể.
         * Nếu nhân viên chưa được gán lịch, trả về lịch mặc định của công ty.
         *
         * @param employeeId ID nhân viên
         * @param date       ngày cần kiểm tra
         * @return lịch làm việc hiệu lực
         */
        WorkScheduleResponse getEffectiveSchedule(Long employeeId, LocalDate date);

        // ==================== Assignment Operations ====================

        /**
         * Gán lịch làm việc cho một nhân viên
         *
         * @param scheduleId    ID lịch làm việc
         * @param employeeId    ID nhân viên
         * @param effectiveFrom ngày bắt đầu áp dụng
         * @param effectiveTo   ngày kết thúc (null = vô thời hạn)
         * @return thông tin assignment
         */
        WorkScheduleAssignmentResponse assignScheduleToEmployee(
                        Long scheduleId,
                        Long employeeId,
                        LocalDate effectiveFrom,
                        LocalDate effectiveTo);

        /**
         * Gán lịch làm việc cho nhiều nhân viên
         *
         * @param scheduleId ID lịch làm việc
         * @param request    thông tin gán (danh sách nhân viên, thời gian hiệu lực)
         * @return danh sách assignment đã tạo
         */
        List<WorkScheduleAssignmentResponse> assignScheduleToEmployees(
                        Long scheduleId,
                        AssignScheduleRequest request);

        /**
         * Xóa assignment (soft delete)
         *
         * @param assignmentId ID assignment
         */
        void removeAssignment(Long assignmentId);

        /**
         * Lấy danh sách assignment của một lịch làm việc (phân trang)
         *
         * @param scheduleId ID lịch làm việc
         * @param pageable   thông tin phân trang
         * @return danh sách assignment
         */
        Page<WorkScheduleAssignmentResponse> getAssignmentsBySchedule(Long scheduleId, Pageable pageable);

        /**
         * Lấy danh sách assignment của một nhân viên
         *
         * @param employeeId ID nhân viên
         * @return danh sách assignment
         */
        List<WorkScheduleAssignmentResponse> getAssignmentsByEmployee(Long employeeId);
}
