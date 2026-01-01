package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.request.*;
import com.tamabee.api_hr.dto.response.BreakConfigResponse;
import com.tamabee.api_hr.dto.response.CompanySettingsResponse;
import com.tamabee.api_hr.dto.response.OvertimeConfigResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.BreakConfigMapper;
import com.tamabee.api_hr.mapper.company.OvertimeConfigMapper;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý cấu hình chấm công và tính lương của công ty.
 * Chỉ ADMIN_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/settings")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
public class CompanySettingsController {

    private final ICompanySettingsService companySettingsService;
    private final UserRepository userRepository;
    private final BreakConfigMapper breakConfigMapper;
    private final OvertimeConfigMapper overtimeConfigMapper;

    /**
     * Lấy toàn bộ cấu hình của công ty
     */
    @GetMapping
    public ResponseEntity<BaseResponse<CompanySettingsResponse>> getSettings() {
        Long companyId = getCurrentUserCompanyId();
        CompanySettingsResponse settings = companySettingsService.getSettings(companyId);
        return ResponseEntity.ok(BaseResponse.success(settings, "Lấy cấu hình công ty thành công"));
    }

    /**
     * Cập nhật cấu hình chấm công
     */
    @PutMapping("/attendance")
    public ResponseEntity<BaseResponse<AttendanceConfig>> updateAttendanceConfig(
            @Valid @RequestBody AttendanceConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        AttendanceConfig config = companySettingsService.updateAttendanceConfig(companyId, request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình chấm công thành công"));
    }

    /**
     * Cập nhật cấu hình tính lương
     */
    @PutMapping("/payroll")
    public ResponseEntity<BaseResponse<PayrollConfig>> updatePayrollConfig(
            @Valid @RequestBody PayrollConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        PayrollConfig config = companySettingsService.updatePayrollConfig(companyId, request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình tính lương thành công"));
    }

    /**
     * Cập nhật cấu hình tăng ca
     */
    @PutMapping("/overtime")
    public ResponseEntity<BaseResponse<OvertimeConfigResponse>> updateOvertimeConfig(
            @Valid @RequestBody OvertimeConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        OvertimeConfig config = companySettingsService.updateOvertimeConfig(companyId, request);
        OvertimeConfigResponse response = overtimeConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật cấu hình tăng ca thành công"));
    }

    /**
     * Lấy cấu hình tăng ca
     */
    @GetMapping("/overtime")
    public ResponseEntity<BaseResponse<OvertimeConfigResponse>> getOvertimeConfig() {
        Long companyId = getCurrentUserCompanyId();
        OvertimeConfig config = companySettingsService.getOvertimeConfig(companyId);
        OvertimeConfigResponse response = overtimeConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy cấu hình tăng ca thành công"));
    }

    /**
     * Cập nhật cấu hình giờ giải lao
     */
    @PutMapping("/break")
    public ResponseEntity<BaseResponse<BreakConfigResponse>> updateBreakConfig(
            @Valid @RequestBody BreakConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        BreakConfig config = companySettingsService.updateBreakConfig(companyId, request);
        BreakConfigResponse response = breakConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật cấu hình giờ giải lao thành công"));
    }

    /**
     * Lấy cấu hình giờ giải lao
     */
    @GetMapping("/break")
    public ResponseEntity<BaseResponse<BreakConfigResponse>> getBreakConfig() {
        Long companyId = getCurrentUserCompanyId();
        BreakConfig config = companySettingsService.getBreakConfig(companyId);
        BreakConfigResponse response = breakConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy cấu hình giờ giải lao thành công"));
    }

    /**
     * Cập nhật cấu hình phụ cấp
     */
    @PutMapping("/allowance")
    public ResponseEntity<BaseResponse<AllowanceConfig>> updateAllowanceConfig(
            @Valid @RequestBody AllowanceConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        AllowanceConfig config = companySettingsService.updateAllowanceConfig(companyId, request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình phụ cấp thành công"));
    }

    /**
     * Cập nhật cấu hình khấu trừ
     */
    @PutMapping("/deduction")
    public ResponseEntity<BaseResponse<DeductionConfig>> updateDeductionConfig(
            @Valid @RequestBody DeductionConfigRequest request) {
        Long companyId = getCurrentUserCompanyId();
        DeductionConfig config = companySettingsService.updateDeductionConfig(companyId, request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình khấu trừ thành công"));
    }

    /**
     * Lấy companyId của user đang đăng nhập
     */
    private Long getCurrentUserCompanyId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getCompanyId();
    }
}
