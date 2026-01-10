package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.response.PayrollPeriodSummaryResponse;
import com.tamabee.api_hr.dto.response.PayrollPreviewResponse;
import com.tamabee.api_hr.dto.response.PayrollRecordResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IPayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Controller quản lý lương cho admin/manager công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/payroll")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class PayrollController {

    private final IPayrollService payrollService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // ==================== Preview & Finalize ====================

    /**
     * Preview lương của công ty cho một kỳ (chưa finalize)
     * GET /api/company/payroll/preview?period=2025-01
     */
    @GetMapping("/preview")
    public ResponseEntity<BaseResponse<PayrollPreviewResponse>> previewPayroll(
            @RequestParam String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        PayrollPreviewResponse preview = payrollService.previewPayroll(yearMonth);
        return ResponseEntity.ok(BaseResponse.success(preview, "Preview lương thành công"));
    }

    /**
     * Finalize lương của công ty cho một kỳ
     * POST /api/company/payroll/finalize?period=2025-01
     */
    @PostMapping("/finalize")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<PayrollPeriodSummaryResponse>> finalizePayroll(
            @RequestParam String period) {
        UserEntity currentUser = getCurrentUser();
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        PayrollPeriodSummaryResponse summary = payrollService.finalizePayroll(
                yearMonth, currentUser.getId());
        return ResponseEntity.ok(BaseResponse.success(summary, "Finalize lương thành công"));
    }

    // ==================== Payment Processing ====================

    /**
     * Đánh dấu tất cả bản ghi lương của công ty trong kỳ là đã thanh toán
     * POST /api/company/payroll/pay?period=2025-01
     */
    @PostMapping("/pay")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> markAsPaid(@RequestParam String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        payrollService.markAsPaid(yearMonth);
        return ResponseEntity.ok(BaseResponse.success(null, "Đánh dấu thanh toán thành công"));
    }

    /**
     * Đánh dấu một bản ghi lương là đã thanh toán
     * POST /api/company/payroll/records/{id}/pay
     */
    @PostMapping("/records/{id}/pay")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> markEmployeeAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String paymentReference) {
        payrollService.markEmployeeAsPaid(id, paymentReference);
        return ResponseEntity.ok(BaseResponse.success(null, "Đánh dấu thanh toán cho nhân viên thành công"));
    }

    /**
     * Retry thanh toán cho bản ghi lương bị lỗi
     * POST /api/company/payroll/records/{id}/retry
     */
    @PostMapping("/records/{id}/retry")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> retryPayment(@PathVariable Long id) {
        payrollService.retryPayment(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Retry thanh toán thành công"));
    }

    // ==================== Salary Notification ====================

    /**
     * Gửi thông báo lương cho tất cả nhân viên trong kỳ
     * POST /api/company/payroll/notify?period=2025-01
     */
    @PostMapping("/notify")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> sendSalaryNotifications(@RequestParam String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        payrollService.sendSalaryNotifications(yearMonth);
        return ResponseEntity.ok(BaseResponse.success(null, "Gửi thông báo lương thành công"));
    }

    // ==================== Query Operations ====================

    /**
     * Lấy danh sách bản ghi lương của công ty theo kỳ (phân trang)
     * GET /api/company/payroll?period=2025-01
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PayrollRecordResponse>>> getPayrollRecords(
            @RequestParam String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "employeeId"));
        Page<PayrollRecordResponse> records = payrollService.getPayrollRecords(yearMonth, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách lương thành công"));
    }

    /**
     * Lấy tổng hợp lương của công ty theo kỳ
     * GET /api/company/payroll/{period}/summary
     */
    @GetMapping("/{period}/summary")
    public ResponseEntity<BaseResponse<PayrollPeriodSummaryResponse>> getPayrollPeriodSummary(
            @PathVariable String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        PayrollPeriodSummaryResponse summary = payrollService.getPayrollPeriodSummary(yearMonth);
        return ResponseEntity.ok(BaseResponse.success(summary, "Lấy tổng hợp lương thành công"));
    }

    /**
     * Lấy chi tiết bản ghi lương theo ID
     * GET /api/company/payroll/records/{id}
     */
    @GetMapping("/records/{id}")
    public ResponseEntity<BaseResponse<PayrollRecordResponse>> getPayrollRecordById(@PathVariable Long id) {
        PayrollRecordResponse record = payrollService.getPayrollRecordById(id);
        return ResponseEntity.ok(BaseResponse.success(record, "Lấy thông tin lương thành công"));
    }

    // ==================== Export ====================

    /**
     * Export danh sách lương ra file CSV
     * GET /api/company/payroll/export/csv?period=2025-01
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportPayrollCsv(@RequestParam String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        byte[] csvData = payrollService.exportPayrollCsv(yearMonth);

        String filename = String.format("payroll_%s.csv", period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Export danh sách lương ra file PDF
     * GET /api/company/payroll/export/pdf?period=2025-01
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPayrollPdf(@RequestParam String period) {
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        byte[] pdfData = payrollService.exportPayrollPdf(yearMonth);

        String filename = String.format("payroll_%s.pdf", period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    /**
     * Download payslip PDF của một nhân viên
     * GET /api/company/payroll/{recordId}/download
     */
    @GetMapping("/{recordId}/download")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long recordId) {
        UserEntity currentUser = getCurrentUser();
        PayrollRecordResponse record = payrollService.getPayrollRecordById(recordId);

        byte[] pdfData = payrollService.generatePayslip(recordId);

        // Tên file theo ngôn ngữ của admin - encode UTF-8 cho header
        String payslipLabel = getPayslipLabel(currentUser.getLanguage());
        String filename = String.format("%s_%s_%d-%02d.pdf",
                payslipLabel,
                record.getEmployeeCode(),
                record.getYear(),
                record.getMonth());

        // Encode filename cho Content-Disposition (RFC 5987)
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
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
     * Lấy thông tin user đang đăng nhập
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
    }
}
