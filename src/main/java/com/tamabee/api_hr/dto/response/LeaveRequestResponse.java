package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chứa thông tin yêu cầu nghỉ phép.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {

    private Long id;

    // Thông tin nhân viên
    private Long employeeId;
    private String employeeName;

    // Thông tin công ty
    private Long companyId;

    // Loại nghỉ phép
    private LeaveType leaveType;

    // Khoảng thời gian nghỉ
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;

    // Lý do nghỉ
    private String reason;

    // Trạng thái
    private LeaveStatus status;

    // Thông tin phê duyệt
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectionReason;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
