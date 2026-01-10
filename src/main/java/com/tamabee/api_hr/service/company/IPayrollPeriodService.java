package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.PaymentRequest;
import com.tamabee.api_hr.dto.request.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.response.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
