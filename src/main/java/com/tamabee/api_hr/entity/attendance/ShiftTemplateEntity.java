package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Entity lưu trữ mẫu ca làm việc.
 * Định nghĩa các ca làm việc chuẩn của công ty (ca sáng, ca chiều, ca đêm...).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shift_templates", indexes = {
        @Index(name = "idx_shift_template_company_id", columnList = "companyId"),
        @Index(name = "idx_shift_template_deleted", columnList = "deleted"),
        @Index(name = "idx_shift_template_active", columnList = "companyId, isActive")
})
public class ShiftTemplateEntity extends BaseEntity {

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Tên ca làm việc
    @Column(nullable = false, length = 100)
    private String name;

    // Thời gian bắt đầu ca
    @Column(nullable = false)
    private LocalTime startTime;

    // Thời gian kết thúc ca
    @Column(nullable = false)
    private LocalTime endTime;

    // Số phút giải lao trong ca
    private Integer breakMinutes;

    // Hệ số lương (1.0 cho ca thường, 1.5 cho ca đêm, 2.0 cho ca lễ...)
    @Column(precision = 5, scale = 2)
    private BigDecimal multiplier;

    // Mô tả ca làm việc
    @Column(length = 500)
    private String description;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean isActive = true;
}
