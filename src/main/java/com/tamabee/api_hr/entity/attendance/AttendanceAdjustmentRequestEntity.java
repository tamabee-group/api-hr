package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ yêu cầu điều chỉnh chấm công của nhân viên.
 * Nhân viên có thể yêu cầu thay đổi giờ check-in/check-out và cần manager phê
 * duyệt.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_adjustment_requests", indexes = {
        @Index(name = "idx_adjustment_employee_id", columnList = "employeeId"),
        @Index(name = "idx_adjustment_company_id", columnList = "companyId"),
        @Index(name = "idx_adjustment_record_id", columnList = "attendanceRecordId"),
        @Index(name = "idx_adjustment_break_record_id", columnList = "breakRecordId"),
        @Index(name = "idx_adjustment_assigned_to", columnList = "assignedTo"),
        @Index(name = "idx_adjustment_deleted", columnList = "deleted"),
        @Index(name = "idx_adjustment_status", columnList = "status"),
        @Index(name = "idx_adjustment_pending", columnList = "companyId, status")
})
public class AttendanceAdjustmentRequestEntity extends BaseEntity {

    // ID nhân viên yêu cầu
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // ID bản ghi chấm công cần điều chỉnh (nullable khi tạo yêu cầu cho ngày chưa
    // có chấm công)
    private Long attendanceRecordId;

    // Ngày làm việc cần điều chỉnh (dùng khi không có attendanceRecordId)
    private java.time.LocalDate workDate;

    // ID của break record cần điều chỉnh (cho multiple breaks support)
    private Long breakRecordId;

    // ID người được gán xử lý yêu cầu (manager/admin)
    private Long assignedTo;

    // === Thời gian gốc ===
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // === Thời gian break gốc ===
    private LocalDateTime originalBreakStart;
    private LocalDateTime originalBreakEnd;

    // === Thời gian yêu cầu thay đổi ===
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    // === Thời gian break yêu cầu thay đổi ===
    private LocalDateTime requestedBreakStart;
    private LocalDateTime requestedBreakEnd;

    // Lý do yêu cầu điều chỉnh
    @Column(length = 500, nullable = false)
    private String reason;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    // === Approval info ===
    // Người phê duyệt/từ chối
    private Long approvedBy;

    // Thời gian phê duyệt/từ chối
    private LocalDateTime approvedAt;

    // Ghi chú của người phê duyệt
    @Column(length = 500)
    private String approverComment;

    // Lý do từ chối (nếu bị từ chối)
    @Column(length = 500)
    private String rejectionReason;
}
