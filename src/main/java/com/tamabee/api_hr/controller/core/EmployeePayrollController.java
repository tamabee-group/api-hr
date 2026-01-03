package com.tamabee.api_hr.controller.core;

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
 * Controller cho nhân viên xem lương của mình.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/payroll")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeePayrollController {

    private final IPayrollService payrollService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Lấy lịch sử lương của nhân viên (phân trang)
     * GET /api/employee/payroll
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PayrollRecordResponse>>> getMyPayrollHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserEntity currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year", "month"));
        Page<PayrollRecordResponse> records = payrollService.getEmployeePayrollHistory(
                currentUser.getId(), pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy lịch sử lương thành công"));
    }

    /**
     * Lấy chi tiết lương của nhân viên theo kỳ
     * GET /api/employee/payroll/{period}
     */
    @GetMapping("/{period}")
    public ResponseEntity<BaseResponse<PayrollRecordResponse>> getMyPayrollByPeriod(
            @PathVariable String period) {
        UserEntity currentUser = getCurrentUser();
        YearMonth yearMonth = YearMonth.parse(period, PERIOD_FORMATTER);
        PayrollRecordResponse record = payrollService.getEmployeePayroll(currentUser.getId(), yearMonth);
        return ResponseEntity.ok(BaseResponse.success(record, "Lấy thông tin lương thành công"));
    }

    /**
     * Download payslip PDF của nhân viên
     * GET /api/employee/payroll/{recordId}/download
     */
    @GetMapping("/{recordId}/download")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long recordId) {
        UserEntity currentUser = getCurrentUser();

        // Kiểm tra bản ghi lương thuộc về nhân viên hiện tại
        PayrollRecordResponse record = payrollService.getPayrollRecordById(recordId);
        if (!record.getEmployeeId().equals(currentUser.getId())) {
            throw NotFoundException.payrollRecord(recordId);
        }

        byte[] pdfData = payrollService.generatePayslip(recordId);

        // Tên file theo ngôn ngữ - encode UTF-8 cho header
        String payslipLabel = getPayslipLabel(currentUser.getLanguage());
        String filename = String.format("%s_%s_%d-%02d.pdf",
                payslipLabel,
                currentUser.getEmployeeCode(),
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
