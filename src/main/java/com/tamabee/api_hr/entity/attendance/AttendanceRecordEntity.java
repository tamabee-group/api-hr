package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.BreakType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ bản ghi chấm công của nhân viên.
 * Bao gồm thời gian gốc, thời gian sau làm tròn, và các tính toán liên quan.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_records", indexes = {
        @Index(name = "idx_attendance_employee_id", columnList = "employeeId"),
        @Index(name = "idx_attendance_work_date", columnList = "workDate"),
        @Index(name = "idx_attendance_employee_date", columnList = "employeeId, workDate"),
        @Index(name = "idx_attendance_status", columnList = "status")
})
public class AttendanceRecordEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // Ngày làm việc
    @Column(nullable = false)
    private LocalDate workDate;

    // === Thời gian gốc (trước khi làm tròn) ===
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // === Thời gian sau khi làm tròn ===
    private LocalDateTime roundedCheckIn;
    private LocalDateTime roundedCheckOut;

    // === Tính toán thời gian (đơn vị: phút) ===
    // Tổng số phút làm việc
    private Integer workingMinutes;

    // Số phút tăng ca
    private Integer overtimeMinutes;

    // Số phút đi muộn
    private Integer lateMinutes;

    // Số phút về sớm
    private Integer earlyLeaveMinutes;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    // === Device & Location info ===
    // ID thiết bị check-in
    private String checkInDeviceId;

    // ID thiết bị check-out
    private String checkOutDeviceId;

    // Vị trí check-in
    private Double checkInLatitude;
    private Double checkInLongitude;

    // Vị trí check-out
    private Double checkOutLatitude;
    private Double checkOutLongitude;

    // === Audit info ===
    // Lý do điều chỉnh (nếu có)
    @Column(length = 500)
    private String adjustmentReason;

    // Người điều chỉnh
    private Long adjustedBy;

    // Thời gian điều chỉnh
    private LocalDateTime adjustedAt;

    // === Break time fields ===
    // Tổng thời gian giải lao (phút)
    private Integer totalBreakMinutes;

    // Thời gian giải lao hiệu lực sau khi áp dụng min/max (phút)
    private Integer effectiveBreakMinutes;

    // Loại giải lao áp dụng (PAID/UNPAID)
    @Enumerated(EnumType.STRING)
    private BreakType breakType;

    // Có tuân thủ minimum break không
    private Boolean breakCompliant;
}
