package com.tamabee.api_hr.dto.response.report;

import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Chi phí theo loại lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostBySalaryType {

    private SalaryType salaryType;
    private Integer employeeCount;
    private BigDecimal totalCost;
    private BigDecimal baseSalaryCost;
    private BigDecimal overtimeCost;
    private BigDecimal allowanceCost;
    private Double percentageOfTotal;
}
