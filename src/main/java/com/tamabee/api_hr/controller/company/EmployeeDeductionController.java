package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.payroll.DeductionAssignmentRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeDeductionResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeDeductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý khấu trừ cá nhân của nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/employees/{employeeId}/deductions")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class EmployeeDeductionController {

    private final IEmployeeDeductionService deductionService;

    /**
     * Lấy danh sách khấu trừ của nhân viên
     * GET /api/company/employees/{employeeId}/deductions
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<EmployeeDeductionResponse>>> getEmployeeDeductions(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<EmployeeDeductionResponse> deductions = deductionService.getEmployeeDeductions(employeeId,
                includeInactive);
        return ResponseEntity.ok(BaseResponse.success(deductions, "Lấy danh sách khấu trừ thành công"));
    }

    /**
     * Gán khấu trừ cho nhân viên
     * POST /api/company/employees/{employeeId}/deductions
     */
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeDeductionResponse>> assignDeduction(
            @PathVariable Long employeeId,
            @Valid @RequestBody DeductionAssignmentRequest request) {
        EmployeeDeductionResponse response = deductionService.assignDeduction(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Gán khấu trừ thành công"));
    }

    /**
     * Cập nhật khấu trừ của nhân viên
     * PUT /api/company/employees/{employeeId}/deductions/{deductionId}
     */
    @PutMapping("/{deductionId}")
    public ResponseEntity<BaseResponse<EmployeeDeductionResponse>> updateDeduction(
            @PathVariable Long employeeId,
            @PathVariable Long deductionId,
            @Valid @RequestBody DeductionAssignmentRequest request) {
        EmployeeDeductionResponse response = deductionService.updateDeduction(deductionId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật khấu trừ thành công"));
    }

    /**
     * Vô hiệu hóa khấu trừ
     * DELETE /api/company/employees/{employeeId}/deductions/{deductionId}
     */
    @DeleteMapping("/{deductionId}")
    public ResponseEntity<BaseResponse<Void>> deactivateDeduction(
            @PathVariable Long employeeId,
            @PathVariable Long deductionId) {
        deductionService.deactivateDeduction(deductionId);
        return ResponseEntity.ok(BaseResponse.success(null, "Vô hiệu hóa khấu trừ thành công"));
    }
}
