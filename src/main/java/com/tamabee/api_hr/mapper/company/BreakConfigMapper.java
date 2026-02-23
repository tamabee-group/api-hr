package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.dto.response.attendance.BreakConfigResponse;

/**
 * Mapper chuyển đổi giữa BreakConfig và DTOs
 */
@Component
public class BreakConfigMapper {

    /**
     * Chuyển BreakConfig sang response
     */
    public BreakConfigResponse toResponse(BreakConfig config) {
        if (config == null) {
            return null;
        }

        return BreakConfigResponse.builder()
                .breakEnabled(config.getBreakEnabled())
                .defaultBreakMinutes(config.getDefaultBreakMinutes())
                .maxBreaksPerDay(config.getMaxBreaksPerDay())
                .build();
    }

    /**
     * Tạo BreakConfig mới từ request
     */
    public BreakConfig toConfig(BreakConfigRequest request) {
        if (request == null) {
            return BreakConfig.builder().build();
        }

        return BreakConfig.builder()
                .breakEnabled(request.getBreakEnabled() != null ? request.getBreakEnabled() : true)
                .defaultBreakMinutes(request.getDefaultBreakMinutes() != null ? request.getDefaultBreakMinutes() : 60)
                .maxBreaksPerDay(request.getMaxBreaksPerDay() != null ? request.getMaxBreaksPerDay() : 3)
                .build();
    }

    /**
     * Cập nhật BreakConfig từ request (chỉ cập nhật các field không null)
     */
    public void updateConfig(BreakConfig config, BreakConfigRequest request) {
        if (config == null || request == null) {
            return;
        }

        if (request.getBreakEnabled() != null) {
            config.setBreakEnabled(request.getBreakEnabled());
        }
        if (request.getDefaultBreakMinutes() != null) {
            config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getMaxBreaksPerDay() != null) {
            config.setMaxBreaksPerDay(request.getMaxBreaksPerDay());
        }
    }
}
