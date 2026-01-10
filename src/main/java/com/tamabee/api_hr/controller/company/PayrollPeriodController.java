package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.PaymentRequest;
import com.tamabee.api_hr.dto.request.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.response.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IPayrollPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý kỳ lương.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/payroll-periods")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class PayrollPeriodController {

    private final IPayrollPeriodService payrollPeriodService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách kỳ lương của công ty
     * GET /api/company/payroll-periods
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PayrollPeriodResponse>>> getPayrollPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year", "month"));
        Page<PayrollPeriodResponse> periods = payrollPeriodService.getPayrollPeriods(pageable);
        return ResponseEntity.ok(BaseResponse.success(periods, "Lấy danh sách kỳ lương thành công"));
    }

    /**
     * Lấy chi tiết kỳ lương bao gồm tất cả payroll items
     * GET /api/company/payroll-periods/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<PayrollPeriodDetailResponse>> getPayrollPeriodDetail(@PathVariable Long id) {
        PayrollPeriodDetailResponse detail = payrollPeriodService.getPayrollPeriodDetail(id);
        return ResponseEntity.ok(BaseResponse.success(detail, "Lấy chi tiết kỳ lương thành công"));
    }

    /**
     * Tạo kỳ lương mới với status DRAFT
     * POST /api/company/payroll-periods
     */
    @PostMapping
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> createPayrollPeriod(
            @Valid @RequestBody PayrollPeriodRequest request) {
        Long createdBy = getCurrentUserId();
        PayrollPeriodResponse response = payrollPeriodService.createPayrollPeriod(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo kỳ lương thành công"));
    }

    /**
     * Tính lương cho kỳ - generate payroll items cho tất cả nhân viên active
     * POST /api/company/payroll-periods/{id}/calculate
     */
    @PostMapping("/{id}/calculate")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> calculatePayroll(@PathVariable Long id) {
        PayrollPeriodResponse response = payrollPeriodService.calculatePayroll(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Tính lương thành công"));
    }

    /**
     * Điều chỉnh payroll item
     * PUT /api/company/payroll-periods/items/{itemId}/adjust
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
     * Submit kỳ lương để review - chuyển status từ DRAFT sang REVIEWING
     * POST /api/company/payroll-periods/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> submitForReview(@PathVariable Long id) {
        PayrollPeriodResponse response = payrollPeriodService.submitForReview(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Submit kỳ lương để review thành công"));
    }

    /**
     * Duyệt kỳ lương - chuyển status từ REVIEWING sang APPROVED
     * POST /api/company/payroll-periods/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<PayrollPeriodResponse>> approvePayroll(@PathVariable Long id) {
        Long approverId = getCurrentUserId();
        PayrollPeriodResponse response = payrollPeriodService.approvePayroll(id, approverId);
        return ResponseEntity.ok(BaseResponse.success(response, "Duyệt kỳ lương thành công"));
    }

    /**
     * Đánh dấu kỳ lương đã thanh toán - chuyển status từ APPROVED sang PAID
     * POST /api/company/payroll-periods/{id}/pay
     */
    @PostMapping("/{id}/pay")
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
}
