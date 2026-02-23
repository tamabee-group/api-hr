package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ cấu hình tính lương.
 * Bao gồm loại lương mặc định, ngày trả lương, ngày chốt công,
 * cách làm tròn lương, số ngày/giờ làm việc tiêu chuẩn.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payroll_settings", indexes = {
        @Index(name = "idx_payroll_settings_deleted", columnList = "deleted")
})
public class PayrollSettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Loại lương mặc định: MONTHLY, HOURLY, DAILY
    private String defaultSalaryType;

    // Ngày trả lương (1-31)
    private Integer payDay;

    // Ngày chốt công (1-31)
    private Integer cutoffDay;

    // Cách làm tròn lương: NEAREST, UP, DOWN
    private String salaryRounding;

    // Số ngày làm việc tiêu chuẩn mỗi tháng
    private Integer standardWorkingDaysPerMonth;

    // Số giờ làm việc tiêu chuẩn mỗi ngày
    private Integer standardWorkingHoursPerDay;
}
