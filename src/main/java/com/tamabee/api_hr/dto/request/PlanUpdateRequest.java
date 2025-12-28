package com.tamabee.api_hr.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO để cập nhật gói dịch vụ
 * Tất cả các trường đều optional, chỉ cập nhật các trường được gửi
 */
@Data
public class PlanUpdateRequest {

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
    @Positive(message = "Giá hàng tháng phải lớn hơn 0")
    private BigDecimal monthlyPrice;

    @Positive(message = "Số nhân viên tối đa phải lớn hơn 0")
    private Integer maxEmployees;

    private Boolean isActive;

    // Danh sách features (nếu có sẽ thay thế toàn bộ features cũ)
    @Valid
    private List<PlanFeatureRequest> features;
}
