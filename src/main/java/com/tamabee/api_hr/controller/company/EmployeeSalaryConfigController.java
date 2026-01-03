package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.SalaryConfigRequest;
import com.tamabee.api_hr.dto.response.EmployeeSalaryConfigResponse;
import com.tamabee.api_hr.dto.response.SalaryConfigValidationResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.company.IEmployeeSalaryConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý cấu hình lương nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/employees/{employeeId}/salary-config")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class EmployeeSalaryConfigController {

    private final IEmployeeSalaryConfigService salaryConfigService;

    /**
     * Lấy cấu hình lương hiện tại của nhân viên
     * GET /api/company/employees/{employeeId}/salary-config/current
     */
    @GetMapping("/current")
    public ResponseEntity<BaseResponse<EmployeeSalaryConfigResponse>> getCurrentSalaryConfig(
            @PathVariable Long employeeId) {
        EmployeeSalaryConfigResponse response = salaryConfigService.getCurrentSalaryConfig(employeeId);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy cấu hình lương hiện tại thành công"));
    }

    /**
     * Lấy lịch sử cấu hình lương của nhân viên
     * GET /api/company/employees/{employeeId}/salary-config/history
     */
    @GetMapping("/history")
    public ResponseEntity<BaseResponse<List<EmployeeSalaryConfigResponse>>> getSalaryConfigHistory(
            @PathVariable Long employeeId) {
        List<EmployeeSalaryConfigResponse> history = salaryConfigService.getSalaryConfigHistory(employeeId);
        return ResponseEntity.ok(BaseResponse.success(history, "Lấy lịch sử cấu hình lương thành công"));
    }

    /**
     * Tạo cấu hình lương mới cho nhân viên
     * POST /api/company/employees/{employeeId}/salary-config
     */
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeSalaryConfigResponse>> createSalaryConfig(
            @PathVariable Long employeeId,
            @Valid @RequestBody SalaryConfigRequest request) {
        EmployeeSalaryConfigResponse response = salaryConfigService.createSalaryConfig(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo cấu hình lương thành công"));
    }

    /**
     * Cập nhật cấu hình lương (tạo version mới)
     * PUT /api/company/employees/{employeeId}/salary-config/{configId}
     */
    @PutMapping("/{configId}")
    public ResponseEntity<BaseResponse<EmployeeSalaryConfigResponse>> updateSalaryConfig(
            @PathVariable Long employeeId,
            @PathVariable Long configId,
            @Valid @RequestBody SalaryConfigRequest request) {
        EmployeeSalaryConfigResponse response = salaryConfigService.updateSalaryConfig(configId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật cấu hình lương thành công"));
    }

    /**
     * Validate cấu hình lương mới - kiểm tra xem có ảnh hưởng đến kỳ lương hiện tại
     * không
     * POST /api/company/employees/{employeeId}/salary-config/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<BaseResponse<SalaryConfigValidationResponse>> validateSalaryConfig(
            @PathVariable Long employeeId,
            @Valid @RequestBody SalaryConfigRequest request) {
        SalaryConfigValidationResponse response = salaryConfigService.validateSalaryConfig(employeeId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Kiểm tra cấu hình lương thành công"));
    }

    /**
     * Xóa cấu hình lương
     * DELETE /api/company/employees/{employeeId}/salary-config/{configId}
     */
    @DeleteMapping("/{configId}")
    public ResponseEntity<BaseResponse<Void>> deleteSalaryConfig(
            @PathVariable Long employeeId,
            @PathVariable Long configId) {
        salaryConfigService.deleteSalaryConfig(configId);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa cấu hình lương thành công"));
    }
}
