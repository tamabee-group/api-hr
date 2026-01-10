package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.response.attendance.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Mapper chuyển đổi giữa AttendanceAdjustmentRequestEntity và DTO.
 */
@Component
public class AttendanceAdjustmentMapper {

    /**
     * Chuyển đổi entity sang response
     *
     * @param entity       entity cần chuyển đổi
     * @param employeeName tên nhân viên
     * @param approverName tên người phê duyệt (null nếu chưa xử lý)
     * @param workDate     ngày làm việc của bản ghi chấm công
     * @return response
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            LocalDate workDate) {
        return toResponse(entity, employeeName, approverName, workDate, null);
    }

    /**
     * Chuyển đổi entity sang response với thông tin break record
     *
     * @param entity       entity cần chuyển đổi
     * @param employeeName tên nhân viên
     * @param approverName tên người phê duyệt (null nếu chưa xử lý)
     * @param workDate     ngày làm việc của bản ghi chấm công
     * @param breakRecord  break record được điều chỉnh (null nếu không có)
     * @return response
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            LocalDate workDate,
            BreakRecordEntity breakRecord) {
        return toResponse(entity, employeeName, approverName, null, workDate, breakRecord);
    }

    /**
     * Chuyển đổi entity sang response với đầy đủ thông tin
     *
     * @param entity         entity cần chuyển đổi
     * @param employeeName   tên nhân viên
     * @param approverName   tên người phê duyệt (null nếu chưa xử lý)
     * @param assignedToName tên người được gán xử lý
     * @param workDate       ngày làm việc của bản ghi chấm công
     * @param breakRecord    break record được điều chỉnh (null nếu không có)
     * @return response
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            String assignedToName,
            LocalDate workDate,
            BreakRecordEntity breakRecord) {
        if (entity == null) {
            return null;
        }

        // Lấy breakNumber từ break record nếu có
        Integer breakNumber = breakRecord != null ? breakRecord.getBreakNumber() : null;

        return AdjustmentRequestResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .attendanceRecordId(entity.getAttendanceRecordId())
                .workDate(workDate)
                .assignedTo(entity.getAssignedTo())
                .assignedToName(assignedToName)
                .breakRecordId(entity.getBreakRecordId())
                .breakNumber(breakNumber)
                .originalCheckIn(entity.getOriginalCheckIn())
                .originalCheckOut(entity.getOriginalCheckOut())
                .originalBreakStart(entity.getOriginalBreakStart())
                .originalBreakEnd(entity.getOriginalBreakEnd())
                .requestedCheckIn(entity.getRequestedCheckIn())
                .requestedCheckOut(entity.getRequestedCheckOut())
                .requestedBreakStart(entity.getRequestedBreakStart())
                .requestedBreakEnd(entity.getRequestedBreakEnd())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approverName(approverName)
                .approvedAt(entity.getApprovedAt())
                .approverComment(entity.getApproverComment())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi entity sang response với thông tin từ attendance record
     *
     * @param entity           entity cần chuyển đổi
     * @param employeeName     tên nhân viên
     * @param approverName     tên người phê duyệt
     * @param attendanceRecord bản ghi chấm công liên quan
     * @return response
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            AttendanceRecordEntity attendanceRecord) {
        LocalDate workDate = attendanceRecord != null ? attendanceRecord.getWorkDate() : null;
        return toResponse(entity, employeeName, approverName, workDate, null);
    }

    /**
     * Chuyển đổi entity sang response với thông tin từ attendance record và break
     * record
     *
     * @param entity           entity cần chuyển đổi
     * @param employeeName     tên nhân viên
     * @param approverName     tên người phê duyệt
     * @param attendanceRecord bản ghi chấm công liên quan
     * @param breakRecord      break record được điều chỉnh
     * @return response
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            AttendanceRecordEntity attendanceRecord,
            BreakRecordEntity breakRecord) {
        LocalDate workDate = attendanceRecord != null ? attendanceRecord.getWorkDate() : null;
        return toResponse(entity, employeeName, approverName, workDate, breakRecord);
    }
}
