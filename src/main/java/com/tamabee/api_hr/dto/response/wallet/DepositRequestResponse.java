package com.tamabee.api_hr.dto.response.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.DepositStatus;

import lombok.Data;

/**
 * Response DTO cho yêu cầu nạp tiền
 */
@Data
public class DepositRequestResponse {

    private Long id;

    private Long companyId;

    private String companyName;

    private BigDecimal amount;

    private String transferProofUrl;

    private DepositStatus status;

    // Employee code của người tạo yêu cầu
    private String requestedBy;

    // Tên người tạo yêu cầu (fallback về employee code nếu không có name)
    private String requesterName;

    // Role của người tạo yêu cầu
    private String requesterRole;

    // Email người tạo yêu cầu
    private String requesterEmail;

    // Ngôn ngữ của người tạo yêu cầu (vi, en, ja)
    private String requesterLanguage;

    // Employee code của người duyệt/từ chối
    private String approvedBy;

    // Tên người duyệt/từ chối
    private String approverName;

    // Role của người duyệt/từ chối
    private String approverRole;

    // Email người duyệt/từ chối
    private String approverEmail;

    // Lý do từ chối
    private String rejectionReason;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
}
