package com.tamabee.api_hr.dto.response.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả phân ca hàng loạt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAssignmentResult {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<ShiftAssignmentResponse> successfulAssignments;
    private List<FailedAssignment> failedAssignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedAssignment {
        private Long employeeId;
        private String employeeName;
        private String reason;
    }
}
