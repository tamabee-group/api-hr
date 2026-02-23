package com.tamabee.api_hr.mapper.admin;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.CreateSystemNotificationRequest;
import com.tamabee.api_hr.dto.response.SystemNotificationResponse;
import com.tamabee.api_hr.entity.core.SystemNotificationEntity;

/**
 * Mapper cho SystemNotification entity.
 * Chuyển đổi giữa Entity, Request DTO và Response DTO.
 */
@Component
public class SystemNotificationMapper {

    /**
     * Chuyển đổi CreateSystemNotificationRequest sang SystemNotificationEntity.
     *
     * @param request         Request DTO
     * @param createdByUserId ID người tạo (từ JWT token)
     * @param createdByName   Tên người tạo
     * @return SystemNotificationEntity
     */
    public SystemNotificationEntity toEntity(CreateSystemNotificationRequest request,
                                              Long createdByUserId, String createdByName) {
        if (request == null) {
            return null;
        }

        return SystemNotificationEntity.builder()
                .titleVi(request.getTitleVi())
                .titleEn(request.getTitleEn())
                .titleJa(request.getTitleJa())
                .contentVi(request.getContentVi())
                .contentEn(request.getContentEn())
                .contentJa(request.getContentJa())
                .targetAudience(request.getTargetAudience())
                .createdByUserId(createdByUserId)
                .createdByName(createdByName)
                .build();
    }

    /**
     * Chuyển đổi SystemNotificationEntity sang SystemNotificationResponse.
     *
     * @param entity Entity cần chuyển đổi
     * @return SystemNotificationResponse DTO hoặc null nếu entity là null
     */
    public SystemNotificationResponse toResponse(SystemNotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        return SystemNotificationResponse.builder()
                .id(entity.getId())
                .titleVi(entity.getTitleVi())
                .titleEn(entity.getTitleEn())
                .titleJa(entity.getTitleJa())
                .contentVi(entity.getContentVi())
                .contentEn(entity.getContentEn())
                .contentJa(entity.getContentJa())
                .targetAudience(entity.getTargetAudience())
                .createdByName(entity.getCreatedByName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
