package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo sử dụng ca làm việc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftUtilizationReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng quan
    private Integer totalShiftAssignments;
    private Integer completedShifts;
    private Integer cancelledShifts;
    private Integer swappedShifts;
    private Double shiftCompletionRate;

    // Thống kê swap
    private Integer totalSwapRequests;
    private Integer approvedSwaps;
    private Integer rejectedSwaps;
    private Integer pendingSwaps;
    private Double swapApprovalRate;

    // Chi tiết theo ca
    private List<ShiftTemplateSummary> shiftSummaries;
}
