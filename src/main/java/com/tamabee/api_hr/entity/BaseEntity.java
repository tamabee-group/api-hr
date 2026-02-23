package com.tamabee.api_hr.entity;

import java.time.LocalDateTime;

import com.tamabee.api_hr.config.RegionAwareAuditListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

/**
 * Base entity chỉ chứa id, createdAt, updatedAt.
 * Field deleted được thêm riêng vào từng entity cần soft delete.
 *
 * Sử dụng RegionAwareAuditListener thay vì AuditingEntityListener
 * để inject timezone từ company region vào createdAt/updatedAt.
 * Timezone được xác định từ RegionContext (set bởi JwtAuthenticationFilter từ JWT claim "region").
 * Fallback: UTC nếu không có region (system operation, scheduler).
 */
@Data
@MappedSuperclass
@EntityListeners(RegionAwareAuditListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
