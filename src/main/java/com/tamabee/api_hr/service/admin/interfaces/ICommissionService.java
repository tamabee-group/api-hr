package com.tamabee.api_hr.service.admin.interfaces;

import com.tamabee.api_hr.dto.request.wallet.CommissionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.CommissionOverallSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service quản lý hoa hồng giới thiệu cho nhân viên Tamabee
 * Hỗ trợ tính hoa hồng, xem danh sách, tổng hợp và thanh toán
 */
public interface ICommissionService {

    // ==================== Commission Processing ====================

    /**
     * Xử lý hoa hồng khi company thanh toán lần đầu (sau free trial)
     * Chỉ tính hoa hồng nếu:
     * - Company được giới thiệu bởi nhân viên Tamabee
     * - Đây là lần thanh toán đầu tiên của company
     *
     * @param companyId ID của company thanh toán
     */
    void processCommission(Long companyId);

    // ==================== View Operations ====================

    /**
     * Lấy tất cả commissions (phân trang)
     * Dùng cho ADMIN_TAMABEE, MANAGER_TAMABEE
     *
     * @param filter   filter theo employeeCode, status, khoảng thời gian
     * @param pageable thông tin phân trang
     * @return danh sách commissions
     */
    Page<CommissionResponse> getAll(CommissionFilterRequest filter, Pageable pageable);

    /**
     * Lấy commissions của nhân viên hiện tại (từ JWT token)
     * Dùng cho EMPLOYEE_TAMABEE
     *
     * @param filter   filter theo status, khoảng thời gian
     * @param pageable thông tin phân trang
     * @return danh sách commissions
     */
    Page<CommissionResponse> getMyCommissions(CommissionFilterRequest filter, Pageable pageable);

    // ==================== Summary Operations ====================

    /**
     * Lấy tổng hợp hoa hồng theo employeeCode
     * Dùng cho ADMIN_TAMABEE, MANAGER_TAMABEE
     *
     * @param employeeCode employee code của nhân viên Tamabee
     * @return tổng hợp hoa hồng
     */
    CommissionSummaryResponse getSummary(String employeeCode);

    /**
     * Lấy tổng hợp hoa hồng toàn bộ hệ thống
     * Dùng cho ADMIN_TAMABEE, MANAGER_TAMABEE
     *
     * @return tổng hợp hoa hồng toàn bộ
     */
    CommissionOverallSummaryResponse getOverallSummary();

    /**
     * Lấy tổng hợp hoa hồng của nhân viên hiện tại (từ JWT token)
     * Dùng cho EMPLOYEE_TAMABEE
     *
     * @return tổng hợp hoa hồng
     */
    CommissionSummaryResponse getMySummary();

    // ==================== Admin Operations ====================

    /**
     * Đánh dấu commission đã thanh toán
     * Chỉ ADMIN_TAMABEE, MANAGER_TAMABEE có quyền
     *
     * @param id ID của commission
     * @return commission đã cập nhật
     */
    CommissionResponse markAsPaid(Long id);

    // ==================== Eligibility Operations ====================

    /**
     * Tính toán eligibility của commission dựa trên billing
     * Commission eligible khi: company_total_billing > pending_commission_amount
     *
     * @param commissionId ID của commission
     * @return true nếu eligible
     */
    boolean calculateEligibility(Long commissionId);

    /**
     * Recalculate eligibility khi có billing mới
     * Được gọi sau mỗi billing transaction
     *
     * @param companyId ID của company vừa có billing
     */
    void recalculateOnBilling(Long companyId);

    /**
     * Lấy danh sách commissions với eligibility status
     *
     * @param pageable thông tin phân trang
     * @return Page of commissions with eligibility
     */
    Page<CommissionResponse> getCommissionsWithEligibility(Pageable pageable);
}
