package com.tamabee.api_hr.dto.request.payroll;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để tạo kỳ lương mới
 * Có thể truyền year/month hoặc periodStart/periodEnd
 * Nếu chỉ truyền periodStart/periodEnd, year/month sẽ được extract từ periodStart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodRequest {

    @Min(value = 2020, message = "Năm phải từ 2020 trở đi")
    @Max(value = 2100, message = "Năm không được vượt quá 2100")
    private Integer year;

    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    private Integer month;

    // Ngày bắt đầu kỳ lương (tùy chọn, mặc định là ngày đầu tháng)
    private LocalDate periodStart;

    // Ngày kết thúc kỳ lương (tùy chọn, mặc định là ngày cuối tháng)
    private LocalDate periodEnd;
}
