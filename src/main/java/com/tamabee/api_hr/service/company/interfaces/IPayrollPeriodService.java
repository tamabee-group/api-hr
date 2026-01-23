package com.tamabee.api_hr.service.company.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.payroll.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.request.wallet.PaymentRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodResponse;

/**
 * Service interface cho quản lý kỳ lương
 */
public interface IPayrollPeriodService {

    /**
     * Tạo kỳ lương mới với status DRAFT
     *
     * @param request   Thông tin kỳ lương
     * @param createdBy ID người tạo
     * @return Thông tin kỳ lương đã tạo
     */
    PayrollPeriodResponse createPayrollPeriod(PayrollPeriodRequest request, Long createdBy);

    /**
     * Tính lương cho kỳ - generate payroll items cho tất cả nhân viên active
     *
     * @param periodId ID kỳ lương
     * @return Thông tin kỳ lương sau khi tính toán
     */
    PayrollPeriodResponse calculatePayroll(Long periodId);

    /**
     * Lấy chi tiết kỳ lương bao gồm tất cả payroll items
     *
     * @param periodId ID kỳ lương
     * @return Chi tiết kỳ lương
     */
    PayrollPeriodDetailResponse getPayrollPeriodDetail(Long periodId);

    /**
     * Lấy danh sách kỳ lương của công ty (phân trang)
     *
     * @param pageable Thông tin phân trang
     * @return Danh sách kỳ lương
     */
    Page<PayrollPeriodResponse> getPayrollPeriods(Pageable pageable);

    /**
     * Điều chỉnh payroll item - lưu số tiền và lý do điều chỉnh
     *
     * @param itemId     ID payroll item
     * @param request    Thông tin điều chỉnh
     * @param adjustedBy ID người điều chỉnh
     * @return Thông tin payroll item sau điều chỉnh
     */
    PayrollItemResponse adjustPayrollItem(Long itemId, PayrollAdjustmentRequest request, Long adjustedBy);

    /**
     * Submit kỳ lương để review - chuyển status từ DRAFT sang REVIEWING
     *
     * @param periodId ID kỳ lương
     * @return Thông tin kỳ lương sau khi submit
     */
    PayrollPeriodResponse submitForReview(Long periodId);

    /**
     * Duyệt kỳ lương - chuyển status từ REVIEWING sang APPROVED
     *
     * @param periodId   ID kỳ lương
     * @param approverId ID người duyệt
     * @return Thông tin kỳ lương sau khi duyệt
     */
    PayrollPeriodResponse approvePayroll(Long periodId, Long approverId);

    /**
     * Đánh dấu kỳ lương đã thanh toán - chuyển status từ APPROVED sang PAID
     *
     * @param periodId ID kỳ lương
     * @param request  Thông tin thanh toán
     * @return Thông tin kỳ lương sau khi thanh toán
     */
    PayrollPeriodResponse markAsPaid(Long periodId, PaymentRequest request);

    /**
     * Từ chối kỳ lương - chuyển status từ REVIEWING về DRAFT
     *
     * @param periodId ID kỳ lương
     * @param reason   Lý do từ chối
     * @return Thông tin kỳ lương sau khi từ chối
     */
    PayrollPeriodResponse rejectPayroll(Long periodId, String reason);

    /**
     * Lấy danh sách payroll items của kỳ lương với filter và pagination
     *
     * @param periodId   ID kỳ lương
     * @param employeeId Filter theo employee (optional)
     * @param status     Filter theo status (optional)
     * @param pageable   Thông tin phân trang
     * @return Danh sách payroll items
     */
    Page<PayrollItemResponse> getPayrollItems(Long periodId, Long employeeId, String status, Pageable pageable);

    /**
     * Lấy chi tiết 1 payroll item
     *
     * @param itemId ID payroll item
     * @return Chi tiết payroll item
     */
    PayrollItemResponse getPayrollItemById(Long itemId);

    /**
     * Lấy lịch sử payslip của employee
     *
     * @param employeeId ID nhân viên
     * @param pageable   Thông tin phân trang
     * @return Danh sách payslip của nhân viên
     */
    Page<PayrollItemResponse> getEmployeePayslips(Long employeeId, Pageable pageable);

    /**
     * Lấy tất cả payslips của công ty
     *
     * @param employeeId ID nhân viên (optional filter)
     * @param status     Status (optional filter)
     * @param pageable   Thông tin phân trang
     * @return Danh sách tất cả payslips
     */
    Page<PayrollItemResponse> getAllCompanyPayslips(Long employeeId, String status, Pageable pageable);

    /**
     * Generate PDF payslip cho payroll item
     *
     * @param itemId ID của payroll item
     * @return PDF data dưới dạng byte array
     */
    byte[] generatePayslipPdf(Long itemId);
}
