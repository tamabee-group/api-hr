package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.CreateHolidayRequest;
import com.tamabee.api_hr.dto.request.UpdateHolidayRequest;
import com.tamabee.api_hr.dto.response.HolidayResponse;
import com.tamabee.api_hr.entity.leave.HolidayEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa HolidayEntity và DTO.
 */
@Component
public class HolidayMapper {

    /**
     * Chuyển đổi entity sang response
     *
     * @param entity entity cần chuyển đổi
     * @return response
     */
    public HolidayResponse toResponse(HolidayEntity entity) {
        if (entity == null) {
            return null;
        }

        return HolidayResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .date(entity.getDate())
                .name(entity.getName())
                .type(entity.getType())
                .isPaid(entity.getIsPaid())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi request sang entity
     *
     * @param companyId ID công ty
     * @param request   request tạo mới
     * @return entity
     */
    public HolidayEntity toEntity(Long companyId, CreateHolidayRequest request) {
        if (request == null) {
            return null;
        }

        HolidayEntity entity = new HolidayEntity();
        entity.setCompanyId(companyId);
        entity.setDate(request.getDate());
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setIsPaid(request.getIsPaid() != null ? request.getIsPaid() : true);
        entity.setDescription(request.getDescription());
        return entity;
    }

    /**
     * Cập nhật entity từ request
     *
     * @param entity  entity cần cập nhật
     * @param request request cập nhật
     */
    public void updateEntity(HolidayEntity entity, UpdateHolidayRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getDate() != null) {
            entity.setDate(request.getDate());
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getIsPaid() != null) {
            entity.setIsPaid(request.getIsPaid());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}
