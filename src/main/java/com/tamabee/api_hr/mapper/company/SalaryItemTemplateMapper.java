package com.tamabee.api_hr.mapper.company;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.payroll.CreateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.response.payroll.SalaryItemTemplateResponse;
import com.tamabee.api_hr.entity.payroll.SalaryItemTemplateEntity;

/**
 * Mapper cho SalaryItemTemplate entities và DTOs.
 */
@Component
public class SalaryItemTemplateMapper {

    /**
     * Chuyển đổi từ Request DTO sang Entity
     */
    public SalaryItemTemplateEntity toEntity(CreateSalaryItemTemplateRequest request) {
        if (request == null) {
            return null;
        }

        SalaryItemTemplateEntity entity = new SalaryItemTemplateEntity();
        entity.setName(request.getName());
        entity.setType(request.getType());
        return entity;
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO
     */
    public SalaryItemTemplateResponse toResponse(SalaryItemTemplateEntity entity) {
        if (entity == null) {
            return null;
        }

        return SalaryItemTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .build();
    }

    /**
     * Chuyển đổi danh sách Entity sang danh sách Response DTO
     */
    public List<SalaryItemTemplateResponse> toResponseList(List<SalaryItemTemplateEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
