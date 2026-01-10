package com.tamabee.api_hr.service.admin.interfaces;

import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.ReferredCompanyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service cho Employee Tamabee xem và theo dõi company đã giới thiệu
 * Chỉ Employee Tamabee có quyền truy cập
 */
public interface IEmployeeReferralService {

    /**
     * Lấy danh sách companies mà employee đã giới thiệu
     * Bao gồm thông tin service usage và commission status
     *
     * @param employeeCode Mã nhân viên Tamabee
     * @param pageable     Thông tin phân trang
     * @return Page of referred companies with service usage
     */
    Page<ReferredCompanyResponse> getReferredCompanies(String employeeCode, Pageable pageable);

    /**
     * Lấy thống kê commission của employee
     * Bao gồm: total pending, eligible, paid amounts
     *
     * @param employeeCode Mã nhân viên Tamabee
     * @return Commission summary
     */
    CommissionSummaryResponse getCommissionSummary(String employeeCode);
}
