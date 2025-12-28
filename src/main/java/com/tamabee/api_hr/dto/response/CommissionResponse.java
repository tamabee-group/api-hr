package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.CommissionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho hoa hồng giới thiệu
 * Status: PENDING (chưa đủ điều kiện), ELIGIBLE (đủ điều kiện), PAID (đã thanh
 * toán)
 */
@Data
public class CommissionResponse {

    private Long id;

    // Employee code của nhân viên Tamabee nhận hoa hồng
    private String employeeCode;

    // Tên nhân viên Tamabee
    private String employeeName;

    // Company được giới thiệu
    private Long companyId;

    private String companyName;

    // Số tiền hoa hồng
    private BigDecimal amount;

    // Trạng thái: PENDING, ELIGIBLE, PAID
    private CommissionStatus status;

    // Tổng billing của company tại thời điểm tạo commission
    private BigDecimal companyBillingAtCreation;

    // Thời điểm thanh toán hoa hồng
    private LocalDateTime paidAt;

    // Employee code của người thanh toán hoa hồng
    private String paidBy;

    // Tên người thanh toán hoa hồng
    private String paidByName;

    private LocalDateTime createdAt;
}
