package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Entity lưu trữ phân ca làm việc cho nhân viên.
 * Ghi nhận ca làm việc được gán cho nhân viên vào ngày cụ thể.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shift_assignments", indexes = {
        @Index(name = "idx_shift_assign_employee_id", columnList = "employeeId"),
        @Index(name = "idx_shift_assign_company_id", columnList = "companyId"),
        @Index(name = "idx_shift_assign_work_date", columnList = "workDate"),
        @Index(name = "idx_shift_assign_employee_date", columnList = "employeeId, workDate"),
        @Index(name = "idx_shift_assign_company_date", columnList = "companyId, workDate")
})
public class ShiftAssignmentEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // ID mẫu ca làm việc
    @Column(nullable = false)
    private Long shiftTemplateId;

    // Ngày làm việc
    @Column(nullable = false)
    private LocalDate workDate;

    // Trạng thái phân ca
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftAssignmentStatus status = ShiftAssignmentStatus.SCHEDULED;

    // ID nhân viên đã đổi ca (nếu có)
    private Long swappedWithEmployeeId;

    // ID phân ca gốc trước khi đổi (nếu có)
    private Long swappedFromAssignmentId;
}
