package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.dto.response.PlanResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IPlanService;
import com.tamabee.api_hr.service.core.IPlanFeaturesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller công khai cho gói dịch vụ (Plan)
 * Không yêu cầu xác thực - dùng cho trang landing page và sidebar
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PublicPlanController {

    private final IPlanService planService;
    private final IPlanFeaturesService planFeaturesService;

    /**
     * Lấy danh sách plans đang active
     * GET /api/plans/active
     * Sắp xếp theo giá tăng dần
     */
    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<PlanResponse>>> getActivePlans() {
        List<PlanResponse> plans = planService.getActivePlans();
        return ResponseEntity.ok(BaseResponse.success(plans));
    }

    /**
     * Lấy danh sách features của plan theo planId.
     * GET /api/plans/{planId}/features
     * Dùng cho frontend để render sidebar động.
     */
    @GetMapping("/{planId}/features")
    public ResponseEntity<BaseResponse<PlanFeaturesResponse>> getFeatures(@PathVariable Long planId) {
        PlanFeaturesResponse response = planFeaturesService.getFeaturesByPlanId(planId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy tất cả features với enabled = true.
     * GET /api/plans/all-features
     * Dùng cho Tamabee users (không có planId).
     */
    @GetMapping("/all-features")
    public ResponseEntity<BaseResponse<PlanFeaturesResponse>> getAllFeatures() {
        PlanFeaturesResponse response = planFeaturesService.getAllFeaturesEnabled();
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
