package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho thông tin cấu hình hoa hồng
 * Dùng để hiển thị thông tin hoa hồng cho nhân viên Tamabee
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSettingsResponse {

    // Số tiền hoa hồng cố định (JPY)
    private Integer commissionAmount;

    // Số tháng miễn phí khi có mã giới thiệu
    private Integer referralBonusMonths;

    // Số tháng miễn phí cho company mới
    private Integer freeTrialMonths;
}
