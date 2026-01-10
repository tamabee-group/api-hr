package com.tamabee.api_hr.service.admin;

import java.math.BigDecimal;
import java.util.List;

import com.tamabee.api_hr.dto.request.SettingUpdateRequest;
import com.tamabee.api_hr.dto.response.SettingResponse;

/**
 * Service quản lý cấu hình hệ thống Tamabee
 * Hỗ trợ đọc và cập nhật các thông số hệ thống
 * Các giá trị được cache để tối ưu performance
 */
public interface ISettingService {

    /**
     * Lấy setting theo key
     *
     * @param key setting key
     * @return thông tin setting
     */
    SettingResponse get(String key);

    /**
     * Cập nhật setting theo key
     * Chỉ ADMIN_TAMABEE có quyền cập nhật
     *
     * @param key     setting key
     * @param request thông tin cập nhật
     * @return setting đã cập nhật
     */
    SettingResponse update(String key, SettingUpdateRequest request);

    /**
     * Lấy tất cả settings
     *
     * @return danh sách settings
     */
    List<SettingResponse> getAll();

    /**
     * Lấy số tháng miễn phí cho company mới (cached)
     *
     * @return số tháng miễn phí
     */
    int getFreeTrialMonths();

    /**
     * Lấy số tháng miễn phí thêm khi có mã giới thiệu (cached)
     *
     * @return số tháng bonus
     */
    int getReferralBonusMonths();

    /**
     * Lấy tỷ lệ hoa hồng giới thiệu (cached)
     *
     * @return tỷ lệ hoa hồng (ví dụ: 0.10 = 10%)
     */
    BigDecimal getCommissionRate();

    /**
     * Lấy giá mỗi nhân viên cho gói Custom (cached)
     *
     * @return giá mỗi nhân viên (JPY)
     */
    int getCustomPricePerEmployee();
}
