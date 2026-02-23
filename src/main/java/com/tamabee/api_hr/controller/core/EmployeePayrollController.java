package com.tamabee.api_hr.controller.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho nhân viên xem lương của mình.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/payroll")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeePayrollController {

    private final IPayrollPeriodService payrollPeriodService;
    private final UserRepository userRepository;

    /**
     * Lấy lịch sử lương của nhân viên (phân trang)
     * GET /api/employee/payroll
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getMyPayrollHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        UserEntity currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<PayrollItemResponse> items = payrollPeriodService.getEmployeePayslips(
                currentUser.getId(), status, pageable);
        return ResponseEntity.ok(BaseResponse.success(items, "Lấy lịch sử lương thành công"));
    }

    /**
     * Lấy chi tiết lương của nhân viên theo ID
     * GET /api/employee/payroll/{itemId}
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<BaseResponse<PayrollItemResponse>> getMyPayrollById(
            @PathVariable Long itemId) {
        UserEntity currentUser = getCurrentUser();
        PayrollItemResponse item = payrollPeriodService.getPayrollItemById(itemId);
        
        // Kiểm tra item thuộc về nhân viên hiện tại
        if (!item.getEmployeeId().equals(currentUser.getId())) {
            throw NotFoundException.payrollRecord(itemId);
        }
        
        return ResponseEntity.ok(BaseResponse.success(item, "Lấy thông tin lương thành công"));
    }

    /**
     * Download payslip PDF của nhân viên
     * GET /api/employee/payroll/{itemId}/download
     */
    @GetMapping("/{itemId}/download")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long itemId) {
        UserEntity currentUser = getCurrentUser();

        // Kiểm tra item thuộc về nhân viên hiện tại
        PayrollItemResponse item = payrollPeriodService.getPayrollItemById(itemId);
        if (!item.getEmployeeId().equals(currentUser.getId())) {
            throw NotFoundException.payrollRecord(itemId);
        }

        byte[] pdfData = payrollPeriodService.generatePayslipPdf(itemId);

        // Tên file theo ngôn ngữ - encode UTF-8 cho header
        String payslipLabel = getPayslipLabel(currentUser.getLanguage());
        String filename = String.format("%s_%s_%d-%02d.pdf",
                payslipLabel,
                item.getEmployeeCode(),
                item.getYear(),
                item.getMonth());

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
