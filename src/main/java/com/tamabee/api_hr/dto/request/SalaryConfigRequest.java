package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.SalaryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO cho việc tạo/cập nhật cấu hình lương nhân viên
 */
@Data
public class SalaryConfigRequest {

    @NotNull(message = "Loại lương không được để trống")
    private SalaryType salaryType;

    // Lương tháng (bắt buộc cho MONTHLY)
    @Positive(message = "Lương tháng phải lớn hơn 0")
    private BigDecimal monthlySalary;

    // Lương ngày (bắt buộc cho DAILY)
    @Positive(message = "Lương ngày phải lớn hơn 0")
    private BigDecimal dailyRate;

    // Lương giờ (bắt buộc cho HOURLY)
    @Positive(message = "Lương giờ phải lớn hơn 0")
    private BigDecimal hourlyRate;

    // Lương theo ca (bắt buộc cho SHIFT_BASED)
    @Positive(message = "Lương theo ca phải lớn hơn 0")
    private BigDecimal shiftRate;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDate effectiveFrom;

    // Ghi chú
    private String note;
}
