package com.tamabee.api_hr.entity.audit;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.WorkMode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ audit log khi work mode của công ty thay đổi.
 * Ghi lại mọi thay đổi từ FIXED_HOURS sang FLEXIBLE_SHIFT và ngược lại.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "work_mode_change_logs", indexes = {
        @Index(name = "idx_work_mode_change_logs_changed_at", columnList = "changedAt")
})
public class WorkModeChangeLogEntity extends BaseEntity {

    // Chế độ làm việc trước khi thay đổi
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkMode previousMode;

    // Chế độ làm việc sau khi thay đổi
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkMode newMode;

    // Người thực hiện thay đổi (email hoặc employee code)
    @Column(nullable = false)
    private String changedBy;

    // Thời gian thay đổi
    @Column(nullable = false)
    private LocalDateTime changedAt;

    // Lý do thay đổi (optional)
    @Column(length = 500)
    private String reason;
}
