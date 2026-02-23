package com.tamabee.api_hr.dto.request.payroll;

import com.tamabee.api_hr.enums.SalaryItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO cho việc tạo template phụ cấp/khấu trừ
 */
@Data
public class CreateSalaryItemTemplateRequest {

    @NotBlank(message = "Tên template không được để trống")
    private String name;

    @NotNull(message = "Loại template không được để trống")
    private SalaryItemType type;
}
