package com.tamabee.api_hr.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO để tạo gói dịch vụ mới
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
public class PlanCreateRequest {

    // Vietnamese
    @NotBlank(message = "Tên tiếng Việt không được để trống")
    private String nameVi;

    private String descriptionVi;

    // English
    @NotBlank(message = "Tên tiếng Anh không được để trống")
    private String nameEn;

    private String descriptionEn;

    // Japanese
    @NotBlank(message = "Tên tiếng Nhật không được để trống")
    private String nameJa;

    private String descriptionJa;

    // Common fields
    @NotNull(message = "Giá hàng tháng không được để trống")
    @Positive(message = "Giá hàng tháng phải lớn hơn 0")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Số nhân viên tối đa không được để trống")
    @Positive(message = "Số nhân viên tối đa phải lớn hơn 0")
    private Integer maxEmployees;

    private Boolean isActive = true;

    // Danh sách features
    @Valid
    private List<PlanFeatureRequest> features;
}
