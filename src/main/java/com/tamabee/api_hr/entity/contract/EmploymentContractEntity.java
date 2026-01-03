package com.tamabee.api_hr.entity.contract;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ContractType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Entity lưu trữ hợp đồng lao động của nhân viên.
 * Quản lý thông tin hợp đồng, thời hạn, và liên kết với cấu hình lương.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employment_contracts", indexes = {
        @Index(name = "idx_contract_employee_id", columnList = "employeeId"),
        @Index(name = "idx_contract_company_id", columnList = "companyId"),
        @Index(name = "idx_contract_deleted", columnList = "deleted"),
        @Index(name = "idx_contract_status", columnList = "status"),
        @Index(name = "idx_contract_end_date", columnList = "endDate"),
        @Index(name = "idx_contract_employee_status", columnList = "employeeId, status"),
        @Index(name = "idx_contract_company_status", columnList = "companyId, status")
})
public class EmploymentContractEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Loại hợp đồng
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    // Số hợp đồng
    @Column(length = 100)
    private String contractNumber;

    // Ngày bắt đầu hợp đồng
    @Column(nullable = false)
    private LocalDate startDate;

    // Ngày kết thúc hợp đồng
    private LocalDate endDate;

    // ID cấu hình lương liên kết
    private Long salaryConfigId;

    // Trạng thái hợp đồng
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    // Lý do chấm dứt hợp đồng
    @Column(length = 500)
    private String terminationReason;

    // Ngày chấm dứt hợp đồng
    private LocalDate terminatedAt;

    // Ghi chú
    @Column(length = 1000)
    private String notes;
}
