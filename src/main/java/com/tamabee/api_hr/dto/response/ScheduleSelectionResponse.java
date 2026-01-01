package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.SelectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chứa thông tin yêu cầu chọn lịch làm việc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSelectionResponse {

    private Long id;

    // Thông tin nhân viên
    private Long employeeId;
    private String employeeName;

    // Thông tin công ty
    private Long companyId;

    // Thông tin lịch làm việc
    private Long scheduleId;
    private String scheduleName;

    // Thời gian áp dụng
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Trạng thái
    private SelectionStatus status;

    // Thông tin phê duyệt
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectionReason;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
