package com.tamabee.api_hr.dto.response.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.CompanyStatus;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO cho thông tin công ty (company profile)
 */
@Data
@Builder
public class CompanyProfileResponse {
    private Long id;
    private String name;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private String industry;
    private String zipcode;
    private String locale;
    private String language;
    private String logo;
    private String tenantDomain;
    private CompanyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Plan info
    private Long planId;
    private String planNameVi;
    private String planNameEn;
    private String planNameJa;
    private BigDecimal planMonthlyPrice;
    private Integer planMaxEmployees;
    
    // Wallet info
    private BigDecimal walletBalance;
    private LocalDateTime lastBillingDate;
    private LocalDateTime nextBillingDate;
    private LocalDateTime freeTrialEndDate;
    private Boolean isFreeTrialActive;
}
