package com.tamabee.api_hr.mapper.company;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.payroll.AssignSalaryItemRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryItemResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryItemEntity;

/**
 * Mapper cho EmployeeSalaryItem entities và DTOs.
 */
@Component
public class EmployeeSalaryItemMapper {

    /**
     * Chuyển đổi từ Request DTO sang Entity
     */
    public EmployeeSalaryItemEntity toEntity(AssignSalaryItemRequest request, Long employeeId) {
        if (request == null) {
            return null;
        }

        EmployeeSalaryItemEntity entity = new EmployeeSalaryItemEntity();
        entity.setEmployeeId(employeeId);
        entity.setTemplateId(request.getTemplateId());
        entity.setAmount(request.getAmount());
        return entity;
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO
     */
    public EmployeeSalaryItemResponse toResponse(EmployeeSalaryItemEntity entity) {
        if (entity == null) {
            return null;
        }

        String templateName = null;
        var type = entity.getTemplate() != null ? entity.getTemplate().getType() : null;
        if (entity.getTemplate() != null) {
            templateName = entity.getTemplate().getName();
        }

        return EmployeeSalaryItemResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .templateId(entity.getTemplateId())
                .templateName(templateName)
                .type(type)
                .amount(entity.getAmount())
                .build();
    }

    /**
     * Chuyển đổi danh sách Entity sang danh sách Response DTO
     */
    public List<EmployeeSalaryItemResponse> toResponseList(List<EmployeeSalaryItemEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
