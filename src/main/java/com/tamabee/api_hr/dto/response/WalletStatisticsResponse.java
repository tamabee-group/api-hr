package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO cho thống kê tổng hợp wallet (Admin Dashboard)
 */
@Data
public class WalletStatisticsResponse {

    // Tổng số công ty
    private Long totalCompanies;

    // Tổng số dư tất cả wallet
    private BigDecimal totalBalance;

    // Số công ty có số dư thấp (< 1 tháng subscription)
    private Long companiesWithLowBalance;

    // Số công ty đang trong thời gian miễn phí
    private Long companiesInFreeTrial;

    // Tổng số tiền đã nạp (tất cả công ty)
    private BigDecimal totalDeposits;

    // Tổng số tiền đã billing (tất cả công ty)
    private BigDecimal totalBillings;
}
