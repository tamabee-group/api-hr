package com.tamabee.api_hr.service.core.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.audit.AuditLogQueryRequest;
import com.tamabee.api_hr.dto.response.audit.AuditLogResponse;
import com.tamabee.api_hr.entity.audit.AuditLogEntity;
import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.audit.AuditLogRepository;
import com.tamabee.api_hr.service.core.interfaces.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của IAuditLogService.
 * Ghi log các thay đổi trong hệ thống để audit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional
    public void logAttendanceChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.ATTENDANCE_RECORD, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logPayrollChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.PAYROLL_RECORD, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logSettingsChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.COMPANY_SETTINGS, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logWorkScheduleChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.WORK_SCHEDULE, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logLeaveRequestChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.LEAVE_REQUEST, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logAdjustmentRequestChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.ADJUSTMENT_REQUEST, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logScheduleSelectionChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.SCHEDULE_SELECTION, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Async
    @Transactional
    public void logHolidayChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        logChange(companyId, AuditEntityType.HOLIDAY, entityId, action,
                userId, userName, beforeValue, afterValue, description);
    }

    @Override
    @Transactional
    public void logChange(Long companyId, AuditEntityType entityType, Long entityId,
            AuditAction action, Long userId, String userName,
            Object beforeValue, Object afterValue, String description) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setUserId(userId);
            auditLog.setUserName(userName);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(description);

            // Serialize before/after values to JSON
            if (beforeValue != null) {
                auditLog.setBeforeValue(serializeToJson(beforeValue));
            }
            if (afterValue != null) {
                auditLog.setAfterValue(serializeToJson(afterValue));
            }

            auditLogRepository.save(auditLog);

            log.debug("Audit log created: entityType={}, entityId={}, action={}, userId={}",
                    entityType, entityId, action, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log: entityType={}, entityId={}, action={}",
                    entityType, entityId, action, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        AuditLogEntity entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.AUDIT_LOG_NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Long companyId, Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Long companyId, AuditLogQueryRequest request, Pageable pageable) {
        return auditLogRepository.findByFilters(
                request.getEntityType(),
                request.getAction(),
                request.getUserId(),
                request.getStartTime(),
                request.getEndTime(),
                pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(AuditEntityType entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Serialize object to JSON string.
     */
    private String serializeToJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return value.toString();
        }
    }

    /**
     * Convert entity to response DTO.
     */
    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .timestamp(entity.getTimestamp())
                .beforeValue(entity.getBeforeValue())
                .afterValue(entity.getAfterValue())
                .description(entity.getDescription())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
