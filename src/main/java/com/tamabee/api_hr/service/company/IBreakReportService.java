package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.response.DailyBreakReportResponse;
import com.tamabee.api_hr.dto.response.MonthlyBreakReportResponse;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Service tạo báo cáo giờ giải lao của công ty.
 * Hỗ trợ báo cáo hàng ngày và hàng tháng.
 */
public interface IBreakReportService {

    /**
     * Tạo báo cáo giờ giải lao hàng ngày
     *
     * @param companyId ID công ty
     * @param date      ngày báo cáo
     * @return báo cáo giờ giải lao hàng ngày
     */
    DailyBreakReportResponse generateDailyBreakReport(Long companyId, LocalDate date);

    /**
     * Tạo báo cáo giờ giải lao hàng tháng
     *
     * @param companyId ID công ty
     * @param yearMonth tháng báo cáo
     * @return báo cáo giờ giải lao hàng tháng
     */
    MonthlyBreakReportResponse generateMonthlyBreakReport(Long companyId, YearMonth yearMonth);
}
