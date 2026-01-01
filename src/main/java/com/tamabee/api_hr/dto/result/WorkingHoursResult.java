package com.tamabee.api_hr.dto.result;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tính toán giờ làm việc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursResult {

    // Tổng số phút làm việc (chưa trừ break)
    @Builder.Default
    private Integer grossWorkingMinutes = 0;

    // Số phút làm việc thực tế (sau khi trừ break nếu unpaid)
    @Builder.Default
    private Integer netWorkingMinutes = 0;

    // Tổng số phút giải lao thực tế
    @Builder.Default
    private Integer totalBreakMinutes = 0;

    // Số phút giải lao hiệu lực (sau khi áp dụng min/max)
    @Builder.Default
    private Integer effectiveBreakMinutes = 0;

    // Loại giải lao áp dụng
    private BreakType breakType;

    // Có tuân thủ minimum break không
    @Builder.Default
    private Boolean breakCompliant = true;

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
