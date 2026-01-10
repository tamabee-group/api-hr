package com.tamabee.api_hr.dto.response.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO cho tổng hợp hoa hồng theo nhân viên
 * Bao gồm: total pending, eligible, paid amounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSummaryResponse {

    // Employee code của nhân viên Tamabee
    private String employeeCode;

    // Tên nhân viên Tamabee
    private String employeeName;

    // Tổng số referrals (companies đã giới thiệu)
    private Integer totalReferrals;

    // Tổng số hoa hồng
    private Long totalCommissions;

    // Tổng số tiền hoa hồng
    private BigDecimal totalAmount;

    // Số hoa hồng đang chờ (PENDING - chưa đủ điều kiện)
    private Long pendingCommissions;

    // Số tiền hoa hồng đang chờ
    private BigDecimal pendingAmount;

    // Số hoa hồng đủ điều kiện (ELIGIBLE - đủ điều kiện nhưng chưa thanh toán)
    private Long eligibleCommissions;

    // Số tiền hoa hồng đủ điều kiện
    private BigDecimal eligibleAmount;

    // Số hoa hồng đã thanh toán
    private Long paidCommissions;

    // Số tiền hoa hồng đã thanh toán
    private BigDecimal paidAmount;
}
