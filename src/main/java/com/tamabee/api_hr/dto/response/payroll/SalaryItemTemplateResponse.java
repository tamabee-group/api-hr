package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.SalaryItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho template phụ cấp/khấu trừ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryItemTemplateResponse {

    private Long id;
    private String name;
    private SalaryItemType type;
}
