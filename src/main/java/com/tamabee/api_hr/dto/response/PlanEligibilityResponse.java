package com.tamabee.api_hr.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho plan với thông tin eligibility
 * Dùng để hiển thị danh sách plans và cho biết plan nào có thể chọn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanEligibilityResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private Integer maxEmployees;
    private Boolean isActive;
    
    // Eligibility info
    private Boolean eligible;
    private String ineligibleReason; // null nếu eligible
}
