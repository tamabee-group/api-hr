package com.tamabee.api_hr.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.response.CommissionSettingsResponse;
import com.tamabee.api_hr.dto.response.wallet.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.admin.interfaces.IEmployeeReferralService;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho Employee Tamabee xem và theo dõi company đã giới thiệu
 * Chỉ EMPLOYEE_TAMABEE có quyền truy cập endpoint /api/employee/referrals
 * ADMIN_TAMABEE và MANAGER_TAMABEE có quyền xem referrals của employee khác
 */
@RestController
@RequiredArgsConstructor
public class EmployeeReferralController {

    private final IEmployeeReferralService employeeReferralService;
    private final ISettingService settingService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách companies đã giới thiệu của chính mình (phân trang)
     * GET /api/employee/referrals
     * Chỉ EMPLOYEE_TAMABEE có quyền truy cập
     */
    @GetMapping("/api/employee/referrals")
    @PreAuthorize(RoleConstants.HAS_EMPLOYEE_TAMABEE)
    public ResponseEntity<BaseResponse<Page<ReferredCompanyResponse>>> getReferredCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String employeeCode = getCurrentUserEmployeeCode();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReferredCompanyResponse> companies = employeeReferralService.getReferredCompanies(employeeCode, pageable);
        return ResponseEntity.ok(BaseResponse.success(companies));
    }

    /**
     * Lấy danh sách companies đã giới thiệu của employee khác (phân trang)
     * GET /api/admin/employees/{employeeId}/referrals
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền truy cập
     */
    @GetMapping("/api/admin/employees/{employeeId}/referrals")
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<Page<ReferredCompanyResponse>>> getEmployeeReferrals(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReferredCompanyResponse> companies = employeeReferralService.getReferredCompaniesByEmployeeId(employeeId,
                pageable);
        return ResponseEntity.ok(BaseResponse.success(companies));
    }

    /**
     * Lấy thống kê commission của employee
     * GET /api/admin/employees/{employeeId}/commission-summary
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền truy cập
     */
    @GetMapping("/api/admin/employees/{employeeId}/commission-summary")
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse>> getEmployeeCommissionSummary(
            @PathVariable Long employeeId) {
        var summary = employeeReferralService.getCommissionSummaryByEmployeeId(employeeId);
        return ResponseEntity.ok(BaseResponse.success(summary));
    }

    /**
     * Lấy thông tin cấu hình hoa hồng
     * GET /api/admin/commission-settings
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền truy cập
     */
    @GetMapping("/api/admin/commission-settings")
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<CommissionSettingsResponse>> getCommissionSettings() {
        CommissionSettingsResponse settings = CommissionSettingsResponse.builder()
                .commissionAmount(settingService.getCommissionAmount())
                .referralBonusMonths(settingService.getReferralBonusMonths())
                .freeTrialMonths(settingService.getFreeTrialMonths())
                .build();
        return ResponseEntity.ok(BaseResponse.success(settings));
    }

    /**
     * Lấy employeeCode của user hiện tại từ JWT token
     */
    private String getCurrentUserEmployeeCode() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        return user.getEmployeeCode();
    }
}
