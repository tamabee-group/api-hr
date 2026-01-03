package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.ReportQuery;
import com.tamabee.api_hr.dto.response.report.*;
import com.tamabee.api_hr.enums.ExportFormat;
import com.tamabee.api_hr.enums.ReportType;

/**
 * Service interface cho các báo cáo
 */
public interface IReportService {

    /**
     * Tạo báo cáo tổng hợp chấm công
     */
    AttendanceSummaryReport generateAttendanceSummary(Long companyId, ReportQuery query);

    /**
     * Tạo báo cáo làm thêm giờ
     */
    OvertimeReport generateOvertimeReport(Long companyId, ReportQuery query);

    /**
     * Tạo báo cáo tuân thủ nghỉ giải lao
     */
    BreakComplianceReport generateBreakComplianceReport(Long companyId, ReportQuery query);

    /**
     * Tạo báo cáo tổng hợp lương
     */
    PayrollSummaryReport generatePayrollSummary(Long companyId, ReportQuery query);

    /**
     * Tạo báo cáo phân tích chi phí
     */
    CostAnalysisReport generateCostAnalysis(Long companyId, ReportQuery query);

    /**
     * Tạo báo cáo sử dụng ca làm việc
     */
    ShiftUtilizationReport generateShiftUtilization(Long companyId, ReportQuery query);

    /**
     * Xuất báo cáo theo định dạng (CSV, PDF) với ngôn ngữ
     * 
     * @param type      loại báo cáo
     * @param companyId ID công ty
     * @param query     điều kiện truy vấn
     * @param format    định dạng xuất (CSV, PDF)
     * @param language  ngôn ngữ (vi, ja, en)
     */
    byte[] exportReport(ReportType type, Long companyId, ReportQuery query, ExportFormat format, String language);
}
