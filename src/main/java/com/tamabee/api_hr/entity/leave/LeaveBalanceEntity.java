package com.tamabee.api_hr.entity.leave;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ số ngày phép còn lại của nhân viên theo năm và loại phép.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "leave_balances", indexes = {
        @Index(name = "idx_leave_balances_employee_id", columnList = "employeeId"),
        @Index(name = "idx_leave_balances_deleted", columnList = "deleted"),
        @Index(name = "idx_leave_balances_employee_year", columnList = "employeeId, year"),
        @Index(name = "idx_leave_balances_employee_year_type", columnList = "employeeId, year, leaveType", unique = true)
})
public class LeaveBalanceEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // Năm
    @Column(nullable = false)
    private Integer year;

    // Loại nghỉ phép
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;

    // Tổng số ngày phép được cấp
    @Column(nullable = false)
    private Integer totalDays;

    // Số ngày đã sử dụng
    @Column(nullable = false)
    private Integer usedDays = 0;

    // Số ngày còn lại (computed: totalDays - usedDays)
    @Column(nullable = false)
    private Integer remainingDays;
}
