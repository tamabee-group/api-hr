package com.tamabee.api_hr.entity.company;

import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ cấu hình giải lao.
 * Bao gồm loại giải lao, thời gian tối thiểu/tối đa, chế độ giải lao cố định,
 * cấu hình ca đêm, và danh sách khung giờ giải lao cố định (JSONB).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "break_settings", indexes = {
        @Index(name = "idx_break_settings_deleted", columnList = "deleted")
})
public class BreakSettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Bật/tắt giải lao
    private Boolean breakEnabled;

    // Loại giải lao: PAID, UNPAID
    private String breakType;

    // Thời gian giải lao mặc định (phút)
    private Integer defaultBreakMinutes;

    // Thời gian giải lao tối thiểu (phút)
    private Integer minimumBreakMinutes;

    // Thời gian giải lao tối đa (phút)
    private Integer maximumBreakMinutes;

    // Sử dụng mức tối thiểu theo luật
    private Boolean useLegalMinimum;

    // Region cho luật lao động
    private String region;

    // Chế độ giải lao cố định
    private Boolean fixedBreakMode;

    // Số lần giải lao mỗi ca
    private Integer breakPeriodsPerAttendance;

    // Số lần giải lao tối đa mỗi ngày
    private Integer maxBreaksPerDay;

    // Danh sách khung giờ giải lao cố định (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String fixedBreakPeriods;

    // Giờ bắt đầu ca đêm
    private LocalTime nightShiftStartTime;

    // Giờ kết thúc ca đêm
    private LocalTime nightShiftEndTime;

    // Thời gian giải lao tối thiểu ca đêm (phút)
    private Integer nightShiftMinimumBreakMinutes;

    // Thời gian giải lao mặc định ca đêm (phút)
    private Integer nightShiftDefaultBreakMinutes;
}
