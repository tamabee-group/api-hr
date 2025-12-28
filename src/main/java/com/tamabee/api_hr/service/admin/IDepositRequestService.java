package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.request.RejectRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service quản lý yêu cầu nạp tiền
 * Hỗ trợ tạo, xem, duyệt và từ chối yêu cầu nạp tiền
 */
public interface IDepositRequestService {

    // ==================== Company Operations ====================

    /**
     * Tạo yêu cầu nạp tiền mới
     * Dùng cho ADMIN_COMPANY tạo yêu cầu nạp tiền
     * CompanyId và requestedBy được lấy từ JWT token
     *
     * @param request thông tin yêu cầu nạp tiền (amount, transferProofUrl)
     * @return thông tin yêu cầu đã tạo
     */
    DepositRequestResponse create(DepositRequestCreateRequest request);

    /**
     * Lấy danh sách yêu cầu nạp tiền của company hiện tại (từ JWT token)
     * Dùng cho ADMIN_COMPANY xem yêu cầu của mình
     *
     * @param filter   filter theo status
     * @param pageable thông tin phân trang
     * @return danh sách yêu cầu nạp tiền
     */
    Page<DepositRequestResponse> getMyRequests(DepositFilterRequest filter, Pageable pageable);

    // ==================== Admin Operations ====================

    /**
     * Lấy tất cả yêu cầu nạp tiền (phân trang)
     * Dùng cho ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE xem tất cả yêu cầu
     *
     * @param filter   filter theo status và companyId
     * @param pageable thông tin phân trang
     * @return danh sách yêu cầu nạp tiền
     */
    Page<DepositRequestResponse> getAll(DepositFilterRequest filter, Pageable pageable);

    /**
     * Lấy chi tiết yêu cầu nạp tiền theo id
     *
     * @param id ID của yêu cầu nạp tiền
     * @return thông tin chi tiết yêu cầu
     */
    DepositRequestResponse getById(Long id);

    /**
     * Duyệt yêu cầu nạp tiền
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền
     * - Cập nhật status thành APPROVED
     * - Cộng tiền vào wallet của company
     * - Tạo transaction record
     * - Gửi email thông báo
     *
     * @param id ID của yêu cầu nạp tiền
     * @return thông tin yêu cầu đã duyệt
     */
    DepositRequestResponse approve(Long id);

    /**
     * Từ chối yêu cầu nạp tiền
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền
     * - Cập nhật status thành REJECTED
     * - Ghi nhận lý do từ chối
     * - KHÔNG thay đổi balance của wallet
     *
     * @param id      ID của yêu cầu nạp tiền
     * @param request thông tin từ chối (rejectionReason)
     * @return thông tin yêu cầu đã từ chối
     */
    DepositRequestResponse reject(Long id, RejectRequest request);
}
