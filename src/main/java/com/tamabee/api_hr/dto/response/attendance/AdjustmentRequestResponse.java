package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tamabee.api_hr.enums.AdjustmentRequestType;
import com.tamabee.api_hr.enums.AdjustmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin yêu cầu điều chỉnh chấm công.
 * Hỗ trợ nhiều break items trong 1 request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentRequestResponse {

    private Long id;

    // Loại yêu cầu: ADJUST, DELETE_RECORD
    private AdjustmentRequestType requestType;

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

    // Thời gian gốc
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // Thời gian yêu cầu thay đổi
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    // Danh sách các break items được điều chỉnh
    private List<BreakItemResponse> breakItems;

    // Tất cả break records của ngày đó (để người duyệt có cái nhìn đầy đủ)
    private List<BreakRecordResponse> allBreakRecords;

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
