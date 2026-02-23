package com.tamabee.api_hr.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho trạng thái subscription của company
 * Bao gồm thông tin plan hiện tại, wallet, và danh sách plans có thể upgrade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {
    // Thông tin company
    private Long companyId;
    private String companyName;
    private String companyStatus;
    private Integer currentEmployeeCount;
    
    // Thông tin plan hiện tại
    private Long currentPlanId;
    private String currentPlanName;
    private BigDecimal currentPlanPrice;
    private Integer currentPlanMaxEmployees;
    
    // Thông tin wallet
    private BigDecimal walletBalance;
    private LocalDateTime freeTrialEndDate;
    private LocalDateTime nextBillingDate;
    private Boolean isInFreeTrial;
    
    // Số ngày còn lại trước khi bị xóa (nếu INACTIVE)
    private Integer daysUntilDeletion;
    
    // Scheduled plan change (downgrade)
    private Long scheduledPlanId;
    private String scheduledPlanName;
    private BigDecimal scheduledPlanPrice;
    private String scheduledPlanEffectiveDate; // Format: yyyy-MM-dd
    
    // Grace period cho upgrade cancellation (15 phút)
    private Boolean canCancelUpgrade;
    private LocalDateTime cancelUpgradeDeadline;
    private Long previousPlanId;
    private String previousPlanName;
    
    // Danh sách plans có thể chọn
    private List<PlanEligibilityResponse> availablePlans;
}
