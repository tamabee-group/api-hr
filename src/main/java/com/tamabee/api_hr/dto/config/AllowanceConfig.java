package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình phụ cấp của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceConfig {

    @Builder.Default
    private List<AllowanceRule> allowances = new ArrayList<>();
}
