package com.tamabee.api_hr.dto.request.payroll;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request DTO cho việc cập nhật phụ cấp/khấu trừ của nhân viên
 */
@Data
public class UpdateSalaryItemRequest {

    @NotNull(message = "Template ID không được để trống")
    private Long templateId;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
}
