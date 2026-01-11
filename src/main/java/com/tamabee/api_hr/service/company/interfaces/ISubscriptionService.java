package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.response.SubscriptionStatusResponse;

/**
 * Service quản lý subscription của company
 * Bao gồm: xem trạng thái, đổi plan, kiểm tra eligibility
 */
public interface ISubscriptionService {

    /**
     * Lấy trạng thái subscription của company hiện tại (từ TenantContext)
     *
     * @param language ngôn ngữ để hiển thị tên plan
     * @return thông tin subscription
     */
    SubscriptionStatusResponse getSubscriptionStatus(String language);

    /**
     * Thay đổi plan của company hiện tại (từ TenantContext)
     *
     * @param newPlanId ID của plan mới
     * @param language ngôn ngữ để hiển thị tên plan
     * @return thông tin subscription sau khi đổi
     */
    SubscriptionStatusResponse changePlan(Long newPlanId, String language);

    /**
     * Đếm số nhân viên active của company hiện tại
     *
     * @return số nhân viên active
     */
    int countActiveEmployees();

    /**
     * Kích hoạt lại company sau khi nạp tiền đủ
     * Kiểm tra balance >= plan price trước khi reactivate
     *
     * @param language ngôn ngữ để hiển thị tên plan
     * @return thông tin subscription sau khi reactivate
     */
    SubscriptionStatusResponse reactivate(String language);
}
