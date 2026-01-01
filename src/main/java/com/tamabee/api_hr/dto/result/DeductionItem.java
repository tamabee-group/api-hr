package com.tamabee.api_hr.dto.result;

import com.tamabee.api_hr.enums.DeductionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Chi tiết một khoản khấu trừ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionItem {

    private String code;

    private String name;

    private DeductionType type;

    private BigDecimal amount;

    // Thứ tự áp dụng
    private Integer order;
}
