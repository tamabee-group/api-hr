package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.FeatureCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO cho danh sách feature codes của một plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeaturesResponse {

    private Long planId;
    private String planName;
    private Set<FeatureCode> features;
}
