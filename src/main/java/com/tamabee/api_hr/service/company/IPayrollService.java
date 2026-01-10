package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.response.PayrollPeriodSummaryResponse;
import com.tamabee.api_hr.dto.response.PayrollPreviewResponse;
import com.tamabee.api_hr.dto.response.PayrollRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;

/**
 * Service quản lý tính lương và thanh toán.
 * Hỗ trợ preview, finalize, payment tracking và salary notification.
 */
public interface IPayrollService {

    // ==================== Preview & Finalize ====================

    /**
     * Preview lương của công ty cho một kỳ (chưa finalize)
     * Tính toán lương cho tất cả nhân viên dựa trên attendance records
     *
     * @param period kỳ lương (tháng)
     * @return preview lương với chi tiết từng nhân viên
     */
    PayrollPreviewResponse previewPayroll(YearMonth period);

    /**
     * Preview lương của một nhân viên cho một kỳ
     *
     * @param employeeId ID nhân viên
     * @param period     kỳ lương (tháng)
     * @return preview lương của nhân viên
     */
    PayrollRecordResponse previewEmployeePayroll(Long employeeId, YearMonth period);

    /**
     * Finalize lương của công ty cho một kỳ
     * Sau khi finalize, các bản ghi lương không thể sửa đổi
     *
     * @param period      kỳ lương (tháng)
     * @param finalizedBy ID người finalize
     * @return tổng hợp lương đã finalize
     */
    PayrollPeriodSummaryResponse finalizePayroll(YearMonth period, Long finalizedBy);

    // ==================== Payment Processing ====================

    /**
     * Đánh dấu tất cả bản ghi lương của công ty trong kỳ là đã thanh toán
     *
     * @param period kỳ lương (tháng)
     */
    void markAsPaid(YearMonth period);

    /**
     * Đánh dấu một bản ghi lương là đã thanh toán
     *
     * @param payrollRecordId  ID bản ghi lương
     * @param paymentReference mã tham chiếu thanh toán (optional)
     */
    void markEmployeeAsPaid(Long payrollRecordId, String paymentReference);

    /**
     * Retry thanh toán cho bản ghi lương bị lỗi
     *
     * @param payrollRecordId ID bản ghi lương
     */
    void retryPayment(Long payrollRecordId);

    // ==================== Salary Notification ====================

    /**
     * Gửi thông báo lương cho tất cả nhân viên trong kỳ
     * Chỉ gửi cho các bản ghi đã finalize và chưa gửi thông báo
     *
     * @param period kỳ lương (tháng)
     */
    void sendSalaryNotifications(YearMonth period);

    /**
     * Gửi thông báo lương cho một nhân viên
     *
     * @param payrollRecordId ID bản ghi lương
     */
    void sendSalaryNotification(Long payrollRecordId);

    // ==================== Query Operations ====================

    /**
     * Lấy bản ghi lương theo ID
     *
     * @param recordId ID bản ghi lương
     * @return bản ghi lương
     */
    PayrollRecordResponse getPayrollRecordById(Long recordId);

    /**
     * Lấy tổng hợp lương của công ty theo kỳ
     *
     * @param period kỳ lương (tháng)
     * @return tổng hợp lương
     */
    PayrollPeriodSummaryResponse getPayrollPeriodSummary(YearMonth period);

    /**
     * Lấy danh sách bản ghi lương của công ty theo kỳ (phân trang)
     *
     * @param period   kỳ lương (tháng)
     * @param pageable thông tin phân trang
     * @return danh sách bản ghi lương
     */
    Page<PayrollRecordResponse> getPayrollRecords(YearMonth period, Pageable pageable);

    /**
     * Lấy bản ghi lương của nhân viên theo kỳ
     *
     * @param employeeId ID nhân viên
     * @param period     kỳ lương (tháng)
     * @return bản ghi lương (null nếu không có)
     */
    PayrollRecordResponse getEmployeePayroll(Long employeeId, YearMonth period);

    /**
     * Lấy lịch sử lương của nhân viên (phân trang)
     *
     * @param employeeId ID nhân viên
     * @param pageable   thông tin phân trang
     * @return danh sách bản ghi lương
     */
    Page<PayrollRecordResponse> getEmployeePayrollHistory(Long employeeId, Pageable pageable);

    // ==================== Export ====================

    /**
     * Export danh sách lương ra file CSV
     *
     * @param period kỳ lương (tháng)
     * @return nội dung file CSV dưới dạng byte array
     */
    byte[] exportPayrollCsv(YearMonth period);

    /**
     * Export danh sách lương ra file PDF
     *
     * @param period kỳ lương (tháng)
     * @return nội dung file PDF dưới dạng byte array
     */
    byte[] exportPayrollPdf(YearMonth period);

    /**
     * Tạo payslip PDF cho một nhân viên
     *
     * @param payrollRecordId ID bản ghi lương
     * @return nội dung file PDF dưới dạng byte array
     */
    byte[] generatePayslip(Long payrollRecordId);
}
