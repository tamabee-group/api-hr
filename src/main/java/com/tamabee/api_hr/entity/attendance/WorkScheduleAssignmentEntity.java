package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Entity lưu trữ việc gán lịch làm việc cho nhân viên.
 * Mỗi nhân viên có thể được gán nhiều lịch khác nhau theo khoảng thời gian.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "work_schedule_assignments", indexes = {
        @Index(name = "idx_schedule_assignments_employee_id", columnList = "employeeId"),
        @Index(name = "idx_schedule_assignments_schedule_id", columnList = "scheduleId"),
        @Index(name = "idx_schedule_assignments_deleted", columnList = "deleted"),
        @Index(name = "idx_schedule_assignments_effective", columnList = "employeeId, effectiveFrom, effectiveTo")
})
public class WorkScheduleAssignmentEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID lịch làm việc
    @Column(nullable = false)
    private Long scheduleId;

    // Ngày bắt đầu áp dụng
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // Ngày kết thúc áp dụng (null = vô thời hạn)
    private LocalDate effectiveTo;
}
