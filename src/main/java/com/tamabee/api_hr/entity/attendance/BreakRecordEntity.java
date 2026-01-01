package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ bản ghi giờ giải lao của nhân viên.
 * Mỗi bản ghi chấm công có thể có nhiều bản ghi giải lao.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "break_records", indexes = {
        @Index(name = "idx_break_records_attendance", columnList = "attendanceRecordId"),
        @Index(name = "idx_break_records_employee_id", columnList = "employeeId"),
        @Index(name = "idx_break_records_company_id", columnList = "companyId"),
        @Index(name = "idx_break_records_work_date", columnList = "workDate"),
        @Index(name = "idx_break_records_deleted", columnList = "deleted"),
        @Index(name = "idx_break_records_employee_date", columnList = "employeeId, workDate"),
        @Index(name = "idx_break_records_break_number", columnList = "attendanceRecordId, breakNumber")
})
public class BreakRecordEntity extends BaseEntity {

    // ID bản ghi chấm công liên quan
    @Column(nullable = false)
    private Long attendanceRecordId;

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Ngày làm việc
    @Column(nullable = false)
    private LocalDate workDate;

    // Số thứ tự break trong ngày (1, 2, 3, ...)
    @Column(nullable = false)
    private Integer breakNumber;

    // Thời gian bắt đầu giải lao
    private LocalDateTime breakStart;

    // Thời gian kết thúc giải lao
    private LocalDateTime breakEnd;

    // Thời gian giải lao thực tế (phút)
    private Integer actualBreakMinutes;

    // Thời gian giải lao hiệu lực sau khi áp dụng min/max (phút)
    private Integer effectiveBreakMinutes;

    // Ghi chú
    @Column(length = 500)
    private String notes;
}
