package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.request.SettingUpdateRequest;
import com.tamabee.api_hr.dto.response.SettingResponse;
import com.tamabee.api_hr.entity.wallet.TamabeeSettingEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper cho TamabeeSetting entity
 * Chuyển đổi giữa Entity, Request DTO và Response DTO
 */
@Component
public class TamabeeSettingMapper {

    /**
     * Cập nhật TamabeeSettingEntity từ SettingUpdateRequest
     * Chỉ cập nhật các trường được gửi (không null)
     */
    public void updateEntity(TamabeeSettingEntity entity, SettingUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getSettingValue() != null) {
            entity.setSettingValue(request.getSettingValue());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }

    /**
     * Chuyển đổi TamabeeSettingEntity sang SettingResponse
     */
    public SettingResponse toResponse(TamabeeSettingEntity entity) {
        if (entity == null) {
            return null;
        }

        SettingResponse response = new SettingResponse();
        response.setId(entity.getId());
        response.setSettingKey(entity.getSettingKey());
        response.setSettingValue(entity.getSettingValue());
        response.setDescription(entity.getDescription());
        response.setValueType(entity.getValueType());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}
