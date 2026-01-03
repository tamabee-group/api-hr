package com.tamabee.api_hr.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO cho kết quả phân ca hàng loạt
 */
@Data
@Builder
public class BatchAssignmentResult {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<ShiftAssignmentResponse> successfulAssignments;
    private List<FailedAssignment> failedAssignments;

    @Data
    @Builder
    public static class FailedAssignment {
        private Long employeeId;
        private String employeeName;
        private String reason;
    }
}
