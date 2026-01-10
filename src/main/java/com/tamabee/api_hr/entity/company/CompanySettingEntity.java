package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.WorkMode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * Entity lưu trữ cấu hình chấm công và tính lương của từng công ty.
 * Mỗi tenant DB có 1 bộ settings duy nhất với các config được lưu dưới dạng JSONB.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company_settings", indexes = {
        @Index(name = "idx_company_settings_deleted", columnList = "deleted"),
        @Index(name = "idx_company_settings_work_mode", columnList = "workMode")
})
public class CompanySettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Chế độ làm việc của công ty
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkMode workMode = WorkMode.FLEXIBLE_SHIFT;

    // Giờ bắt đầu làm việc mặc định (dùng cho FIXED_HOURS mode)
    private LocalTime defaultWorkStartTime;

    // Giờ kết thúc làm việc mặc định (dùng cho FIXED_HOURS mode)
    private LocalTime defaultWorkEndTime;

    // Thời gian nghỉ giải lao mặc định (phút) (dùng cho FIXED_HOURS mode)
    private Integer defaultBreakMinutes;

    // Cấu hình chấm công (giờ làm việc, làm tròn, grace period, device/location)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attendanceConfig;

    // Cấu hình tính lương (loại lương, ngày trả lương, ngày chốt)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payrollConfig;

    // Cấu hình tăng ca (hệ số, giờ đêm, giới hạn)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String overtimeConfig;

    // Cấu hình phụ cấp (danh sách các loại phụ cấp)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String allowanceConfig;

    // Cấu hình khấu trừ (danh sách các loại khấu trừ, phạt đi muộn/về sớm)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String deductionConfig;

    // Cấu hình giờ giải lao (break type, min/max, legal minimum)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String breakConfig;
}
