package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo phân tích chi phí nhân sự
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostAnalysisReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng chi phí
    private BigDecimal totalLaborCost;
    private BigDecimal totalBaseSalaryCost;
    private BigDecimal totalOvertimeCost;
    private BigDecimal totalAllowanceCost;
    private BigDecimal totalDeductionAmount;

    // Phân tích theo loại hợp đồng
    private List<CostByContractType> costByContractType;

    // Phân tích theo loại lương
    private List<CostBySalaryType> costBySalaryType;
}
