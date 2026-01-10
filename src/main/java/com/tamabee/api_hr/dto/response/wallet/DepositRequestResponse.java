package com.tamabee.api_hr.dto.response.wallet;

import com.tamabee.api_hr.enums.DepositStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // Email người tạo yêu cầu
    private String requesterEmail;

    // Employee code của người duyệt/từ chối
    private String approvedBy;

    // Tên người duyệt/từ chối
    private String approvedByName;

    // Lý do từ chối
    private String rejectionReason;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
}
