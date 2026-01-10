package com.tamabee.api_hr.service.core.interfaces;

import com.tamabee.api_hr.dto.response.wallet.PlanFeaturesResponse;

/**
 * Service lấy danh sách features của plan.
 * Dùng cho frontend để render sidebar động theo plan.
 */
public interface IPlanFeaturesService {

    /**
     * Lấy danh sách features của plan theo planId
     * 
     * @param planId ID của plan
     * @return PlanFeaturesResponse chứa planId, planName và danh sách features
     */
    PlanFeaturesResponse getFeaturesByPlanId(Long planId);

    /**
     * Lấy tất cả features với enabled = true.
     * Dùng cho Tamabee users (không có planId).
     * 
     * @return PlanFeaturesResponse với tất cả features enabled
     */
    PlanFeaturesResponse getAllFeaturesEnabled();
}
