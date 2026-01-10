package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.SelectionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ yêu cầu chọn lịch làm việc của nhân viên.
 * Nhân viên có thể chọn lịch làm việc phù hợp và cần manager phê duyệt.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "schedule_selections", indexes = {
        @Index(name = "idx_selection_employee_id", columnList = "employeeId"),
        @Index(name = "idx_selection_schedule_id", columnList = "scheduleId"),
        @Index(name = "idx_selection_status", columnList = "status")
})
public class ScheduleSelectionEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID lịch làm việc được chọn
    @Column(nullable = false)
    private Long scheduleId;

    // Ngày bắt đầu áp dụng
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // Ngày kết thúc áp dụng (null = vô thời hạn)
    private LocalDate effectiveTo;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SelectionStatus status = SelectionStatus.PENDING;

    // === Approval info ===
    // Người phê duyệt/từ chối
    private Long approvedBy;

    // Thời gian phê duyệt/từ chối
    private LocalDateTime approvedAt;

    // Lý do từ chối (nếu bị từ chối)
    @Column(length = 500)
    private String rejectionReason;
}
