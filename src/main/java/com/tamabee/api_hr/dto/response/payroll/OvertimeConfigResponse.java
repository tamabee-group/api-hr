package com.tamabee.api_hr.dto.response.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Response chứa cấu hình tăng ca của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConfigResponse {

    // Bật/tắt tính overtime
    private Boolean overtimeEnabled;

    // Yêu cầu phê duyệt
    private Boolean requireApproval;

    // Số giờ làm việc tiêu chuẩn
    private Integer standardWorkingHours;

    // Giờ bắt đầu/kết thúc ca đêm
    private LocalTime nightStartTime;
    private LocalTime nightEndTime;

    // Overtime multipliers
    private BigDecimal regularOvertimeRate;
    private BigDecimal nightWorkRate;
    private BigDecimal nightOvertimeRate;
    private BigDecimal holidayOvertimeRate;
    private BigDecimal holidayNightOvertimeRate;
    private BigDecimal weekendOvertimeRate;

    // Sử dụng legal minimum hay custom
    private Boolean useLegalMinimum;

    // Locale cho legal requirements
    private String locale;

    // Giới hạn tăng ca
    private Integer maxOvertimeHoursPerDay;
    private Integer maxOvertimeHoursPerMonth;
}
