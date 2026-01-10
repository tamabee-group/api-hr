package com.tamabee.api_hr.mapper.company;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.response.payroll.PayrollPreviewResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse;
import com.tamabee.api_hr.dto.result.AllowanceItem;
import com.tamabee.api_hr.dto.result.DeductionItem;
import com.tamabee.api_hr.dto.result.PayrollResult;
import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.enums.PaymentStatus;
import com.tamabee.api_hr.enums.PayrollStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper chuyển đổi giữa PayrollRecordEntity và PayrollRecordResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollMapper {

    private final ObjectMapper objectMapper;

    /**
     * Tạo entity từ kết quả tính toán lương
     */
    public PayrollRecordEntity toEntity(
            Long employeeId,
            Long companyId,
            YearMonth period,
            PayrollResult result) {

        if (result == null) {
            return null;
        }

        PayrollRecordEntity entity = new PayrollRecordEntity();
        entity.setEmployeeId(employeeId);
        entity.setYear(period.getYear());
        entity.setMonth(period.getMonthValue());
        entity.setSalaryType(result.getSalaryType());
        entity.setBaseSalary(result.getBaseSalary());
        entity.setTotalOvertimePay(result.getTotalOvertimePay());
        entity.setTotalAllowances(result.getTotalAllowances());
        entity.setTotalDeductions(result.getTotalDeductions());
        entity.setGrossSalary(result.getGrossSalary());
        entity.setNetSalary(result.getNetSalary());
        entity.setStatus(PayrollStatus.DRAFT);
        entity.setPaymentStatus(PaymentStatus.PENDING);
        entity.setNotificationSent(false);

        // Set overtime details
        if (result.getOvertimeResult() != null) {
            entity.setRegularOvertimePay(result.getOvertimeResult().getRegularOvertimePay());
            entity.setNightOvertimePay(result.getOvertimeResult().getNightOvertimePay());
            entity.setHolidayOvertimePay(result.getOvertimeResult().getHolidayOvertimePay());
            entity.setWeekendOvertimePay(result.getOvertimeResult().getWeekendOvertimePay());
            entity.setRegularOvertimeHours(result.getOvertimeResult().getRegularOvertimeMinutes() != null
                    ? result.getOvertimeResult().getRegularOvertimeMinutes() / 60
                    : 0);
            entity.setNightOvertimeHours(result.getOvertimeResult().getNightOvertimeMinutes() != null
                    ? result.getOvertimeResult().getNightOvertimeMinutes() / 60
                    : 0);
            entity.setHolidayOvertimeHours(result.getOvertimeResult().getHolidayOvertimeMinutes() != null
                    ? result.getOvertimeResult().getHolidayOvertimeMinutes() / 60
                    : 0);
            entity.setWeekendOvertimeHours(result.getOvertimeResult().getWeekendOvertimeMinutes() != null
                    ? result.getOvertimeResult().getWeekendOvertimeMinutes() / 60
                    : 0);
        }

        // Set allowance details as JSON
        if (result.getAllowanceResult() != null && result.getAllowanceResult().getItems() != null) {
            entity.setAllowanceDetails(serializeAllowanceItems(result.getAllowanceResult().getItems()));
        }

        // Set deduction details as JSON
        if (result.getDeductionResult() != null && result.getDeductionResult().getItems() != null) {
            entity.setDeductionDetails(serializeDeductionItems(result.getDeductionResult().getItems()));
        }

        return entity;
    }

    /**
     * Tạo preview response từ danh sách records
     */
    public PayrollPreviewResponse toPreviewResponse(
            Long companyId,
            String companyName,
            YearMonth period,
            List<PayrollRecordResponse> records) {

        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalGrossSalary = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;

        for (PayrollRecordResponse record : records) {
            if (record.getBaseSalary() != null) {
                totalBaseSalary = totalBaseSalary.add(record.getBaseSalary());
            }
            if (record.getTotalOvertimePay() != null) {
                totalOvertimePay = totalOvertimePay.add(record.getTotalOvertimePay());
            }
            if (record.getTotalAllowances() != null) {
                totalAllowances = totalAllowances.add(record.getTotalAllowances());
            }
            if (record.getTotalDeductions() != null) {
                totalDeductions = totalDeductions.add(record.getTotalDeductions());
            }
            if (record.getGrossSalary() != null) {
                totalGrossSalary = totalGrossSalary.add(record.getGrossSalary());
            }
            if (record.getNetSalary() != null) {
                totalNetSalary = totalNetSalary.add(record.getNetSalary());
            }
        }

        return PayrollPreviewResponse.builder()
                .companyId(companyId)
                .companyName(companyName)
                .year(period.getYear())
                .month(period.getMonthValue())
                .period(formatPeriod(period.getYear(), period.getMonthValue()))
                .totalEmployees(records.size())
                .totalBaseSalary(totalBaseSalary)
                .totalOvertimePay(totalOvertimePay)
                .totalAllowances(totalAllowances)
                .totalDeductions(totalDeductions)
                .totalGrossSalary(totalGrossSalary)
                .totalNetSalary(totalNetSalary)
                .records(records)
                .build();
    }

    /**
     * Chuyển đổi entity sang response
     */
    public PayrollRecordResponse toResponse(PayrollRecordEntity entity, String employeeName, String employeeCode) {
        if (entity == null) {
            return null;
        }

        return PayrollRecordResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .year(entity.getYear())
                .month(entity.getMonth())
                .period(formatPeriod(entity.getYear(), entity.getMonth()))
                .salaryType(entity.getSalaryType())
                .baseSalary(entity.getBaseSalary())
                .workingDays(entity.getWorkingDays())
                .workingHours(entity.getWorkingHours())
                .regularOvertimePay(entity.getRegularOvertimePay())
                .nightOvertimePay(entity.getNightOvertimePay())
                .holidayOvertimePay(entity.getHolidayOvertimePay())
                .weekendOvertimePay(entity.getWeekendOvertimePay())
                .totalOvertimePay(entity.getTotalOvertimePay())
                .regularOvertimeHours(entity.getRegularOvertimeHours())
                .nightOvertimeHours(entity.getNightOvertimeHours())
                .holidayOvertimeHours(entity.getHolidayOvertimeHours())
                .weekendOvertimeHours(entity.getWeekendOvertimeHours())
                .allowanceDetails(parseAllowanceDetails(entity.getAllowanceDetails()))
                .totalAllowances(entity.getTotalAllowances())
                .deductionDetails(parseDeductionDetails(entity.getDeductionDetails()))
                .totalDeductions(entity.getTotalDeductions())
                .grossSalary(entity.getGrossSalary())
                .netSalary(entity.getNetSalary())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .paidAt(entity.getPaidAt())
                .paymentReference(entity.getPaymentReference())
                .notificationSent(entity.getNotificationSent())
                .notificationSentAt(entity.getNotificationSentAt())
                .finalizedAt(entity.getFinalizedAt())
                .finalizedBy(entity.getFinalizedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Format period thành chuỗi YYYY-MM
     */
    private String formatPeriod(Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        return String.format("%d-%02d", year, month);
    }

    /**
     * Parse JSON allowance details
     */
    private List<PayrollRecordResponse.AllowanceItemResponse> parseAllowanceDetails(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<AllowanceItem> items = objectMapper.readValue(json, new TypeReference<>() {
            });
            return items.stream()
                    .map(item -> PayrollRecordResponse.AllowanceItemResponse.builder()
                            .code(item.getCode())
                            .name(item.getName())
                            .amount(item.getAmount())
                            .taxable(item.getTaxable())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Không thể parse allowance details: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse JSON deduction details
     */
    private List<PayrollRecordResponse.DeductionItemResponse> parseDeductionDetails(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<DeductionItem> items = objectMapper.readValue(json, new TypeReference<>() {
            });
            return items.stream()
                    .map(item -> PayrollRecordResponse.DeductionItemResponse.builder()
                            .code(item.getCode())
                            .name(item.getName())
                            .amount(item.getAmount())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Không thể parse deduction details: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Serialize allowance items thành JSON
     */
    private String serializeAllowanceItems(List<AllowanceItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.warn("Không thể serialize allowance items: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Serialize deduction items thành JSON
     */
    private String serializeDeductionItems(List<DeductionItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.warn("Không thể serialize deduction items: {}", e.getMessage());
            return null;
        }
    }
}
