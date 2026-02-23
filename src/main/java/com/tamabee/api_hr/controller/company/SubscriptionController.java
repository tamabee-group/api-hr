package com.tamabee.api_hr.controller.company;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.ChangePlanRequest;
import com.tamabee.api_hr.dto.response.PlanChangeHistoryResponse;
import com.tamabee.api_hr.dto.response.SubscriptionStatusResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.ISubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý subscription của company
 */
@RestController
@RequestMapping("/api/company/subscription")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
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
     * Lấy lịch sử thay đổi plan
     */
    @GetMapping("/history")
    public ResponseEntity<BaseResponse<List<PlanChangeHistoryResponse>>> getPlanChangeHistory(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        List<PlanChangeHistoryResponse> response = subscriptionService.getPlanChangeHistory(language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Đổi gói dịch vụ (chỉ Admin/Manager)
     */
    @PostMapping("/change-plan")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
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
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<SubscriptionStatusResponse>> reactivate(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        SubscriptionStatusResponse response = subscriptionService.reactivate(language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Hủy upgrade gần nhất (trong grace period 15 phút)
     * Cho phép user hủy nếu lỡ nâng cấp nhầm
     */
    @PostMapping("/cancel-upgrade")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<SubscriptionStatusResponse>> cancelUpgrade(
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
        SubscriptionStatusResponse response = subscriptionService.cancelUpgrade(language);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
