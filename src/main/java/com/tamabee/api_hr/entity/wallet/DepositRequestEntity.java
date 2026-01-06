package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity cho yêu cầu nạp tiền
 * Company tạo yêu cầu với ảnh chứng minh chuyển khoản, Tamabee staff duyệt/từ
 * chối
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "deposit_requests", indexes = {
        @Index(name = "idx_deposit_requests_company_id", columnList = "companyId"),
        @Index(name = "idx_deposit_requests_status", columnList = "status"),
        @Index(name = "idx_deposit_requests_deleted", columnList = "deleted"),
        @Index(name = "idx_deposit_requests_company_id_deleted", columnList = "companyId, deleted"),
        @Index(name = "idx_deposit_requests_status_deleted", columnList = "status, deleted")
})
public class DepositRequestEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // URL ảnh chứng minh chuyển khoản
    @Column(name = "transfer_proof_url", nullable = false, length = 500)
    private String transferProofUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DepositStatus status = DepositStatus.PENDING;

    // Employee code của người tạo yêu cầu
    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    // Employee code của người duyệt/từ chối
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    // Lý do từ chối (nếu bị reject)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Thời điểm xử lý (approve/reject)
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
