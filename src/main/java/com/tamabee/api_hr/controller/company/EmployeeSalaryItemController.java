package com.tamabee.api_hr.controller.company;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.payroll.AssignSalaryItemRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryItemResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeSalaryItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý phụ cấp/khấu trừ của nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền CRUD salary items.
 */
@RestController
@RequestMapping("/api/company/employees/{employeeId}/salary-items")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class EmployeeSalaryItemController {

    private final IEmployeeSalaryItemService salaryItemService;

    /**
     * Lấy tất cả salary items của nhân viên
     * GET /api/company/employees/{employeeId}/salary-items
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<EmployeeSalaryItemResponse>>> getEmployeeSalaryItems(
            @PathVariable Long employeeId) {
        List<EmployeeSalaryItemResponse> items = salaryItemService.getEmployeeSalaryItems(employeeId);
        return ResponseEntity.ok(BaseResponse.success(items, "Lấy danh sách phụ cấp/khấu trừ thành công"));
    }

    /**
     * Lấy danh sách phụ cấp của nhân viên
     * GET /api/company/employees/{employeeId}/salary-items/allowances
     */
    @GetMapping("/allowances")
    public ResponseEntity<BaseResponse<List<EmployeeSalaryItemResponse>>> getEmployeeAllowances(
            @PathVariable Long employeeId) {
        List<EmployeeSalaryItemResponse> items = salaryItemService.getEmployeeAllowances(employeeId);
        return ResponseEntity.ok(BaseResponse.success(items, "Lấy danh sách phụ cấp thành công"));
    }

    /**
     * Lấy danh sách khấu trừ của nhân viên
     * GET /api/company/employees/{employeeId}/salary-items/deductions
     */
    @GetMapping("/deductions")
    public ResponseEntity<BaseResponse<List<EmployeeSalaryItemResponse>>> getEmployeeDeductions(
            @PathVariable Long employeeId) {
        List<EmployeeSalaryItemResponse> items = salaryItemService.getEmployeeDeductions(employeeId);
        return ResponseEntity.ok(BaseResponse.success(items, "Lấy danh sách khấu trừ thành công"));
    }

    /**
     * Gán salary item cho nhân viên
     * POST /api/company/employees/{employeeId}/salary-items
     */
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeSalaryItemResponse>> assignSalaryItem(
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignSalaryItemRequest request) {
        EmployeeSalaryItemResponse item = salaryItemService.assignSalaryItem(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(item, "Gán phụ cấp/khấu trừ thành công"));
    }

    /**
     * Cập nhật salary item
     * PUT /api/company/employees/{employeeId}/salary-items/{itemId}
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<BaseResponse<EmployeeSalaryItemResponse>> updateSalaryItem(
            @PathVariable Long employeeId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateSalaryItemRequest request) {
        EmployeeSalaryItemResponse item = salaryItemService.updateSalaryItem(itemId, request);
        return ResponseEntity.ok(BaseResponse.success(item, "Cập nhật phụ cấp/khấu trừ thành công"));
    }

    /**
     * Xóa salary item
     * DELETE /api/company/employees/{employeeId}/salary-items/{itemId}
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<BaseResponse<Void>> deleteSalaryItem(
            @PathVariable Long employeeId,
            @PathVariable Long itemId) {
        salaryItemService.deleteSalaryItem(itemId);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa phụ cấp/khấu trừ thành công"));
    }
}
