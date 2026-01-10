package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho public settings
 * Dùng cho landing page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSettingsResponse {
    private int freeTrialMonths;
    private int referralBonusMonths;
    private int customPricePerEmployee;
}
