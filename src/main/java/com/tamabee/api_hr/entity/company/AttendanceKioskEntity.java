package com.tamabee.api_hr.entity.company;

import java.time.LocalDateTime;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ thông tin máy chấm công cố định (kiosk).
 * Mỗi kiosk gắn với 1 vị trí chấm công, xác thực bằng PIN.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_kiosks", indexes = {
        @Index(name = "idx_kiosk_deleted", columnList = "deleted"),
        @Index(name = "idx_kiosk_active", columnList = "is_active"),
        @Index(name = "idx_kiosk_pin_code", columnList = "pin_code")
})
public class AttendanceKioskEntity extends BaseEntity {

    @Column(nullable = false)
    private Boolean deleted = false;

    // Tên kiosk (VD: "Máy chấm công tầng 1")
    @Column(nullable = false, length = 200)
    private String name;

    // Mã PIN để đăng nhập kiosk
    @Column(nullable = false, length = 10)
    private String pinCode;

    // Vị trí chấm công gắn với kiosk
    @Column(nullable = false)
    private Long locationId;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean isActive = true;

    // Thời điểm hoạt động gần nhất
    private LocalDateTime lastActiveAt;
}
