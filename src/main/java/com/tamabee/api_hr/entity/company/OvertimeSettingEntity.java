package com.tamabee.api_hr.entity.company;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ cấu hình tăng ca.
 * Bao gồm bật/tắt tăng ca, giờ làm việc tiêu chuẩn,
 * cấu hình ca đêm, các hệ số tăng ca, giới hạn giờ tăng ca.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "overtime_settings", indexes = {
        @Index(name = "idx_overtime_settings_deleted", columnList = "deleted")
})
public class OvertimeSettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Bật/tắt tăng ca
    private Boolean overtimeEnabled;

    // Số giờ làm việc tiêu chuẩn mỗi ngày
    private Integer standardWorkingHours;

    // Giờ bắt đầu ca đêm
    private LocalTime nightStartTime;

    // Giờ kết thúc ca đêm
    private LocalTime nightEndTime;

    // Hệ số tăng ca thường
    private BigDecimal regularOvertimeRate;

    // Hệ số làm đêm
    private BigDecimal nightWorkRate;

    // Hệ số tăng ca đêm
    private BigDecimal nightOvertimeRate;

    // Hệ số tăng ca ngày lễ
    private BigDecimal holidayOvertimeRate;

    // Hệ số tăng ca đêm ngày lễ
    private BigDecimal holidayNightOvertimeRate;

    // Hệ số tăng ca cuối tuần
    private BigDecimal weekendOvertimeRate;

    // Sử dụng mức tối thiểu theo luật
    private Boolean useLegalMinimum;

    // Region cho luật lao động
    private String region;

    // Số giờ tăng ca tối đa mỗi ngày
    private Integer maxOvertimeHoursPerDay;

    // Số giờ tăng ca tối đa mỗi tháng
    private Integer maxOvertimeHoursPerMonth;
}
