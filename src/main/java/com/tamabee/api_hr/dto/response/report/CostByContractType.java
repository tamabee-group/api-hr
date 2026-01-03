package com.tamabee.api_hr.dto.response.report;

import com.tamabee.api_hr.enums.ContractType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Chi phí theo loại hợp đồng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostByContractType {

    private ContractType contractType;
    private Integer employeeCount;
    private BigDecimal totalCost;
    private BigDecimal baseSalaryCost;
    private BigDecimal overtimeCost;
    private BigDecimal allowanceCost;
    private Double percentageOfTotal;
}
