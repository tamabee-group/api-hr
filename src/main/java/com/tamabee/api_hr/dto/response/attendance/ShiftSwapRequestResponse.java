package com.tamabee.api_hr.dto.response.attendance;

import com.tamabee.api_hr.enums.SwapRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO cho yêu cầu đổi ca.
 */
@Data
public class ShiftSwapRequestResponse {

    private Long id;
    private Long companyId;
    private Long requesterId;
    private String requesterName;
    private Long targetEmployeeId;
    private String targetEmployeeName;
    private Long requesterAssignmentId;
    private ShiftAssignmentResponse requesterAssignment;
    private Long targetAssignmentId;
    private ShiftAssignmentResponse targetAssignment;
    private String reason;
    private SwapRequestStatus status;
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
