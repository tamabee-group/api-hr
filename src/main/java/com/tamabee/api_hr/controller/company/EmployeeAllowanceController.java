package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.payroll.AllowanceAssignmentRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeAllowanceResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeAllowanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý phụ cấp cá nhân của nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/employees/{employeeId}/allowances")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class EmployeeAllowanceController {

    private final IEmployeeAllowanceService allowanceService;

    /**
     * Lấy danh sách phụ cấp của nhân viên
     * GET /api/company/employees/{employeeId}/allowances
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<EmployeeAllowanceResponse>>> getEmployeeAllowances(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<EmployeeAllowanceResponse> allowances = allowanceService.getEmployeeAllowances(employeeId,
                includeInactive);
        return ResponseEntity.ok(BaseResponse.success(allowances, "Lấy danh sách phụ cấp thành công"));
    }

    /**
     * Gán phụ cấp cho nhân viên
     * POST /api/company/employees/{employeeId}/allowances
     */
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeAllowanceResponse>> assignAllowance(
            @PathVariable Long employeeId,
            @Valid @RequestBody AllowanceAssignmentRequest request) {
        EmployeeAllowanceResponse response = allowanceService.assignAllowance(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Gán phụ cấp thành công"));
    }

    /**
     * Cập nhật phụ cấp của nhân viên
     * PUT /api/company/employees/{employeeId}/allowances/{allowanceId}
     */
    @PutMapping("/{allowanceId}")
    public ResponseEntity<BaseResponse<EmployeeAllowanceResponse>> updateAllowance(
            @PathVariable Long employeeId,
            @PathVariable Long allowanceId,
            @Valid @RequestBody AllowanceAssignmentRequest request) {
        EmployeeAllowanceResponse response = allowanceService.updateAllowance(allowanceId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật phụ cấp thành công"));
    }

    /**
     * Vô hiệu hóa phụ cấp
     * DELETE /api/company/employees/{employeeId}/allowances/{allowanceId}
     */
    @DeleteMapping("/{allowanceId}")
    public ResponseEntity<BaseResponse<Void>> deactivateAllowance(
            @PathVariable Long employeeId,
            @PathVariable Long allowanceId) {
        allowanceService.deactivateAllowance(allowanceId);
        return ResponseEntity.ok(BaseResponse.success(null, "Vô hiệu hóa phụ cấp thành công"));
    }
}
