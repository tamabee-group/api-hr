package com.tamabee.api_hr.mapper.company;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.CreateWorkScheduleRequest;
import com.tamabee.api_hr.dto.request.UpdateWorkScheduleRequest;
import com.tamabee.api_hr.dto.response.WorkScheduleAssignmentResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.WorkScheduleAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import com.tamabee.api_hr.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa WorkSchedule Entity và DTO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkScheduleMapper {

    private final ObjectMapper objectMapper;

    /**
     * Chuyển CreateWorkScheduleRequest thành Entity
     */
    public WorkScheduleEntity toEntity(CreateWorkScheduleRequest request) {
        if (request == null) {
            return null;
        }

        WorkScheduleEntity entity = new WorkScheduleEntity();
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        entity.setDescription(request.getDescription());

        if (request.getScheduleData() != null) {
            entity.setScheduleData(serializeScheduleData(request.getScheduleData()));
        }

        return entity;
    }

    /**
     * Cập nhật Entity từ UpdateWorkScheduleRequest
     */
    public void updateEntity(WorkScheduleEntity entity, UpdateWorkScheduleRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getScheduleData() != null) {
            entity.setScheduleData(serializeScheduleData(request.getScheduleData()));
        }
    }

    /**
     * Chuyển Entity thành Response
     */
    public WorkScheduleResponse toResponse(WorkScheduleEntity entity) {
        if (entity == null) {
            return null;
        }

        return WorkScheduleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .isDefault(entity.getIsDefault())
                .scheduleData(deserializeScheduleData(entity.getScheduleData()))
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển Assignment Entity thành Response
     */
    public WorkScheduleAssignmentResponse toAssignmentResponse(
            WorkScheduleAssignmentEntity entity,
            String employeeName,
            String scheduleName) {
        if (entity == null) {
            return null;
        }

        return WorkScheduleAssignmentResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .scheduleId(entity.getScheduleId())
                .scheduleName(scheduleName)
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Serialize WorkScheduleData thành JSON string
     */
    private String serializeScheduleData(WorkScheduleData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize schedule data: {}", e.getMessage());
            throw new InternalServerException("Lỗi serialize schedule data", e);
        }
    }

    /**
     * Deserialize JSON string thành WorkScheduleData
     */
    public WorkScheduleData deserializeScheduleData(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, WorkScheduleData.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize schedule data: {}", e.getMessage());
            throw new InternalServerException("Lỗi deserialize schedule data", e);
        }
    }
}
