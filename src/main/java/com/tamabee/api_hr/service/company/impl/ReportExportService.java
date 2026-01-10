package com.tamabee.api_hr.service.company.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tamabee.api_hr.dto.response.report.*;
import com.tamabee.api_hr.util.ReportLabels;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Service xuất báo cáo ra CSV và PDF với hỗ trợ đa ngôn ngữ
 */
@Slf4j
@Service
public class ReportExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== Attendance Summary ====================

    public byte[] exportAttendanceSummaryToCsv(AttendanceSummaryReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.ATTENDANCE_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (EmployeeAttendanceSummary emp : report.getEmployeeSummaries()) {
                printer.printRecord(
                        emp.getEmployeeCode(), emp.getEmployeeName(),
                        emp.getTotalWorkingDays(), emp.getPresentDays(), emp.getAbsentDays(),
                        emp.getLateCount(), emp.getTotalLateMinutes(),
                        emp.getEarlyLeaveCount(), emp.getTotalEarlyLeaveMinutes(),
                        emp.getTotalWorkingMinutes(), emp.getAverageWorkingMinutesPerDay());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportAttendanceSummaryToPdf(AttendanceSummaryReport report, String language)
            throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.ATTENDANCE_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %d | %s: %.2f%%",
                ReportLabels.get(ReportLabels.TOTAL_EMPLOYEES_LABEL, language), report.getTotalEmployees(),
                ReportLabels.get(ReportLabels.ATTENDANCE_RATE_LABEL, language), report.getAttendanceRate()));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.ATTENDANCE_HEADERS, language);
        PdfPTable table = createTable(headers);

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (EmployeeAttendanceSummary emp : report.getEmployeeSummaries()) {
            addCell(table, emp.getEmployeeCode(), dataFont);
            addCell(table, emp.getEmployeeName(), dataFont);
            addCell(table, String.valueOf(emp.getTotalWorkingDays()), dataFont);
            addCell(table, String.valueOf(emp.getPresentDays()), dataFont);
            addCell(table, String.valueOf(emp.getAbsentDays()), dataFont);
            addCell(table, String.valueOf(emp.getLateCount()), dataFont);
            addCell(table, String.valueOf(emp.getTotalLateMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getEarlyLeaveCount()), dataFont);
            addCell(table, String.valueOf(emp.getTotalEarlyLeaveMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getTotalWorkingMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getAverageWorkingMinutesPerDay()), dataFont);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    // ==================== Overtime ====================

    public byte[] exportOvertimeToCsv(OvertimeReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.OVERTIME_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (EmployeeOvertimeSummary emp : report.getEmployeeSummaries()) {
                printer.printRecord(
                        emp.getEmployeeCode(), emp.getEmployeeName(),
                        emp.getRegularOvertimeMinutes(), emp.getNightOvertimeMinutes(),
                        emp.getHolidayOvertimeMinutes(), emp.getWeekendOvertimeMinutes(),
                        emp.getTotalOvertimeMinutes());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportOvertimeToPdf(OvertimeReport report, String language) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.OVERTIME_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %d | %s: %d min",
                ReportLabels.get(ReportLabels.EMPLOYEES_WITH_OT_LABEL, language),
                report.getTotalEmployeesWithOvertime(),
                ReportLabels.get(ReportLabels.TOTAL_OT_LABEL, language), report.getTotalOvertimeMinutes()));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.OVERTIME_HEADERS, language);
        PdfPTable table = createTable(headers);

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (EmployeeOvertimeSummary emp : report.getEmployeeSummaries()) {
            addCell(table, emp.getEmployeeCode(), dataFont);
            addCell(table, emp.getEmployeeName(), dataFont);
            addCell(table, String.valueOf(emp.getRegularOvertimeMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getNightOvertimeMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getHolidayOvertimeMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getWeekendOvertimeMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getTotalOvertimeMinutes()), dataFont);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    // ==================== Break Compliance ====================

    public byte[] exportBreakComplianceToCsv(BreakComplianceReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.BREAK_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (EmployeeBreakSummary emp : report.getEmployeeSummaries()) {
                printer.printRecord(
                        emp.getEmployeeCode(), emp.getEmployeeName(),
                        emp.getTotalBreakCount(), emp.getTotalBreakMinutes(),
                        emp.getAverageBreakMinutesPerDay(),
                        emp.getCompliantBreakCount(), emp.getNonCompliantBreakCount(),
                        emp.getComplianceRate());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportBreakComplianceToPdf(BreakComplianceReport report, String language) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.BREAK_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %d | %s: %.2f%%",
                ReportLabels.get(ReportLabels.TOTAL_EMPLOYEES_LABEL, language), report.getTotalEmployees(),
                ReportLabels.get(ReportLabels.COMPLIANCE_RATE_LABEL, language), report.getOverallComplianceRate()));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.BREAK_HEADERS, language);
        PdfPTable table = createTable(headers);

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (EmployeeBreakSummary emp : report.getEmployeeSummaries()) {
            addCell(table, emp.getEmployeeCode(), dataFont);
            addCell(table, emp.getEmployeeName(), dataFont);
            addCell(table, String.valueOf(emp.getTotalBreakCount()), dataFont);
            addCell(table, String.valueOf(emp.getTotalBreakMinutes()), dataFont);
            addCell(table, String.valueOf(emp.getAverageBreakMinutesPerDay()), dataFont);
            addCell(table, String.valueOf(emp.getCompliantBreakCount()), dataFont);
            addCell(table, String.valueOf(emp.getNonCompliantBreakCount()), dataFont);
            addCell(table, String.format("%.2f", emp.getComplianceRate()), dataFont);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    // ==================== Payroll Summary ====================

    public byte[] exportPayrollSummaryToCsv(PayrollSummaryReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.PAYROLL_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (EmployeePayrollSummary emp : report.getEmployeeSummaries()) {
                printer.printRecord(
                        emp.getEmployeeCode(), emp.getEmployeeName(),
                        emp.getSalaryType(), emp.getBaseSalary(), emp.getCalculatedBaseSalary(),
                        emp.getOvertimePay(), emp.getTotalAllowances(), emp.getTotalDeductions(),
                        emp.getGrossSalary(), emp.getNetSalary());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportPayrollSummaryToPdf(PayrollSummaryReport report, String language) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.PAYROLL_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %d | %s: %s | %s: %s",
                ReportLabels.get(ReportLabels.TOTAL_EMPLOYEES_LABEL, language), report.getTotalEmployees(),
                ReportLabels.get(ReportLabels.TOTAL_GROSS_LABEL, language), report.getTotalGrossSalary(),
                ReportLabels.get(ReportLabels.TOTAL_NET_LABEL, language), report.getTotalNetSalary()));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.PAYROLL_HEADERS, language);
        PdfPTable table = createTable(headers);

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (EmployeePayrollSummary emp : report.getEmployeeSummaries()) {
            addCell(table, emp.getEmployeeCode(), dataFont);
            addCell(table, emp.getEmployeeName(), dataFont);
            addCell(table, emp.getSalaryType() != null ? emp.getSalaryType().name() : "", dataFont);
            addCell(table, formatBigDecimal(emp.getBaseSalary()), dataFont);
            addCell(table, formatBigDecimal(emp.getCalculatedBaseSalary()), dataFont);
            addCell(table, formatBigDecimal(emp.getOvertimePay()), dataFont);
            addCell(table, formatBigDecimal(emp.getTotalAllowances()), dataFont);
            addCell(table, formatBigDecimal(emp.getTotalDeductions()), dataFont);
            addCell(table, formatBigDecimal(emp.getGrossSalary()), dataFont);
            addCell(table, formatBigDecimal(emp.getNetSalary()), dataFont);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    // ==================== Shift Utilization ====================

    public byte[] exportShiftUtilizationToCsv(ShiftUtilizationReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.SHIFT_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (ShiftTemplateSummary shift : report.getShiftSummaries()) {
                printer.printRecord(
                        shift.getShiftName(), shift.getStartTime(), shift.getEndTime(),
                        shift.getTotalAssignments(), shift.getCompletedAssignments(),
                        shift.getCancelledAssignments(), shift.getSwappedAssignments(),
                        shift.getUtilizationRate(), shift.getCompletionRate());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportShiftUtilizationToPdf(ShiftUtilizationReport report, String language) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.SHIFT_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %d | %s: %.2f%%",
                ReportLabels.get(ReportLabels.TOTAL_ASSIGNMENTS_LABEL, language), report.getTotalShiftAssignments(),
                ReportLabels.get(ReportLabels.COMPLETION_RATE_LABEL, language), report.getShiftCompletionRate()));
        addSummaryLine(document, String.format("%s: %d | %s: %.2f%%",
                ReportLabels.get(ReportLabels.SWAP_REQUESTS_LABEL, language), report.getTotalSwapRequests(),
                ReportLabels.get(ReportLabels.APPROVAL_RATE_LABEL, language), report.getSwapApprovalRate()));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.SHIFT_HEADERS, language);
        PdfPTable table = createTable(headers);

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (ShiftTemplateSummary shift : report.getShiftSummaries()) {
            addCell(table, shift.getShiftName(), dataFont);
            addCell(table, shift.getStartTime() != null ? shift.getStartTime().toString() : "", dataFont);
            addCell(table, shift.getEndTime() != null ? shift.getEndTime().toString() : "", dataFont);
            addCell(table, String.valueOf(shift.getTotalAssignments()), dataFont);
            addCell(table, String.valueOf(shift.getCompletedAssignments()), dataFont);
            addCell(table, String.valueOf(shift.getCancelledAssignments()), dataFont);
            addCell(table, String.valueOf(shift.getSwappedAssignments()), dataFont);
            addCell(table, String.format("%.2f", shift.getUtilizationRate()), dataFont);
            addCell(table, String.format("%.2f", shift.getCompletionRate()), dataFont);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    // ==================== Cost Analysis ====================

    public byte[] exportCostAnalysisToCsv(CostAnalysisReport report, String language) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] headers = ReportLabels.getHeaders(ReportLabels.COST_HEADERS, language);

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            // Theo loại lương
            printer.printRecord(ReportLabels.get(ReportLabels.BY_SALARY_TYPE_LABEL, language),
                    "", "", "", "", "", "");
            for (CostBySalaryType cost : report.getCostBySalaryType()) {
                printer.printRecord(
                        cost.getSalaryType() != null ? cost.getSalaryType().name() : "",
                        cost.getEmployeeCount(),
                        formatBigDecimal(cost.getTotalCost()),
                        formatBigDecimal(cost.getBaseSalaryCost()),
                        formatBigDecimal(cost.getOvertimeCost()),
                        formatBigDecimal(cost.getAllowanceCost()),
                        cost.getPercentageOfTotal());
            }

            // Theo loại hợp đồng
            printer.printRecord(ReportLabels.get(ReportLabels.BY_CONTRACT_TYPE_LABEL, language),
                    "", "", "", "", "", "");
            for (CostByContractType cost : report.getCostByContractType()) {
                printer.printRecord(
                        cost.getContractType() != null ? cost.getContractType().name() : "",
                        cost.getEmployeeCount(),
                        formatBigDecimal(cost.getTotalCost()),
                        formatBigDecimal(cost.getBaseSalaryCost()),
                        formatBigDecimal(cost.getOvertimeCost()),
                        formatBigDecimal(cost.getAllowanceCost()),
                        cost.getPercentageOfTotal());
            }
        }
        return out.toByteArray();
    }

    public byte[] exportCostAnalysisToPdf(CostAnalysisReport report, String language) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, ReportLabels.get(ReportLabels.COST_TITLE, language));
        addPeriodInfo(document, report.getStartDate().format(DATE_FORMATTER),
                report.getEndDate().format(DATE_FORMATTER), language);
        addSummaryLine(document, String.format("%s: %s",
                ReportLabels.get(ReportLabels.TOTAL_LABOR_COST_LABEL, language),
                formatBigDecimal(report.getTotalLaborCost())));
        document.add(new Paragraph(" "));

        // Bảng theo loại lương
        document.add(new Paragraph(ReportLabels.get(ReportLabels.BY_SALARY_TYPE_LABEL, language),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        document.add(new Paragraph(" "));

        String[] headers = ReportLabels.getHeaders(ReportLabels.COST_HEADERS, language);
        PdfPTable salaryTable = createTable(headers);
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        for (CostBySalaryType cost : report.getCostBySalaryType()) {
            addCell(salaryTable, cost.getSalaryType() != null ? cost.getSalaryType().name() : "", dataFont);
            addCell(salaryTable, String.valueOf(cost.getEmployeeCount()), dataFont);
            addCell(salaryTable, formatBigDecimal(cost.getTotalCost()), dataFont);
            addCell(salaryTable, formatBigDecimal(cost.getBaseSalaryCost()), dataFont);
            addCell(salaryTable, formatBigDecimal(cost.getOvertimeCost()), dataFont);
            addCell(salaryTable, formatBigDecimal(cost.getAllowanceCost()), dataFont);
            addCell(salaryTable, String.format("%.2f", cost.getPercentageOfTotal()), dataFont);
        }
        document.add(salaryTable);
        document.add(new Paragraph(" "));

        // Bảng theo loại hợp đồng
        document.add(new Paragraph(ReportLabels.get(ReportLabels.BY_CONTRACT_TYPE_LABEL, language),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        document.add(new Paragraph(" "));

        PdfPTable contractTable = createTable(headers);
        for (CostByContractType cost : report.getCostByContractType()) {
            addCell(contractTable, cost.getContractType() != null ? cost.getContractType().name() : "", dataFont);
            addCell(contractTable, String.valueOf(cost.getEmployeeCount()), dataFont);
            addCell(contractTable, formatBigDecimal(cost.getTotalCost()), dataFont);
            addCell(contractTable, formatBigDecimal(cost.getBaseSalaryCost()), dataFont);
            addCell(contractTable, formatBigDecimal(cost.getOvertimeCost()), dataFont);
            addCell(contractTable, formatBigDecimal(cost.getAllowanceCost()), dataFont);
            addCell(contractTable, String.format("%.2f", cost.getPercentageOfTotal()), dataFont);
        }
        document.add(contractTable);

        document.close();
        return out.toByteArray();
    }

    // ==================== Helper Methods ====================

    private void addTitle(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);
        document.add(new Paragraph(" "));
    }

    private void addPeriodInfo(Document document, String startDate, String endDate, String language)
            throws DocumentException {
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String periodLabel = ReportLabels.get(ReportLabels.PERIOD_LABEL, language);
        Paragraph periodPara = new Paragraph(String.format("%s: %s - %s", periodLabel, startDate, endDate), infoFont);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        document.add(periodPara);
    }

    private void addSummaryLine(Document document, String text) throws DocumentException {
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph summaryPara = new Paragraph(text, summaryFont);
        summaryPara.setAlignment(Element.ALIGN_CENTER);
        document.add(summaryPara);
    }

    private PdfPTable createTable(String[] headers) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }
}
