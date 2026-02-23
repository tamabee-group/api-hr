package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tính toán giờ làm việc.
 * Giải lao luôn bị trừ khỏi giờ làm việc, không tính lương.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursResult {

    // Tổng số phút làm việc (chưa trừ break)
    @Builder.Default
    private Integer grossWorkingMinutes = 0;

    // Số phút làm việc thực tế (sau khi trừ break)
    @Builder.Default
    private Integer netWorkingMinutes = 0;

    // Tổng số phút giải lao thực tế
    @Builder.Default
    private Integer totalBreakMinutes = 0;

    // Số phút giải lao hiệu lực
    @Builder.Default
    private Integer effectiveBreakMinutes = 0;

    // Có phải ca đêm không
    @Builder.Default
    private Boolean isNightShift = false;

    // Có phải ca qua đêm không
    @Builder.Default
    private Boolean isOvernightShift = false;

    // Số phút làm trong giờ đêm (22:00-05:00)
    @Builder.Default
    private Integer nightMinutes = 0;

    // Số phút làm trong giờ thường (không phải đêm)
    @Builder.Default
    private Integer regularMinutes = 0;

    /**
     * Lấy số giờ làm việc thực tế (làm tròn)
     */
    public double getNetWorkingHours() {
        return netWorkingMinutes != null ? netWorkingMinutes / 60.0 : 0;
    }

    /**
     * Lấy số giờ làm việc gộp (làm tròn)
     */
    public double getGrossWorkingHours() {
        return grossWorkingMinutes != null ? grossWorkingMinutes / 60.0 : 0;
    }
}
