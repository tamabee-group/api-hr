package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.FeatureCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho danh sách feature codes của một plan.
 * Dùng cho API GET /api/plans/{planId}/features và GET /api/plans/all-features
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeaturesResponse {

    private Long planId;
    private String planName;
    private List<FeatureItem> features;

    /**
     * DTO cho từng feature item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureItem {
        private FeatureCode code;
        private String name;
        private boolean enabled;
    }
}
