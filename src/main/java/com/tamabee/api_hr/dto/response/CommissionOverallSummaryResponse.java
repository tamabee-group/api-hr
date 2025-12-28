package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO cho tổng hợp hoa hồng toàn bộ hệ thống
 * Dùng cho Admin Tamabee xem tổng quan
 */
@Data
public class CommissionOverallSummaryResponse {

    // Tổng số tiền hoa hồng đang chờ thanh toán
    private BigDecimal totalPending;

    // Tổng số tiền hoa hồng đủ điều kiện (chờ thanh toán)
    private BigDecimal totalEligible;

    // Tổng số tiền hoa hồng đã thanh toán
    private BigDecimal totalPaid;

    // Tổng số tiền hoa hồng
    private BigDecimal totalAmount;

    // Thống kê theo nhân viên
    private List<EmployeeSummary> byEmployee;

    // Thống kê theo tháng
    private List<MonthSummary> byMonth;

    /**
     * Thống kê hoa hồng theo nhân viên
     */
    @Data
    public static class EmployeeSummary {
        // Mã nhân viên
        private String employeeCode;

        // Tên nhân viên
        private String employeeName;

        // Số lượng commission
        private Long count;

        // Tổng số tiền pending
        private BigDecimal totalPending;

        // Tổng số tiền eligible
        private BigDecimal totalEligible;

        // Tổng số tiền paid
        private BigDecimal totalPaid;

        // Tổng số tiền
        private BigDecimal totalAmount;
    }

    /**
     * Thống kê hoa hồng theo tháng
     */
    @Data
    public static class MonthSummary {
        // Tháng (format: YYYY-MM)
        private String month;

        // Tổng số tiền hoa hồng trong tháng
        private BigDecimal totalAmount;

        // Số tiền đang chờ thanh toán
        private BigDecimal totalPending;

        // Số tiền đã thanh toán
        private BigDecimal totalPaid;

        // Số lượng commission
        private Long count;
    }
}
