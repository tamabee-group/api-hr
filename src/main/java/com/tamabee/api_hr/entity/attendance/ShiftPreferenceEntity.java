package com.tamabee.api_hr.entity.attendance;

import java.time.LocalTime;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.PreferencePriority;
import com.tamabee.api_hr.enums.PreferenceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ nguyện vọng ca làm việc của nhân viên.
 * Mỗi bản ghi đại diện cho nguyện vọng của một nhân viên cho một ngày cụ thể trong tuần.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shift_preferences", indexes = {
        @Index(name = "idx_preference_employee", columnList = "employeeId"),
        @Index(name = "idx_preference_week", columnList = "year, weekNumber"),
        @Index(name = "idx_preference_employee_week", columnList = "employeeId, year, weekNumber"),
        @Index(name = "idx_preference_status", columnList = "status")
})
public class ShiftPreferenceEntity extends BaseEntity {

    // ID nhân viên gửi nguyện vọng
    @Column(nullable = false)
    private Long employeeId;

    // Năm của tuần nguyện vọng
    @Column(nullable = false)
    private Integer year;

    // Số tuần trong năm (ISO 8601)
    @Column(nullable = false)
    private Integer weekNumber;

    // Ngày trong tuần: 1=Monday..7=Sunday (ISO 8601)
    @Column(nullable = false)
    private Integer dayOfWeek;

    // ID mẫu ca làm việc (nullable nếu dùng custom time)
    private Long shiftTemplateId;

    // Giờ bắt đầu tùy chỉnh (nullable nếu chọn template)
    private LocalTime customStartTime;

    // Giờ kết thúc tùy chỉnh (nullable nếu chọn template)
    private LocalTime customEndTime;

    // Lý do nguyện vọng
    @Column(columnDefinition = "TEXT")
    private String reason;

    // Mức ưu tiên: NORMAL (không có lý do), HIGH (có lý do)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreferencePriority priority = PreferencePriority.NORMAL;

    // Trạng thái: PENDING, APPLIED, EXPIRED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreferenceStatus status = PreferenceStatus.PENDING;

    // ID assignment được tạo khi apply (dùng cho undo)
    private Long appliedAssignmentId;
}
