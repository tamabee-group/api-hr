package com.tamabee.api_hr.entity.audit;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ audit log cho các thay đổi trong hệ thống.
 * Ghi lại mọi thay đổi đối với attendance, payroll, và settings.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity_type", columnList = "entityType"),
        @Index(name = "idx_audit_entity_id", columnList = "entityId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_user_id", columnList = "userId"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_entity_type_id", columnList = "entityType, entityId")
})
public class AuditLogEntity extends BaseEntity {

    // Loại entity được audit
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEntityType entityType;

    // ID của entity được audit
    @Column(nullable = false)
    private Long entityId;

    // Hành động thực hiện
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    // ID người thực hiện
    @Column(nullable = false)
    private Long userId;

    // Tên người thực hiện (lưu snapshot để tránh join)
    @Column(nullable = false)
    private String userName;

    // Thời gian thực hiện
    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Giá trị trước khi thay đổi (JSON)
    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    // Giá trị sau khi thay đổi (JSON)
    @Column(columnDefinition = "TEXT")
    private String afterValue;

    // Mô tả thay đổi
    @Column(length = 500)
    private String description;

    // IP address của người thực hiện (optional)
    private String ipAddress;

    // User agent (optional)
    @Column(length = 500)
    private String userAgent;
}
