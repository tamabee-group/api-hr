package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO cho audit log.
 */
@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private Long companyId;
    private AuditEntityType entityType;
    private Long entityId;
    private AuditAction action;
    private Long userId;
    private String userName;
    private LocalDateTime timestamp;
    private String beforeValue;
    private String afterValue;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;
}
