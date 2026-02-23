package com.tamabee.api_hr.entity.leave;

import java.time.LocalDate;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.HolidayType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ ngày nghỉ lễ.
 * Mỗi tenant DB có holidays riêng.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holidays_date", columnList = "date"),
        @Index(name = "idx_holidays_deleted", columnList = "deleted"),
        @Index(name = "idx_holidays_type", columnList = "type")
})
public class HolidayEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Ngày nghỉ
    @Column(nullable = false)
    private LocalDate date;

    // Tên ngày nghỉ
    @Column(nullable = false)
    private String name;

    // Loại ngày nghỉ: NATIONAL (quốc gia), COMPANY (công ty)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayType type;

    // Có được trả lương không
    @Column(nullable = false)
    private Boolean isPaid = true;

    // Mô tả thêm
    private String description;
}
