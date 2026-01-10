package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.response.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mapper cho PayrollPeriod và PayrollItem entities và DTOs.
 */
@Component
@RequiredArgsConstructor
public class PayrollPeriodMapper {

    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi từ Request DTO sang Entity
     */
    public PayrollPeriodEntity toEntity(PayrollPeriodRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        PayrollPeriodEntity entity = new PayrollPeriodEntity();
        entity.setYear(request.getYear());
        entity.setMonth(request.getMonth());
        entity.setStatus(PayrollPeriodStatus.DRAFT);
        entity.setCreatedBy(createdBy);

        // Tính ngày bắt đầu và kết thúc kỳ lương
        if (request.getPeriodStart() != null) {
            entity.setPeriodStart(request.getPeriodStart());
        } else {
            entity.setPeriodStart(LocalDate.of(request.getYear(), request.getMonth(), 1));
        }

        if (request.getPeriodEnd() != null) {
            entity.setPeriodEnd(request.getPeriodEnd());
        } else {
            YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
            entity.setPeriodEnd(yearMonth.atEndOfMonth());
        }

        // Khởi tạo giá trị mặc định
        entity.setTotalGrossSalary(BigDecimal.ZERO);
        entity.setTotalNetSalary(BigDecimal.ZERO);
        entity.setTotalEmployees(0);

        return entity;
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO
     */
    public PayrollPeriodResponse toResponse(PayrollPeriodEntity entity, Map<Long, UserEntity> userMap) {
        if (entity == null) {
            return null;
        }

        String createdByName = getUserName(entity.getCreatedBy(), userMap);
        String approvedByName = getUserName(entity.getApprovedBy(), userMap);

        return PayrollPeriodResponse.builder()
                .id(entity.getId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .year(entity.getYear())
                .month(entity.getMonth())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .approvedBy(entity.getApprovedBy())
                .approvedByName(approvedByName)
                .approvedAt(entity.getApprovedAt())
                .paidAt(entity.getPaidAt())
                .paymentReference(entity.getPaymentReference())
                .totalGrossSalary(entity.getTotalGrossSalary())
                .totalNetSalary(entity.getTotalNetSalary())
                .totalEmployees(entity.getTotalEmployees())
                .build();
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO (không có user map)
     */
    public PayrollPeriodResponse toResponse(PayrollPeriodEntity entity) {
        return toResponse(entity, Collections.emptyMap());
    }

    /**
     * Chuyển đổi từ Entity sang Detail Response DTO
     */
    public PayrollPeriodDetailResponse toDetailResponse(
            PayrollPeriodEntity entity,
            List<PayrollItemResponse> items,
            Map<Long, UserEntity> userMap) {
        if (entity == null) {
            return null;
        }

        String createdByName = getUserName(entity.getCreatedBy(), userMap);
        String approvedByName = getUserName(entity.getApprovedBy(), userMap);

        // Tính toán thống kê từ items
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        int adjustedItemsCount = 0;

        if (items != null) {
            for (PayrollItemResponse item : items) {
                if (item.getCalculatedBaseSalary() != null) {
                    totalBaseSalary = totalBaseSalary.add(item.getCalculatedBaseSalary());
                }
                if (item.getTotalOvertimePay() != null) {
                    totalOvertimePay = totalOvertimePay.add(item.getTotalOvertimePay());
                }
                if (item.getTotalAllowances() != null) {
                    totalAllowances = totalAllowances.add(item.getTotalAllowances());
                }
                if (item.getTotalDeductions() != null) {
                    totalDeductions = totalDeductions.add(item.getTotalDeductions());
                }
                if (item.getAdjustmentAmount() != null && item.getAdjustmentAmount().compareTo(BigDecimal.ZERO) != 0) {
                    adjustedItemsCount++;
                }
            }
        }

        return PayrollPeriodDetailResponse.builder()
                .id(entity.getId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .year(entity.getYear())
                .month(entity.getMonth())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .approvedBy(entity.getApprovedBy())
                .approvedByName(approvedByName)
                .approvedAt(entity.getApprovedAt())
                .paidAt(entity.getPaidAt())
                .paymentReference(entity.getPaymentReference())
                .totalGrossSalary(entity.getTotalGrossSalary())
                .totalNetSalary(entity.getTotalNetSalary())
                .totalEmployees(entity.getTotalEmployees())
                .items(items)
                .totalBaseSalary(totalBaseSalary)
                .totalOvertimePay(totalOvertimePay)
                .totalAllowances(totalAllowances)
                .totalDeductions(totalDeductions)
                .adjustedItemsCount(adjustedItemsCount)
                .build();
    }

    /**
     * Chuyển đổi PayrollItemEntity sang Response DTO
     */
    public PayrollItemResponse toItemResponse(PayrollItemEntity entity, Map<Long, UserEntity> userMap) {
        if (entity == null) {
            return null;
        }

        UserEntity employee = userMap.get(entity.getEmployeeId());
        String employeeName = null;
        String employeeCode = null;
        if (employee != null) {
            employeeCode = employee.getEmployeeCode();
            if (employee.getProfile() != null) {
                employeeName = employee.getProfile().getName();
            }
        }

        String adjustedByName = getUserName(entity.getAdjustedBy(), userMap);

        // Parse allowance details từ JSON
        List<PayrollItemResponse.AllowanceDetailResponse> allowanceDetails = parseAllowanceDetails(
                entity.getAllowanceDetails());

        // Parse deduction details từ JSON
        List<PayrollItemResponse.DeductionDetailResponse> deductionDetails = parseDeductionDetails(
                entity.getDeductionDetails());

        return PayrollItemResponse.builder()
                .id(entity.getId())
                .payrollPeriodId(entity.getPayrollPeriodId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .salaryType(entity.getSalaryType())
                .baseSalary(entity.getBaseSalary())
                .calculatedBaseSalary(entity.getCalculatedBaseSalary())
                .workingDays(entity.getWorkingDays())
                .workingHours(entity.getWorkingHours())
                .workingMinutes(entity.getWorkingMinutes())
                .regularOvertimeMinutes(entity.getRegularOvertimeMinutes())
                .nightOvertimeMinutes(entity.getNightOvertimeMinutes())
                .holidayOvertimeMinutes(entity.getHolidayOvertimeMinutes())
                .weekendOvertimeMinutes(entity.getWeekendOvertimeMinutes())
                .totalOvertimePay(entity.getTotalOvertimePay())
                .totalBreakMinutes(entity.getTotalBreakMinutes())
                .breakType(entity.getBreakType())
                .breakDeductionAmount(entity.getBreakDeductionAmount())
                .allowanceDetails(allowanceDetails)
                .totalAllowances(entity.getTotalAllowances())
                .deductionDetails(deductionDetails)
                .totalDeductions(entity.getTotalDeductions())
                .grossSalary(entity.getGrossSalary())
                .netSalary(entity.getNetSalary())
                .adjustmentAmount(entity.getAdjustmentAmount())
                .adjustmentReason(entity.getAdjustmentReason())
                .adjustedBy(entity.getAdjustedBy())
                .adjustedByName(adjustedByName)
                .adjustedAt(entity.getAdjustedAt())
                .status(entity.getStatus())
                .build();
    }

    /**
     * Lấy tên user từ map
     */
    private String getUserName(Long userId, Map<Long, UserEntity> userMap) {
        if (userId == null || userMap == null) {
            return null;
        }
        UserEntity user = userMap.get(userId);
        if (user != null && user.getProfile() != null) {
            return user.getProfile().getName();
        }
        return null;
    }

    /**
     * Parse allowance details từ JSON string
     */
    private List<PayrollItemResponse.AllowanceDetailResponse> parseAllowanceDetails(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Parse deduction details từ JSON string
     */
    private List<PayrollItemResponse.DeductionDetailResponse> parseDeductionDetails(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
