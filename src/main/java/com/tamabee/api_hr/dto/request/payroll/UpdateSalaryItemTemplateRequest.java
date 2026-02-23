package com.tamabee.api_hr.dto.request.payroll;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO cho việc cập nhật template phụ cấp/khấu trừ
 */
@Data
public class UpdateSalaryItemTemplateRequest {

    @NotBlank(message = "Tên template không được để trống")
    private String name;
}
