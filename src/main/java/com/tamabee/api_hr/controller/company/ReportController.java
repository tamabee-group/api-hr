package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.ReportQuery;
import com.tamabee.api_hr.dto.response.report.*;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ContractType;
import com.tamabee.api_hr.enums.ExportFormat;
import com.tamabee.api_hr.enums.ReportType;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.SalaryType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller cho các báo cáo.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/reports")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class ReportController {

    private final IReportService reportService;
    private final UserRepository userRepository;

    /**
     * Tạo báo cáo tổng hợp chấm công
     * GET /api/company/reports/attendance-summary
     */
    @GetMapping("/attendance-summary")
    public ResponseEntity<BaseResponse<AttendanceSummaryReport>> getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        AttendanceSummaryReport report = reportService.generateAttendanceSummary(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo chấm công thành công"));
    }

    /**
     * Tạo báo cáo làm thêm giờ
     * GET /api/company/reports/overtime
     */
    @GetMapping("/overtime")
    public ResponseEntity<BaseResponse<OvertimeReport>> getOvertimeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        OvertimeReport report = reportService.generateOvertimeReport(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo làm thêm giờ thành công"));
    }

    /**
     * Tạo báo cáo tuân thủ nghỉ giải lao
     * GET /api/company/reports/break-compliance
     */
    @GetMapping("/break-compliance")
    public ResponseEntity<BaseResponse<BreakComplianceReport>> getBreakComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        BreakComplianceReport report = reportService.generateBreakComplianceReport(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo tuân thủ nghỉ giải lao thành công"));
    }

    /**
     * Tạo báo cáo tổng hợp lương
     * GET /api/company/reports/payroll-summary
     */
    @GetMapping("/payroll-summary")
    public ResponseEntity<BaseResponse<PayrollSummaryReport>> getPayrollSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        PayrollSummaryReport report = reportService.generatePayrollSummary(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo tổng hợp lương thành công"));
    }

    /**
     * Tạo báo cáo phân tích chi phí
     * GET /api/company/reports/cost-analysis
     */
    @GetMapping("/cost-analysis")
    public ResponseEntity<BaseResponse<CostAnalysisReport>> getCostAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        CostAnalysisReport report = reportService.generateCostAnalysis(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo phân tích chi phí thành công"));
    }

    /**
     * Tạo báo cáo sử dụng ca làm việc
     * GET /api/company/reports/shift-utilization
     */
    @GetMapping("/shift-utilization")
    public ResponseEntity<BaseResponse<ShiftUtilizationReport>> getShiftUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);
        ShiftUtilizationReport report = reportService.generateShiftUtilization(companyId, query);
        return ResponseEntity.ok(BaseResponse.success(report, "Tạo báo cáo sử dụng ca làm việc thành công"));
    }

    /**
     * Xuất báo cáo theo định dạng (CSV, PDF)
     * GET /api/company/reports/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam ReportType type,
            @RequestParam ExportFormat format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam(required = false) List<Long> departmentIds,
            @RequestParam(required = false) List<ContractType> contractTypes,
            @RequestParam(required = false) List<SalaryType> salaryTypes,
            @RequestParam(required = false) List<Long> shiftTemplateIds,
            @RequestParam(defaultValue = "vi") String language) {
        Long companyId = getCurrentUserCompanyId();
        ReportQuery query = buildReportQuery(startDate, endDate, employeeIds, departmentIds,
                contractTypes, salaryTypes, shiftTemplateIds);

        byte[] data = reportService.exportReport(type, companyId, query, format, language);

        String filename = buildExportFilename(type, startDate, endDate, format);
        String contentType = format == ExportFormat.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "text/csv; charset=UTF-8";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    /**
     * Build ReportQuery từ các parameters
     */
    private ReportQuery buildReportQuery(
            LocalDate startDate,
            LocalDate endDate,
            List<Long> employeeIds,
            List<Long> departmentIds,
            List<ContractType> contractTypes,
            List<SalaryType> salaryTypes,
            List<Long> shiftTemplateIds) {
        return ReportQuery.builder()
                .startDate(startDate)
                .endDate(endDate)
                .employeeIds(employeeIds)
                .departmentIds(departmentIds)
                .contractTypes(contractTypes)
                .salaryTypes(salaryTypes)
                .shiftTemplateIds(shiftTemplateIds)
                .build();
    }

    /**
     * Build filename cho file export
     */
    private String buildExportFilename(ReportType type, LocalDate startDate, LocalDate endDate, ExportFormat format) {
        String dateRange = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String extension = format == ExportFormat.PDF ? ".pdf" : ".csv";
        return type.name().toLowerCase() + "_" + dateRange + extension;
    }

    /**
     * Lấy companyId của user đang đăng nhập
     */
    private Long getCurrentUserCompanyId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getCompanyId();
    }
}
