package com.tamabee.api_hr.service.company.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.wallet.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.wallet.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.wallet.DepositRequestResponse;

/**
 * Service interface cho company deposit requests
 * Sử dụng masterJdbcTemplate vì deposit_requests nằm trong master DB
 */
public interface ICompanyDepositService {

    /**
     * Lấy danh sách yêu cầu nạp tiền của company hiện tại
     */
    Page<DepositRequestResponse> getMyRequests(DepositFilterRequest filter, Pageable pageable);

    /**
     * Tạo yêu cầu nạp tiền mới
     */
    DepositRequestResponse create(DepositRequestCreateRequest request);

    /**
     * Hủy yêu cầu nạp tiền đang chờ duyệt
     */
    DepositRequestResponse cancel(Long id);
}
