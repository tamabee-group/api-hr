package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Tổng hợp sử dụng một ca làm việc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTemplateSummary {

    private Long shiftTemplateId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    // Thống kê
    private Integer totalAssignments;
    private Integer completedAssignments;
    private Integer cancelledAssignments;
    private Integer swappedAssignments;

    // Tỷ lệ
    private Double utilizationRate;
    private Double completionRate;
}
