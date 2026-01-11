package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.ChangePlanRequest;
import com.tamabee.api_hr.dto.response.SubscriptionStatusResponse;
import com.tamabee.api_hr.service.company.interfaces.ISubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý subscription của company
 */
@RestController
@RequestMapping("/api/company/subscription")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'EMPLOYEE_COMPANY')")
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;

    /**
     * Lấy thông tin subscription hiện tại
     */
    @GetMapping
    public ResponseEntity<BaseResponse<SubscriptionStatusResponse>> getSubscriptionStatus(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        SubscriptionStatusResponse response = subscriptionService.getSubscriptionStatus(language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Đổi gói dịch vụ (chỉ Admin/Manager)
     */
    @PostMapping("/change-plan")
    @PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY')")
    public ResponseEntity<BaseResponse<SubscriptionStatusResponse>> changePlan(
            @Valid @RequestBody ChangePlanRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        SubscriptionStatusResponse response = subscriptionService.changePlan(request.getPlanId(), language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Kích hoạt lại tài khoản sau khi nạp tiền (chỉ Admin)
     * User có thể gọi API này nếu auto-reactivate không hoạt động
     */
    @PostMapping("/reactivate")
    @PreAuthorize("hasRole('ADMIN_COMPANY')")
    public ResponseEntity<BaseResponse<SubscriptionStatusResponse>> reactivate(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        SubscriptionStatusResponse response = subscriptionService.reactivate(language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
