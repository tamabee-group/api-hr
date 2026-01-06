package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.SwapRequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ yêu cầu đổi ca giữa các nhân viên.
 * Ghi nhận thông tin người yêu cầu, người được yêu cầu, và trạng thái duyệt.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shift_swap_requests", indexes = {
        @Index(name = "idx_swap_request_requester_id", columnList = "requesterId"),
        @Index(name = "idx_swap_request_target_id", columnList = "targetEmployeeId"),
        @Index(name = "idx_swap_request_company_id", columnList = "companyId"),
        @Index(name = "idx_swap_request_status", columnList = "status"),
        @Index(name = "idx_swap_request_company_status", columnList = "companyId, status")
})
public class ShiftSwapRequestEntity extends BaseEntity {

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // ID nhân viên yêu cầu đổi ca
    @Column(nullable = false)
    private Long requesterId;

    // ID nhân viên được yêu cầu đổi ca
    @Column(nullable = false)
    private Long targetEmployeeId;

    // ID phân ca của người yêu cầu
    @Column(nullable = false)
    private Long requesterAssignmentId;

    // ID phân ca của người được yêu cầu
    @Column(nullable = false)
    private Long targetAssignmentId;

    // Lý do yêu cầu đổi ca
    @Column(length = 500)
    private String reason;

    // Trạng thái yêu cầu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwapRequestStatus status = SwapRequestStatus.PENDING;

    // ID người duyệt
    private Long approvedBy;

    // Thời gian duyệt
    private LocalDateTime approvedAt;

    // Lý do từ chối (nếu có)
    @Column(length = 500)
    private String rejectionReason;
}
