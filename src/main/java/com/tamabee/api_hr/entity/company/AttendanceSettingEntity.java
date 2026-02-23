package com.tamabee.api_hr.entity.company;

import java.time.LocalTime;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ cấu hình chấm công.
 * Bao gồm giờ làm việc, làm tròn, grace period, thiết bị/vị trí, nghỉ cuối tuần/ngày lễ.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_settings", indexes = {
        @Index(name = "idx_attendance_settings_deleted", columnList = "deleted")
})
public class AttendanceSettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Giờ làm việc
    private LocalTime defaultWorkStartTime;
    private LocalTime defaultWorkEndTime;
    private Integer defaultBreakMinutes;

    // Bật/tắt làm tròn tổng
    private Boolean enableRounding;

    // Bật/tắt làm tròn từng loại
    private Boolean enableCheckInRounding;
    private Boolean enableCheckOutRounding;
    private Boolean enableBreakStartRounding;
    private Boolean enableBreakEndRounding;

    // Cấu hình làm tròn check-in
    private String checkInRoundingInterval;
    private String checkInRoundingDirection;

    // Cấu hình làm tròn check-out
    private String checkOutRoundingInterval;
    private String checkOutRoundingDirection;

    // Cấu hình làm tròn bắt đầu giải lao
    private String breakStartRoundingInterval;
    private String breakStartRoundingDirection;

    // Cấu hình làm tròn kết thúc giải lao
    private String breakEndRoundingInterval;
    private String breakEndRoundingDirection;

    // Grace period (phút cho phép đi muộn/về sớm)
    private Integer lateGraceMinutes;
    private Integer earlyLeaveGraceMinutes;

    // Vị trí
    private Boolean requireGeoLocation;
    private Integer geoFenceRadiusMeters;
    private Boolean allowWebCheckIn;

    // Nghỉ cuối tuần & ngày lễ
    private Boolean saturdayOff;
    private Boolean sundayOff;
    private Boolean holidayOff;
}
