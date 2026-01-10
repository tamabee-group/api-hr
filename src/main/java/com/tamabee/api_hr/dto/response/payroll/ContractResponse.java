package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ContractType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho thông tin hợp đồng lao động
 */
@Data
@Builder
public class ContractResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long companyId;

    // Thông tin hợp đồng
    private ContractType contractType;
    private String contractNumber;
    private LocalDate startDate;
    private LocalDate endDate;

    // Liên kết cấu hình lương
    private Long salaryConfigId;

    // Trạng thái
    private ContractStatus status;

    // Thông tin chấm dứt
    private String terminationReason;
    private LocalDate terminatedAt;

    // Ghi chú
    private String notes;

    // Số ngày còn lại (nếu có endDate)
    private Integer daysUntilExpiry;

    // Audit fields
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
