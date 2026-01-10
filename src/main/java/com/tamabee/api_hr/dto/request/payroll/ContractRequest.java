package com.tamabee.api_hr.dto.request.payroll;

import com.tamabee.api_hr.enums.ContractType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO cho việc tạo/cập nhật hợp đồng lao động
 */
@Data
public class ContractRequest {

    @NotNull(message = "Loại hợp đồng không được để trống")
    private ContractType contractType;

    // Số hợp đồng (tùy chọn)
    private String contractNumber;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    // Ngày kết thúc (null = vô thời hạn)
    private LocalDate endDate;

    // ID cấu hình lương liên kết (tùy chọn)
    private Long salaryConfigId;

    // Ghi chú
    private String notes;
}
