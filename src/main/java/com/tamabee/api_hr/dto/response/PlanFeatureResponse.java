package com.tamabee.api_hr.dto.response;

import lombok.Data;

/**
 * Response DTO cho tính năng của gói dịch vụ
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
public class PlanFeatureResponse {

    private Long id;

    // Vietnamese
    private String featureVi;

    // English
    private String featureEn;

    // Japanese
    private String featureJa;

    // Thứ tự hiển thị
    private Integer sortOrder;

    // Feature nổi bật
    private Boolean isHighlighted;
}
