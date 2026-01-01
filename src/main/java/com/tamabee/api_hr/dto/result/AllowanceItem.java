package com.tamabee.api_hr.dto.result;

import com.tamabee.api_hr.enums.AllowanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Chi tiết một khoản phụ cấp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceItem {

    private String code;

    private String name;

    private AllowanceType type;

    private BigDecimal amount;

    @Builder.Default
    private Boolean taxable = true;

    // Lý do không đủ điều kiện (nếu có)
    private String ineligibleReason;
}
