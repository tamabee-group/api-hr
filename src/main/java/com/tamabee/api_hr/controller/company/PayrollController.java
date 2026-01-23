package com.tamabee.api_hr.controller.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.payroll.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.request.wallet.PaymentRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý lương cho admin/manager công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/payroll")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class PayrollController {

    private final IPayrollPeriodService payrollPeriodService;
    private final UserRepository userRepository;

    // ==================== Payroll Periods Management ====================

    /**
     * Lấy danh sách kỳ lương của công ty
     * GET /api/company/payroll/periods
     */
    @GetMapping("/periods")
    public ResponseEntity<BaseResponse<Page<PayrollPeriodResponse>>> getPayrollPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year", "month"));
        Page<PayrollPeriodResponse> periods = payrollPeriodService.getPayrollPeriods(pageable);
        return ResponseEntity.ok(BaseResponse.success(periods, "Lấy danh sách kỳ lương thành công"));
    }

    /**
     * Lấy chi tiết kỳ lương bao gồm tất cả payroll items
     * GET /api/company/payroll/periods/{id}
     */
    @GetMapping("/periods/{id}")
    public ResponseEntity<BaseResponse<PayrollPeriodDetailResponse>> getPayrollPeriodDetail(@PathVariable Long id) {
        PayrollPeriodDetailResponse detail = payrollPeriodService.getPayrollPeriodDetail(id);
        return ResponseEntity.ok(BaseResponse.success(detail, "Lấy chi tiết kỳ lương thành công"));
    }

    /**
     * Lấy summary của kỳ lương
     * GET /api/company/payroll/periods/{id}/summary
     */
    @GetMapping("/periods/{id}/summary")
    public ResponseEntity<BaseResponse<PayrollPeriodDetailResponse>> getPayrollPeriodSummary(@PathVariable Long id) {
        PayrollPeriodDetailResponse detail = payrollPeriodService.getPayrollPeriodDetail(id);
        return ResponseEntity.ok(BaseResponse.success(detail, "Lấy tổng hợp kỳ lương thành công"));
    }

    /**
     * Tạo kỳ lương mới với status DRAFT
     * POST /api/company/payroll/periods
     */
    @PostMapping("/periods")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> createPayrollPeriod(
            @Valid @RequestBody PayrollPeriodRequest request) {
        Long createdBy = getCurrentUserId();
        PayrollPeriodResponse response = payrollPeriodService.createPayrollPeriod(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo kỳ lương thành công"));
    }

    // ==================== Payroll Calculation ====================

    /**
     * Tính lương cho kỳ - generate payroll items cho tất cả nhân viên active
     * POST /api/company/payroll/periods/{id}/calculate
     */
    @PostMapping("/periods/{id}/calculate")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> calculatePayroll(@PathVariable Long id) {
        PayrollPeriodResponse response = payrollPeriodService.calculatePayroll(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Tính lương thành công"));
    }

    /**
     * Tính lại lương cho kỳ (recalculate)
     * POST /api/company/payroll/periods/{id}/recalculate
     */
    @PostMapping("/periods/{id}/recalculate")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> recalculatePayroll(@PathVariable Long id) {
        PayrollPeriodResponse response = payrollPeriodService.calculatePayroll(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Tính lại lương thành công"));
    }

    // ==================== Payroll Workflow ====================

    /**
     * Submit kỳ lương để review - chuyển status từ DRAFT sang REVIEWING
     * POST /api/company/payroll/periods/{id}/submit
     */
    @PostMapping("/periods/{id}/submit")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> submitForReview(@PathVariable Long id) {
        PayrollPeriodResponse response = payrollPeriodService.submitForReview(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Submit kỳ lương để review thành công"));
    }

    /**
     * Duyệt kỳ lương - chuyển status từ REVIEWING sang APPROVED
     * POST /api/company/payroll/periods/{id}/approve
     */
    @PostMapping("/periods/{id}/approve")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> approvePayroll(@PathVariable Long id) {
        Long approverId = getCurrentUserId();
        PayrollPeriodResponse response = payrollPeriodService.approvePayroll(id, approverId);
        return ResponseEntity.ok(BaseResponse.success(response, "Duyệt kỳ lương thành công"));
    }

    /**
     * Từ chối kỳ lương - chuyển status từ REVIEWING về DRAFT
     * POST /api/company/payroll/periods/{id}/reject
     */
    @PostMapping("/periods/{id}/reject")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> rejectPayroll(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        PayrollPeriodResponse response = payrollPeriodService.rejectPayroll(id, reason);
        return ResponseEntity.ok(BaseResponse.success(response, "Từ chối kỳ lương thành công"));
    }

    /**
     * Đánh dấu kỳ lương đã thanh toán - chuyển status từ APPROVED sang PAID
     * POST /api/company/payroll/periods/{id}/pay
     */
    @PostMapping("/periods/{id}/pay")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> markAsPaid(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentRequest request) {
        if (request == null) {
            request = new PaymentRequest();
        }
        PayrollPeriodResponse response = payrollPeriodService.markAsPaid(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Đánh dấu đã thanh toán thành công"));
    }

    // ==================== Payroll Items Management ====================

    /**
     * Lấy danh sách payroll items của kỳ lương với pagination
     * GET /api/company/payroll/periods/{id}/items
     */
    @GetMapping("/periods/{id}/items")
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getPayrollItems(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PayrollItemResponse> items = payrollPeriodService.getPayrollItems(id, employeeId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(items, "Lấy danh sách payroll items thành công"));
    }

    /**
     * Lấy chi tiết 1 payroll item
     * GET /api/company/payroll/periods/{periodId}/items/{itemId}
     */
    @GetMapping("/periods/{periodId}/items/{itemId}")
    public ResponseEntity<BaseResponse<PayrollItemResponse>> getPayrollItemById(
            @PathVariable Long periodId,
            @PathVariable Long itemId) {
        PayrollItemResponse item = payrollPeriodService.getPayrollItemById(itemId);
        return ResponseEntity.ok(BaseResponse.success(item, "Lấy chi tiết payroll item thành công"));
    }

    /**
     * Điều chỉnh payroll item
     * PUT /api/company/payroll/items/{itemId}/adjust
     */
    @PutMapping("/items/{itemId}/adjust")
    public ResponseEntity<BaseResponse<PayrollItemResponse>> adjustPayrollItem(
            @PathVariable Long itemId,
            @Valid @RequestBody PayrollAdjustmentRequest request) {
        Long adjustedBy = getCurrentUserId();
        PayrollItemResponse response = payrollPeriodService.adjustPayrollItem(itemId, request, adjustedBy);
        return ResponseEntity.ok(BaseResponse.success(response, "Điều chỉnh lương thành công"));
    }

    /**
     * Lấy lịch sử điều chỉnh của payroll item
     * GET /api/company/payroll/periods/{periodId}/items/{itemId}/adjustments
     */
    @GetMapping("/periods/{periodId}/items/{itemId}/adjustments")
    public ResponseEntity<BaseResponse<java.util.List<java.util.Map<String, Object>>>> getPayrollItemAdjustments(
            @PathVariable Long periodId,
            @PathVariable Long itemId) {
        // Tạm thời trả về empty list vì chưa có bảng lưu lịch sử
        java.util.List<java.util.Map<String, Object>> adjustments = new java.util.ArrayList<>();
        return ResponseEntity.ok(BaseResponse.success(adjustments, "Lấy lịch sử điều chỉnh thành công"));
    }

    // ==================== Payslip History ====================

    /**
     * Lấy lịch sử payslip của employee
     * GET /api/company/payroll/employee/{employeeId}/payslips
     */
    @GetMapping("/employee/{employeeId}/payslips")
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getEmployeePayslips(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<PayrollItemResponse> payslips = payrollPeriodService.getEmployeePayslips(employeeId, pageable);
        return ResponseEntity.ok(BaseResponse.success(payslips, "Lấy lịch sử payslip thành công"));
    }

    /**
     * Lấy tất cả payslips của công ty
     * GET /api/company/payroll/payslips
     */
    @GetMapping("/payslips")
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getAllCompanyPayslips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<PayrollItemResponse> payslips = payrollPeriodService.getAllCompanyPayslips(employeeId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(payslips, "Lấy danh sách payslips thành công"));
    }

    // ==================== Export & Download ====================

    /**
     * Download payslip PDF của một payroll item
     * GET /api/company/payroll/items/{itemId}/download
     */
    @GetMapping("/items/{itemId}/download")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long itemId) {
        UserEntity currentUser = getCurrentUser();
        PayrollItemResponse item = payrollPeriodService.getPayrollItemById(itemId);

        // Generate PDF từ payroll item
        byte[] pdfData = payrollPeriodService.generatePayslipPdf(itemId);

        // Tên file theo ngôn ngữ của admin - encode UTF-8 cho header
        String payslipLabel = getPayslipLabel(currentUser.getLanguage());
        String filename = String.format("%s_%s_%d-%02d.pdf",
                payslipLabel,
                item.getEmployeeCode(),
                item.getYear(),
                item.getMonth());

        // Encode filename cho Content-Disposition (RFC 5987)
        String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    /**
     * Lấy label "payslip" theo ngôn ngữ
     */
    private String getPayslipLabel(String language) {
        if (language == null) {
            return "payslip";
        }
        return switch (language.toLowerCase()) {
            case "vi" -> "phieu_luong";
            case "ja" -> "給与明細";
            default -> "payslip";
        };
    }

    // ==================== Helper Methods ====================

    /**
     * Lấy ID của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getId();
    }

    /**
     * Lấy user đang đăng nhập
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
    }
}
