package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.CompanyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho company được Employee Tamabee giới thiệu
 * Bao gồm thông tin company, service usage và commission status
 */
@Data
@Builder
public class ReferredCompanyResponse {

    // Company info
    private Long companyId;
    private String companyName;
    private String ownerName;
    private String planName;
    private CompanyStatus status;

    // Service usage info
    private BigDecimal currentBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalBilling;

    // Commission info
    private Long commissionId;
    private BigDecimal commissionAmount;
    private CommissionStatus commissionStatus;
    private LocalDateTime commissionPaidAt;

    // Timestamps
    private LocalDateTime companyCreatedAt;
}
