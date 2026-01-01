package com.tamabee.api_hr.entity.attendance;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity lưu trữ lịch làm việc của công ty.
 * Hỗ trợ 3 loại: FIXED (cố định), FLEXIBLE (linh hoạt theo ngày), SHIFT (theo
 * ca).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "work_schedules", indexes = {
        @Index(name = "idx_work_schedules_company_id", columnList = "companyId"),
        @Index(name = "idx_work_schedules_deleted", columnList = "deleted"),
        @Index(name = "idx_work_schedules_is_default", columnList = "companyId, isDefault")
})
public class WorkScheduleEntity extends BaseEntity {

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Tên lịch làm việc
    @Column(nullable = false)
    private String name;

    // Loại lịch: FIXED, FLEXIBLE, SHIFT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    // Đánh dấu lịch mặc định của công ty
    @Column(nullable = false)
    private Boolean isDefault = false;

    // Dữ liệu chi tiết lịch làm việc (JSONB)
    // Chứa thông tin giờ bắt đầu, kết thúc, nghỉ trưa theo từng ngày/ca
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String scheduleData;

    // Mô tả lịch làm việc
    private String description;
}
