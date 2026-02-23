package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ vị trí chấm công.
 * Bao gồm tên, địa chỉ, tọa độ GPS (latitude, longitude), bán kính cho phép, trạng thái hoạt động.
 * Hỗ trợ soft delete.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_locations", indexes = {
        @Index(name = "idx_attendance_locations_deleted", columnList = "deleted"),
        @Index(name = "idx_attendance_locations_active", columnList = "is_active")
})
public class AttendanceLocationEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Tên vị trí
    @Column(nullable = false, length = 200)
    private String name;

    // Địa chỉ
    @Column(length = 500)
    private String address;

    // Vĩ độ
    @Column(nullable = false)
    private Double latitude;

    // Kinh độ
    @Column(nullable = false)
    private Double longitude;

    // Bán kính cho phép chấm công (mét)
    @Column(nullable = false)
    private Integer radiusMeters;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean isActive = true;
}
