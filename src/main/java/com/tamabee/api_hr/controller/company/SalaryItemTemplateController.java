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
import com.tamabee.api_hr.dto.request.payroll.CreateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.response.payroll.SalaryItemTemplateResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.SalaryItemType;
import com.tamabee.api_hr.service.company.interfaces.ISalaryItemTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý template phụ cấp/khấu trừ.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền CRUD templates.
 */
@RestController
@RequestMapping("/api/company/salary-item-templates")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class SalaryItemTemplateController {

    private final ISalaryItemTemplateService templateService;

    /**
     * Lấy tất cả templates
     * GET /api/company/salary-item-templates
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<SalaryItemTemplateResponse>>> getAllTemplates() {
        List<SalaryItemTemplateResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(BaseResponse.success(templates, "Lấy danh sách templates thành công"));
    }

    /**
     * Lấy templates theo loại
     * GET /api/company/salary-item-templates/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<BaseResponse<List<SalaryItemTemplateResponse>>> getTemplatesByType(
            @PathVariable SalaryItemType type) {
        List<SalaryItemTemplateResponse> templates = templateService.getTemplatesByType(type);
        return ResponseEntity.ok(BaseResponse.success(templates, "Lấy danh sách templates theo loại thành công"));
    }

    /**
     * Tạo template mới
     * POST /api/company/salary-item-templates
     */
    @PostMapping
    public ResponseEntity<BaseResponse<SalaryItemTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateSalaryItemTemplateRequest request) {
        SalaryItemTemplateResponse template = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(template, "Tạo template thành công"));
    }

    /**
     * Cập nhật template
     * PUT /api/company/salary-item-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<SalaryItemTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalaryItemTemplateRequest request) {
        SalaryItemTemplateResponse template = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(BaseResponse.success(template, "Cập nhật template thành công"));
    }

    /**
     * Xóa template
     * DELETE /api/company/salary-item-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa template thành công"));
    }

    /**
     * Lấy số nhân viên đang sử dụng template
     * GET /api/company/salary-item-templates/{id}/employee-count
     */
    @GetMapping("/{id}/employee-count")
    public ResponseEntity<BaseResponse<Long>> getEmployeeCount(@PathVariable Long id) {
        long count = templateService.getEmployeeCountByTemplateId(id);
        return ResponseEntity.ok(BaseResponse.success(count, "Lấy số nhân viên thành công"));
    }
}
