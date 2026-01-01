package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Điều kiện để nhận phụ cấp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceCondition {

    // Số ngày làm việc tối thiểu
    private Integer minWorkingDays;

    // Số giờ làm việc tối thiểu
    private Integer minWorkingHours;

    // Không có ngày vắng mặt
    private Boolean noAbsence;

    // Không đi muộn
    private Boolean noLateArrival;

    // Không về sớm
    private Boolean noEarlyLeave;
}
