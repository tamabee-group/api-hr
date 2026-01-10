package com.tamabee.api_hr.dto.request.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO cho tính năng của gói dịch vụ
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
public class PlanFeatureRequest {

    // ID của feature (null nếu tạo mới, có giá trị nếu cập nhật)
    private Long id;

    // Vietnamese
    @NotBlank(message = "Tính năng tiếng Việt không được để trống")
    private String featureVi;

    // English
    @NotBlank(message = "Tính năng tiếng Anh không được để trống")
    private String featureEn;

    // Japanese
    @NotBlank(message = "Tính năng tiếng Nhật không được để trống")
    private String featureJa;

    // Thứ tự hiển thị
    @NotNull(message = "Thứ tự hiển thị không được để trống")
    private Integer sortOrder;

    // Feature nổi bật
    private Boolean isHighlighted = false;
}
