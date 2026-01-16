package com.tamabee.api_hr.dto.response.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CompanyResponse {
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
    private Long referredByEmployeeId;
    private String logo;
    private Long ownerId;
    private String tenantDomain;
    private String status;
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
