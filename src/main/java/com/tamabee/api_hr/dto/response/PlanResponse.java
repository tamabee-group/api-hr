package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho gói dịch vụ
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
public class PlanResponse {

    private Long id;

    // Vietnamese
    private String nameVi;
    private String descriptionVi;

    // English
    private String nameEn;
    private String descriptionEn;

    // Japanese
    private String nameJa;
    private String descriptionJa;

    // Common fields
    private BigDecimal monthlyPrice;
    private Integer maxEmployees;
    private Boolean isActive;

    // Danh sách features
    private List<PlanFeatureResponse> features;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
