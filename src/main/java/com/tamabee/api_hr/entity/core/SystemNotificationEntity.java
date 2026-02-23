package com.tamabee.api_hr.entity.core;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.TargetAudience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity lưu trữ master copy thông báo hệ thống do Tamabee tạo.
 * Lưu nội dung 3 ngôn ngữ (vi, en, ja) trong master DB.
 * Không soft delete.
 */
@Entity
@Table(name = "system_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemNotificationEntity extends BaseEntity {

    @Column(name = "title_vi", nullable = false, length = 255)
    private String titleVi;

    @Column(name = "title_en", nullable = false, length = 255)
    private String titleEn;

    @Column(name = "title_ja", nullable = false, length = 255)
    private String titleJa;

    @Column(name = "content_vi", nullable = false, columnDefinition = "TEXT")
    private String contentVi;

    @Column(name = "content_en", nullable = false, columnDefinition = "TEXT")
    private String contentEn;

    @Column(name = "content_ja", nullable = false, columnDefinition = "TEXT")
    private String contentJa;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 50)
    private TargetAudience targetAudience;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;
}
