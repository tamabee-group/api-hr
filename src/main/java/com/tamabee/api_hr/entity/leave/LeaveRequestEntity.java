package com.tamabee.api_hr.entity.leave;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ yêu cầu nghỉ phép của nhân viên.
 * Hỗ trợ các loại nghỉ: ANNUAL (phép năm), SICK (ốm), PERSONAL (việc riêng),
 * UNPAID (không lương).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_requests_employee_id", columnList = "employeeId"),
        @Index(name = "idx_leave_requests_company_id", columnList = "companyId"),
        @Index(name = "idx_leave_requests_status", columnList = "status"),
        @Index(name = "idx_leave_requests_pending", columnList = "companyId, status"),
        @Index(name = "idx_leave_requests_dates", columnList = "employeeId, startDate, endDate")
})
public class LeaveRequestEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Loại nghỉ phép
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;

    // Ngày bắt đầu nghỉ
    @Column(nullable = false)
    private LocalDate startDate;

    // Ngày kết thúc nghỉ
    @Column(nullable = false)
    private LocalDate endDate;

    // Tổng số ngày nghỉ
    private Integer totalDays;

    // Lý do nghỉ
    @Column(length = 500)
    private String reason;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    // === Approval info ===
    // Người phê duyệt/từ chối
    private Long approvedBy;

    // Thời gian phê duyệt/từ chối
    private LocalDateTime approvedAt;

    // Lý do từ chối (nếu bị từ chối)
    @Column(length = 500)
    private String rejectionReason;
}
