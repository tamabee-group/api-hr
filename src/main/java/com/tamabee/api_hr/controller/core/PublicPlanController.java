package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.response.PlanResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller công khai cho gói dịch vụ (Plan)
 * Không yêu cầu xác thực - dùng cho trang landing page
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PublicPlanController {

    private final IPlanService planService;

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
}
