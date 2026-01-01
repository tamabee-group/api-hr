package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.dto.request.AuditLogQueryRequest;
import com.tamabee.api_hr.dto.response.AuditLogResponse;
import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface cho Audit Log Service.
 * Quản lý việc ghi log các thay đổi trong hệ thống.
 */
public interface IAuditLogService {

    /**
     * Ghi log thay đổi attendance record.
     */
    void logAttendanceChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi payroll record.
     */
    void logPayrollChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi company settings.
     */
    void logSettingsChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi work schedule.
     */
    void logWorkScheduleChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi leave request.
     */
    void logLeaveRequestChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi adjustment request.
     */
    void logAdjustmentRequestChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi schedule selection.
     */
    void logScheduleSelectionChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log thay đổi holiday.
     */
    void logHolidayChange(Long companyId, Long entityId, AuditAction action,
            Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Ghi log chung cho bất kỳ entity type nào.
     */
    void logChange(Long companyId, AuditEntityType entityType, Long entityId,
            AuditAction action, Long userId, String userName,
            Object beforeValue, Object afterValue, String description);

    /**
     * Lấy audit log theo ID.
     */
    AuditLogResponse getAuditLogById(Long id);

    /**
     * Lấy danh sách audit logs theo company.
     */
    Page<AuditLogResponse> getAuditLogs(Long companyId, Pageable pageable);

    /**
     * Lấy danh sách audit logs với filter.
     */
    Page<AuditLogResponse> getAuditLogs(Long companyId, AuditLogQueryRequest request, Pageable pageable);

    /**
     * Lấy lịch sử thay đổi của một entity cụ thể.
     */
    List<AuditLogResponse> getEntityHistory(AuditEntityType entityType, Long entityId);

    /**
     * Lấy audit logs theo user.
     */
    Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable);
}
