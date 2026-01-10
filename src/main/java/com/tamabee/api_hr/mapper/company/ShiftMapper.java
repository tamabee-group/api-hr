package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.attendance.ShiftAssignmentRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftSwapRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftTemplateRequest;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftTemplateResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftSwapRequestEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper cho Shift entities và DTOs.
 */
@Component
public class ShiftMapper {

    // ==================== Shift Template ====================

    public ShiftTemplateEntity toEntity(ShiftTemplateRequest request) {
        if (request == null) {
            return null;
        }

        ShiftTemplateEntity entity = new ShiftTemplateEntity();
        entity.setName(request.getName());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setBreakMinutes(request.getBreakMinutes());
        entity.setMultiplier(request.getMultiplier());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return entity;
    }

    public void updateEntity(ShiftTemplateEntity entity, ShiftTemplateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setName(request.getName());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setBreakMinutes(request.getBreakMinutes());
        entity.setMultiplier(request.getMultiplier());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    public ShiftTemplateResponse toResponse(ShiftTemplateEntity entity) {
        if (entity == null) {
            return null;
        }

        ShiftTemplateResponse response = new ShiftTemplateResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setStartTime(entity.getStartTime());
        response.setEndTime(entity.getEndTime());
        response.setBreakMinutes(entity.getBreakMinutes());
        response.setMultiplier(entity.getMultiplier());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    // ==================== Shift Assignment ====================

    public ShiftAssignmentEntity toEntity(ShiftAssignmentRequest request) {
        if (request == null) {
            return null;
        }

        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setShiftTemplateId(request.getShiftTemplateId());
        entity.setWorkDate(request.getWorkDate());

        return entity;
    }

    public ShiftAssignmentResponse toResponse(ShiftAssignmentEntity entity,
            UserEntity employee,
            ShiftTemplateEntity shiftTemplate,
            UserEntity swappedWithEmployee) {
        if (entity == null) {
            return null;
        }

        ShiftAssignmentResponse response = new ShiftAssignmentResponse();
        response.setId(entity.getId());
        response.setEmployeeId(entity.getEmployeeId());
        response.setEmployeeName(
                employee != null && employee.getProfile() != null ? employee.getProfile().getName() : null);
        response.setShiftTemplateId(entity.getShiftTemplateId());
        response.setShiftTemplate(toResponse(shiftTemplate));
        response.setWorkDate(entity.getWorkDate());
        response.setStatus(entity.getStatus());
        response.setSwappedWithEmployeeId(entity.getSwappedWithEmployeeId());
        response.setSwappedWithEmployeeName(swappedWithEmployee != null && swappedWithEmployee.getProfile() != null
                ? swappedWithEmployee.getProfile().getName()
                : null);
        response.setSwappedFromAssignmentId(entity.getSwappedFromAssignmentId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    // ==================== Shift Swap Request ====================

    public ShiftSwapRequestEntity toEntity(ShiftSwapRequest request, Long requesterId) {
        if (request == null) {
            return null;
        }

        ShiftSwapRequestEntity entity = new ShiftSwapRequestEntity();
        entity.setRequesterId(requesterId);
        entity.setTargetEmployeeId(request.getTargetEmployeeId());
        entity.setRequesterAssignmentId(request.getRequesterAssignmentId());
        entity.setTargetAssignmentId(request.getTargetAssignmentId());

        return entity;
    }

    public ShiftSwapRequestResponse toResponse(ShiftSwapRequestEntity entity,
            UserEntity requester,
            UserEntity targetEmployee,
            ShiftAssignmentResponse requesterAssignment,
            ShiftAssignmentResponse targetAssignment,
            UserEntity approver) {
        if (entity == null) {
            return null;
        }

        ShiftSwapRequestResponse response = new ShiftSwapRequestResponse();
        response.setId(entity.getId());
        response.setRequesterId(entity.getRequesterId());
        response.setRequesterName(
                requester != null && requester.getProfile() != null ? requester.getProfile().getName() : null);
        response.setTargetEmployeeId(entity.getTargetEmployeeId());
        response.setTargetEmployeeName(
                targetEmployee != null && targetEmployee.getProfile() != null ? targetEmployee.getProfile().getName()
                        : null);
        response.setRequesterAssignmentId(entity.getRequesterAssignmentId());
        response.setRequesterAssignment(requesterAssignment);
        response.setTargetAssignmentId(entity.getTargetAssignmentId());
        response.setTargetAssignment(targetAssignment);
        response.setReason(entity.getReason());
        response.setStatus(entity.getStatus());
        response.setApprovedBy(entity.getApprovedBy());
        response.setApproverName(
                approver != null && approver.getProfile() != null ? approver.getProfile().getName() : null);
        response.setApprovedAt(entity.getApprovedAt());
        response.setRejectionReason(entity.getRejectionReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}
