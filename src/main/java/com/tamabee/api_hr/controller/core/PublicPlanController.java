package com.tamabee.api_hr.controller.core;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.response.company.PublicSettingsResponse;
import com.tamabee.api_hr.dto.response.wallet.PlanResponse;
import com.tamabee.api_hr.service.admin.interfaces.IPlanService;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;

import lombok.RequiredArgsConstructor;

/**
 * Controller công khai cho gói dịch vụ (Plan)
 * Không yêu cầu xác thực - dùng cho trang landing page
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PublicPlanController {

    private final IPlanService planService;
    private final ISettingService settingService;

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
     * Lấy public settings cho landing page
     * GET /api/plans/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<BaseResponse<PublicSettingsResponse>> getPublicSettings() {
        PublicSettingsResponse response = PublicSettingsResponse.builder()
                .freeTrialMonths(settingService.getFreeTrialMonths())
                .referralBonusMonths(settingService.getReferralBonusMonths())
                .customPricePerEmployee(settingService.getCustomPricePerEmployee())
                .build();
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
