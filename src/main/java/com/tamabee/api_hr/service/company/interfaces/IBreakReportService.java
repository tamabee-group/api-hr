package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.response.attendance.DailyBreakReportResponse;
import com.tamabee.api_hr.dto.response.attendance.MonthlyBreakReportResponse;

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
     * @param date ngày báo cáo
     * @return báo cáo giờ giải lao hàng ngày
     */
    DailyBreakReportResponse generateDailyBreakReport(LocalDate date);

    /**
     * Tạo báo cáo giờ giải lao hàng tháng
     *
     * @param yearMonth tháng báo cáo
     * @return báo cáo giờ giải lao hàng tháng
     */
    MonthlyBreakReportResponse generateMonthlyBreakReport(YearMonth yearMonth);
}
