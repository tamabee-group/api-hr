package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.entity.company.BreakSettingEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Mapper chuyển đổi giữa BreakSettingEntity và BreakConfig DTO.
 */
@Slf4j
@Component
public class BreakSettingMapper {

    /**
     * Tạo entity mới với default values
     */
    public BreakSettingEntity toEntity() {
        BreakSettingEntity entity = new BreakSettingEntity();
        entity.setBreakEnabled(true);
        entity.setDefaultBreakMinutes(60);
        entity.setMaxBreaksPerDay(3);
        return entity;
    }

    /**
     * Chuyển BreakSettingEntity sang BreakConfig DTO
     */
    public BreakConfig toResponse(BreakSettingEntity entity) {
        if (entity == null) {
            return BreakConfig.builder().build();
        }

        return BreakConfig.builder()
                .breakEnabled(entity.getBreakEnabled())
                .defaultBreakMinutes(entity.getDefaultBreakMinutes())
                .maxBreaksPerDay(entity.getMaxBreaksPerDay())
                .build();
    }

    /**
     * Cập nhật entity từ request (chỉ cập nhật các field không null)
     */
    public void updateEntity(BreakSettingEntity entity, BreakConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getBreakEnabled() != null) {
            entity.setBreakEnabled(request.getBreakEnabled());
        }
        if (request.getDefaultBreakMinutes() != null) {
            entity.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getMaxBreaksPerDay() != null) {
            entity.setMaxBreaksPerDay(request.getMaxBreaksPerDay());
        }
    }
}
