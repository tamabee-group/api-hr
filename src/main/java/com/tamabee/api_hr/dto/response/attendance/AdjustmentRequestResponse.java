package com.tamabee.api_hr.dto.response.attendance;

import com.tamabee.api_hr.enums.AdjustmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chứa thông tin yêu cầu điều chỉnh chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentRequestResponse {

    private Long id;

    // Thông tin nhân viên
    private Long employeeId;
    private String employeeName;

    // Thông tin công ty
    private Long companyId;

    // Thông tin bản ghi chấm công
    private Long attendanceRecordId;
    private LocalDate workDate;

    // Thông tin người được gán xử lý
    private Long assignedTo;
    private String assignedToName;

    // Thông tin break record được điều chỉnh
    private Long breakRecordId;
    private Integer breakNumber;

    // Thời gian gốc
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // Thời gian break gốc
    private LocalDateTime originalBreakStart;
    private LocalDateTime originalBreakEnd;

    // Thời gian yêu cầu thay đổi
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    // Thời gian break yêu cầu thay đổi
    private LocalDateTime requestedBreakStart;
    private LocalDateTime requestedBreakEnd;

    // Lý do yêu cầu
    private String reason;

    // Trạng thái
    private AdjustmentStatus status;

    // Thông tin phê duyệt
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedAt;
    private String approverComment;
    private String rejectionReason;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
