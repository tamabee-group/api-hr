package com.tamabee.api_hr.service.admin.interfaces;

import java.time.LocalDateTime;

/**
 * Service xử lý billing tự động hàng tháng
 * Trừ tiền subscription từ wallet của company khi đến ngày billing
 * Xử lý các trường hợp: free trial, insufficient balance, gửi email thông báo
 */
public interface IBillingService {

    /**
     * Xử lý billing hàng tháng cho tất cả company
     * Query các company có nextBillingDate <= now và đã hết free trial
     * Trừ tiền subscription, tạo transaction, cập nhật billing dates
     * Gửi email thông báo thành công hoặc insufficient balance
     */
    void processMonthlyBilling();

    /**
     * Kiểm tra company có đang trong thời gian miễn phí không
     *
     * @param companyId ID của company
     * @return true nếu đang trong free trial, false nếu đã hết
     */
    boolean isInFreeTrial(Long companyId);

    /**
     * Tính ngày kết thúc free trial cho company
     * Dựa trên freeTrialMonths và referralBonusMonths từ settings
     *
     * @param companyId ID của company
     * @return ngày kết thúc free trial
     */
    LocalDateTime calculateFreeTrialEndDate(Long companyId);

    /**
     * Tính ngày kết thúc free trial dựa trên ngày tạo company và có referral hay
     * không
     *
     * @param companyCreatedAt ngày tạo company
     * @param hasReferral      true nếu company có mã giới thiệu
     * @return ngày kết thúc free trial
     */
    LocalDateTime calculateFreeTrialEndDate(LocalDateTime companyCreatedAt, boolean hasReferral);
}
