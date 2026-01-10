package com.tamabee.api_hr.dto.request.payroll;

import com.tamabee.api_hr.dto.config.DeductionRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request cập nhật cấu hình khấu trừ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionConfigRequest {

    @Valid
    private List<DeductionRule> deductions;

    private Boolean enableLatePenalty;

    @DecimalMin("0")
    private BigDecimal latePenaltyPerMinute;

    private Boolean enableEarlyLeavePenalty;

    @DecimalMin("0")
    private BigDecimal earlyLeavePenaltyPerMinute;

    private Boolean enableAbsenceDeduction;
}
