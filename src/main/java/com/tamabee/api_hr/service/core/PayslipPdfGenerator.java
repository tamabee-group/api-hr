package com.tamabee.api_hr.service.core;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.tamabee.api_hr.dto.response.PayrollRecordResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;

/**
 * Service tạo PDF phiếu lương
 * Hỗ trợ tiếng Nhật và tiếng Việt dựa trên locale của công ty
 */
@Slf4j
@Service
public class PayslipPdfGenerator {

    private static final Color HEADER_BG = new Color(128, 128, 128);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    public byte[] generate(PayrollRecordResponse record, UserEntity employee, CompanyEntity company) {
        log.info("Tạo PDF payslip cho nhân viên: {}", employee.getEmployeeCode());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String locale = "ja"; // default
        if (company != null) {
            String companyLocale = company.getLocale();
            if (companyLocale != null) {
                if (companyLocale.contains("Ho_Chi_Minh") || companyLocale.contains("Vietnam")) {
                    locale = "vi";
                }
            }
        }

        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            PdfContentByte cb = writer.getDirectContent();

            // Load font
            String fontPath = getClass().getClassLoader().getResource("fonts/NotoSansCJKjp-Regular.otf").getPath();
            BaseFont bfCJK = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfLatin = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont bfLatinBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            Labels labels = getLabels(locale);

            // === HEADER SECTION ===
            float y = 780;
            float leftMargin = 40;
            float rightBoxX = 380;
            float rightBoxW = 175;
            float rightBoxH = 65;

            // Right side: Company info box with rounded corners (căn ngang với title)
            float boxY = y + 8;
            drawRoundedRect(cb, rightBoxX, boxY - rightBoxH, rightBoxW, rightBoxH, 3);

            // Company name
            String companyName = company != null ? company.getName() : "N/A";
            cb.beginText();
            cb.setFontAndSize(bfCJK, 10);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(rightBoxX + 15, boxY - 18);
            cb.showText(companyName);
            cb.endText();

            // Employee code
            cb.beginText();
            cb.setFontAndSize(bfCJK, 8);
            cb.setColorFill(Color.GRAY);
            cb.setTextMatrix(rightBoxX + 15, boxY - 35);
            cb.showText(labels.employeeId + ": " + employee.getEmployeeCode());
            cb.endText();

            // Employee name
            String empName = employee.getProfile() != null && employee.getProfile().getName() != null
                    ? employee.getProfile().getName()
                    : "N/A";
            cb.beginText();
            cb.setFontAndSize(bfCJK, 10);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(rightBoxX + 15, boxY - 52);
            cb.showText(empName);
            cb.endText();

            // Title: 2025(令和07)年12月15日支給分 給与明細
            cb.beginText();
            cb.setFontAndSize(bfCJK, 14);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(leftMargin, y);
            cb.showText(formatTitle(record, locale));
            cb.endText();

            // Period: 対象期間: 11月01日 ~ 11月30日
            y -= 22;
            cb.beginText();
            cb.setFontAndSize(bfCJK, 9);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(leftMargin, y);
            cb.showText(formatPeriod(record, locale, labels));
            cb.endText();

            // Net Salary row
            y -= 30;
            cb.beginText();
            cb.setFontAndSize(bfCJK, 10);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(leftMargin, y);
            cb.showText(labels.netSalary + ":");
            cb.endText();

            // Net salary value (số dùng Latin font)
            String netSalaryValue = formatCurrency(record.getNetSalary());
            cb.beginText();
            cb.setFontAndSize(bfLatin, 14);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(leftMargin + 100, y);
            cb.showText(netSalaryValue);
            cb.endText();

            // Currency unit (đơn vị tiền dùng CJK font)
            float valueWidth = bfLatin.getWidthPoint(netSalaryValue, 14);
            cb.beginText();
            cb.setFontAndSize(bfCJK, 14);
            cb.setColorFill(Color.BLACK);
            cb.setTextMatrix(leftMargin + 100 + valueWidth + 5, y);
            cb.showText(labels.currency);
            cb.endText();

            // === MAIN CONTENT - 4 COLUMNS ===
            y -= 70;
            float tableY = y;
            float colWidth = 130;
            float gap = 5;
            float tableHeight = 320;
            float headerHeight = 22;
            float rowHeight = 16;

            String[] headers = { labels.attendance, labels.earnings, labels.deductions, labels.others };
            for (int i = 0; i < 4; i++) {
                float x = leftMargin + i * colWidth;

                // Header (không bo góc)
                cb.setColorFill(HEADER_BG);
                cb.rectangle(x, tableY - headerHeight, colWidth - gap, headerHeight);
                cb.fill();

                // Header text (centered)
                cb.beginText();
                cb.setFontAndSize(bfCJK, 9);
                cb.setColorFill(Color.WHITE);
                float textWidth = bfCJK.getWidthPoint(headers[i], 9);
                cb.setTextMatrix(x + (colWidth - gap - textWidth) / 2, tableY - 15);
                cb.showText(headers[i]);
                cb.endText();

                // Column body border
                cb.setColorStroke(BORDER_COLOR);
                cb.rectangle(x, tableY - tableHeight, colWidth - gap, tableHeight - headerHeight);
                cb.stroke();
            }

            // Column 1: Attendance
            float col1X = leftMargin;
            float contentY = tableY - headerHeight - 18;
            drawRow(cb, bfCJK, bfLatin, col1X, contentY, labels.workingDays, str(record.getWorkingDays()),
                    colWidth - gap);
            contentY -= rowHeight;
            drawRow(cb, bfCJK, bfLatin, col1X, contentY, labels.overtimeHours, formatOvertimeHours(record),
                    colWidth - gap);

            // Column 2: Earnings
            float col2X = leftMargin + colWidth;
            contentY = tableY - headerHeight - 18;
            drawRow(cb, bfCJK, bfLatin, col2X, contentY, labels.baseSalary, formatCurrency(record.getBaseSalary()),
                    colWidth - gap);
            contentY -= rowHeight;

            if (record.getAllowanceDetails() != null) {
                for (PayrollRecordResponse.AllowanceItemResponse a : record.getAllowanceDetails()) {
                    String name = a.getName() != null ? a.getName() : labels.allowance;
                    drawRow(cb, bfCJK, bfLatin, col2X, contentY, name, formatCurrency(a.getAmount()), colWidth - gap);
                    contentY -= rowHeight;
                }
            }

            if (record.getTotalOvertimePay() != null && record.getTotalOvertimePay().compareTo(BigDecimal.ZERO) > 0) {
                drawRow(cb, bfCJK, bfLatin, col2X, contentY, labels.overtimePay,
                        formatCurrency(record.getTotalOvertimePay()), colWidth - gap);
            }

            // Earnings total
            float totalY = tableY - tableHeight + 18;
            drawTotalRow(cb, bfCJK, bfLatinBold, col2X, totalY, labels.total, formatCurrency(record.getGrossSalary()),
                    colWidth - gap);

            // Column 3: Deductions
            float col3X = leftMargin + colWidth * 2;
            contentY = tableY - headerHeight - 18;

            if (record.getDeductionDetails() != null && !record.getDeductionDetails().isEmpty()) {
                for (PayrollRecordResponse.DeductionItemResponse d : record.getDeductionDetails()) {
                    String name = d.getName() != null ? d.getName() : labels.deduction;
                    drawRow(cb, bfCJK, bfLatin, col3X, contentY, name, formatCurrency(d.getAmount()), colWidth - gap);
                    contentY -= rowHeight;
                }
            }

            // Deductions total
            drawTotalRow(cb, bfCJK, bfLatinBold, col3X, totalY, labels.total,
                    formatCurrency(record.getTotalDeductions()), colWidth - gap);

            // Column 4: Others
            float col4X = leftMargin + colWidth * 3;
            contentY = tableY - headerHeight - 18;
            drawRow(cb, bfCJK, bfLatin, col4X, contentY, labels.bankTransfer, formatCurrency(record.getNetSalary()),
                    colWidth - gap);

            // === REMARKS SECTION ===
            float remarksY = tableY - tableHeight - 25;

            // Remarks header (không bo góc)
            cb.setColorFill(HEADER_BG);
            cb.rectangle(leftMargin, remarksY - 18, 60, 18);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(bfCJK, 8);
            cb.setColorFill(Color.WHITE);
            cb.setTextMatrix(leftMargin + 10, remarksY - 13);
            cb.showText(labels.remarks);
            cb.endText();

            // Remarks box (không bo góc)
            cb.setColorStroke(BORDER_COLOR);
            cb.rectangle(leftMargin, remarksY - 70, 515, 52);
            cb.stroke();

            document.close();

        } catch (Exception e) {
            log.error("Lỗi tạo PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo PDF: " + e.getMessage(), e);
        }

        log.info("PDF tạo thành công, size: {} bytes", baos.size());
        return baos.toByteArray();
    }

    /**
     * Vẽ hình chữ nhật bo góc (chỉ stroke)
     */
    private void drawRoundedRect(PdfContentByte cb, float x, float y, float w, float h, float r) {
        cb.setColorStroke(BORDER_COLOR);
        cb.roundRectangle(x, y, w, h, r);
        cb.stroke();
    }

    private void drawRow(PdfContentByte cb, BaseFont bfCJK, BaseFont bfLatin,
            float x, float y, String label, String value, float width) {
        cb.beginText();
        cb.setFontAndSize(bfCJK, 8);
        cb.setColorFill(Color.BLACK);
        cb.setTextMatrix(x + 8, y);
        cb.showText(label);
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(bfLatin, 9);
        float valueWidth = bfLatin.getWidthPoint(value, 9);
        cb.setTextMatrix(x + width - valueWidth - 8, y);
        cb.showText(value);
        cb.endText();
    }

    private void drawTotalRow(PdfContentByte cb, BaseFont bfCJK, BaseFont bfLatinBold,
            float x, float y, String label, String value, float width) {
        cb.setColorStroke(BORDER_COLOR);
        cb.moveTo(x + 5, y + 12);
        cb.lineTo(x + width - 5, y + 12);
        cb.stroke();

        cb.beginText();
        cb.setFontAndSize(bfCJK, 8);
        cb.setColorFill(Color.BLACK);
        cb.setTextMatrix(x + 8, y);
        cb.showText(label);
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(bfLatinBold, 10);
        float valueWidth = bfLatinBold.getWidthPoint(value, 10);
        cb.setTextMatrix(x + width - valueWidth - 8, y);
        cb.showText(value);
        cb.endText();
    }

    private String formatTitle(PayrollRecordResponse record, String locale) {
        int year = record.getYear();
        int month = record.getMonth();
        int payMonth = month + 1;
        int payYear = year;
        if (payMonth > 12) {
            payMonth = 1;
            payYear++;
        }

        if ("ja".equals(locale)) {
            LocalDate payDate = LocalDate.of(payYear, payMonth, 15);
            JapaneseDate jpDate = JapaneseDate.from(payDate);
            String eraYear = String.format("%02d", jpDate.get(java.time.temporal.ChronoField.YEAR_OF_ERA));
            return String.format("%d(令和%s)年%d月15日支給分 給与明細", payYear, eraYear, payMonth);
        } else {
            return String.format("Phiếu lương tháng %02d/%d", payMonth, payYear);
        }
    }

    private String formatPeriod(PayrollRecordResponse record, String locale, Labels labels) {
        int year = record.getYear();
        int month = record.getMonth();
        LocalDate end = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());

        if ("ja".equals(locale)) {
            return String.format("%s:   %d月%02d日   ~   %d月%02d日", labels.period, month, 1, month, end.getDayOfMonth());
        } else {
            return String.format("%s: %02d/%02d ~ %02d/%02d/%d", labels.period, 1, month, end.getDayOfMonth(), month,
                    year);
        }
    }

    private String formatOvertimeHours(PayrollRecordResponse record) {
        int total = 0;
        if (record.getRegularOvertimeHours() != null)
            total += record.getRegularOvertimeHours();
        if (record.getNightOvertimeHours() != null)
            total += record.getNightOvertimeHours();
        if (record.getHolidayOvertimeHours() != null)
            total += record.getHolidayOvertimeHours();
        return String.format("%d:%02d", total / 60, total % 60);
    }

    private String str(Integer val) {
        return val != null ? val.toString() : "0";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0";
        return String.format("%,d", amount.longValue());
    }

    private Labels getLabels(String locale) {
        if ("ja".equals(locale)) {
            return new Labels("対象期間", "差引支給額", "円", "社員番号",
                    "勤怠", "支給", "控除", "その他",
                    "出勤日数", "残業時間", "基本給", "手当", "残業手当",
                    "合計", "控除", "銀行振込額", "備考");
        } else {
            return new Labels("Kỳ lương", "Lương thực nhận", "VNĐ", "Mã NV",
                    "Chấm công", "Thu nhập", "Khấu trừ", "Khác",
                    "Ngày công", "Giờ tăng ca", "Lương cơ bản", "Phụ cấp", "Lương tăng ca",
                    "Tổng", "Khấu trừ", "Chuyển khoản", "Ghi chú");
        }
    }

    private static class Labels {
        final String period, netSalary, currency, employeeId;
        final String attendance, earnings, deductions, others;
        final String workingDays, overtimeHours, baseSalary, allowance, overtimePay;
        final String total, deduction, bankTransfer, remarks;

        Labels(String period, String netSalary, String currency, String employeeId,
                String attendance, String earnings, String deductions, String others,
                String workingDays, String overtimeHours, String baseSalary, String allowance, String overtimePay,
                String total, String deduction, String bankTransfer, String remarks) {
            this.period = period;
            this.netSalary = netSalary;
            this.currency = currency;
            this.employeeId = employeeId;
            this.attendance = attendance;
            this.earnings = earnings;
            this.deductions = deductions;
            this.others = others;
            this.workingDays = workingDays;
            this.overtimeHours = overtimeHours;
            this.baseSalary = baseSalary;
            this.allowance = allowance;
            this.overtimePay = overtimePay;
            this.total = total;
            this.deduction = deduction;
            this.bankTransfer = bankTransfer;
            this.remarks = remarks;
        }
    }
}
