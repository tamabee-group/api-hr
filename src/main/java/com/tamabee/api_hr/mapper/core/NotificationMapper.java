package com.tamabee.api_hr.mapper.core;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.response.NotificationResponse;
import com.tamabee.api_hr.entity.core.NotificationEntity;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi giữa NotificationEntity và NotificationResponse DTO.
 */
@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi NotificationEntity sang NotificationResponse DTO.
     * Parse JSON params string thành Map<String, Object>.
     * 
     * @param entity NotificationEntity cần chuyển đổi
     * @return NotificationResponse DTO hoặc null nếu entity là null
     */
    public NotificationResponse toResponse(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .params(parseParams(entity.getParams()))
                .targetUrl(entity.getTargetUrl())
                .type(entity.getType())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .title(entity.getTitle())
                .content(entity.getContent())
                .systemNotificationId(entity.getSystemNotificationId())
                .build();
    }

    /**
     * Parse JSON params string thành Map<String, Object>.
     * Xử lý gracefully khi params là null hoặc JSON không hợp lệ.
     * 
     * @param paramsJson JSON string chứa params
     * @return Map<String, Object> hoặc empty map nếu null/invalid
     */
    private Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // Trả về empty map nếu JSON không hợp lệ
            return Collections.emptyMap();
        }
    }
}
