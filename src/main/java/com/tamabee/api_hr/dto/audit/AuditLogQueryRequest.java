package com.tamabee.api_hr.dto.audit;

import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO cho query audit logs.
 */
@Data
public class AuditLogQueryRequest {
    private AuditEntityType entityType;
    private AuditAction action;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
