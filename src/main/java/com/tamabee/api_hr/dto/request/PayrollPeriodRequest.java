package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO để tạo kỳ lương mới
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodRequest {

    @NotNull(message = "Năm không được để trống")
    @Min(value = 2020, message = "Năm phải từ 2020 trở đi")
    @Max(value = 2100, message = "Năm không được vượt quá 2100")
    private Integer year;

    @NotNull(message = "Tháng không được để trống")
    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    private Integer month;

    // Ngày bắt đầu kỳ lương (tùy chọn, mặc định là ngày đầu tháng)
    private LocalDate periodStart;

    // Ngày kết thúc kỳ lương (tùy chọn, mặc định là ngày cuối tháng)
    private LocalDate periodEnd;
}
