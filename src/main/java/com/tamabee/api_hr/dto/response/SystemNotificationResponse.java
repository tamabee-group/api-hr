package com.tamabee.api_hr.dto.response;

import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.TargetAudience;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho thông báo hệ thống (master copy).
 * Chứa nội dung 3 ngôn ngữ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationResponse {

    private Long id;

    private String titleVi;

    private String titleEn;

    private String titleJa;

    private String contentVi;

    private String contentEn;

    private String contentJa;

    private TargetAudience targetAudience;

    private String createdByName;

    private LocalDateTime createdAt;
}
