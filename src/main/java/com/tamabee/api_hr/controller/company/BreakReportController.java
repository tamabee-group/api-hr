package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.response.attendance.DailyBreakReportResponse;
import com.tamabee.api_hr.dto.response.attendance.MonthlyBreakReportResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IBreakReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Controller quản lý báo cáo giờ giải lao cho admin/manager công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/reports/break")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class BreakReportController {

    private final IBreakReportService breakReportService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Lấy báo cáo giờ giải lao hàng ngày
     * GET /api/company/reports/break/daily?date=2025-01-15
     */
    @GetMapping("/daily")
    public ResponseEntity<BaseResponse<DailyBreakReportResponse>> getDailyBreakReport(
            @RequestParam String date) {
        LocalDate reportDate = LocalDate.parse(date, DATE_FORMATTER);
        DailyBreakReportResponse report = breakReportService.generateDailyBreakReport(reportDate);
        return ResponseEntity.ok(BaseResponse.success(report, "Lấy báo cáo giờ giải lao hàng ngày thành công"));
    }

    /**
     * Lấy báo cáo giờ giải lao hàng tháng
     * GET /api/company/reports/break/monthly?yearMonth=2025-01
     */
    @GetMapping("/monthly")
    public ResponseEntity<BaseResponse<MonthlyBreakReportResponse>> getMonthlyBreakReport(
            @RequestParam String yearMonth) {
        YearMonth reportMonth = YearMonth.parse(yearMonth, MONTH_FORMATTER);
        MonthlyBreakReportResponse report = breakReportService.generateMonthlyBreakReport(reportMonth);
        return ResponseEntity.ok(BaseResponse.success(report, "Lấy báo cáo giờ giải lao hàng tháng thành công"));
    }
}
