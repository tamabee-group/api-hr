package com.tamabee.api_hr.mapper.company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.request.payroll.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;

import lombok.RequiredArgsConstructor;

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
        entity.setStatus(PayrollPeriodStatus.DRAFT);
        entity.setCreatedBy(createdBy);

        // Xác định year và month từ request hoặc từ periodStart
        Integer year = request.getYear();
        Integer month = request.getMonth();
        
        // Nếu không có year/month, extract từ periodStart
        if ((year == null || month == null) && request.getPeriodStart() != null) {
            year = request.getPeriodStart().getYear();
            month = request.getPeriodStart().getMonthValue();
        }
        
        entity.setYear(year);
        entity.setMonth(month);

        // Tính ngày bắt đầu và kết thúc kỳ lương
        if (request.getPeriodStart() != null) {
            entity.setPeriodStart(request.getPeriodStart());
        } else if (year != null && month != null) {
            entity.setPeriodStart(LocalDate.of(year, month, 1));
        }

        if (request.getPeriodEnd() != null) {
            entity.setPeriodEnd(request.getPeriodEnd());
        } else if (year != null && month != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
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
                .rejectionReason(entity.getRejectionReason())
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
                .rejectionReason(entity.getRejectionReason())
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
     * Chuyển đổi PayrollItemEntity sang Response DTO với year/month/paidAt từ period
     */
    public PayrollItemResponse toItemResponse(
            PayrollItemEntity entity, Map<Long, UserEntity> userMap,
            Integer year, Integer month, LocalDateTime paidAt) {
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
                .year(year)
                .month(month)
                .paidAt(paidAt)
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
     * Chuyển đổi PayrollItemEntity sang Response DTO với year/month từ period (backward compatibility)
     */
    public PayrollItemResponse toItemResponse(PayrollItemEntity entity, Map<Long, UserEntity> userMap, Integer year, Integer month) {
        return toItemResponse(entity, userMap, year, month, null);
    }

    /**
     * Chuyển đổi PayrollItemEntity sang Response DTO (không có year/month)
     */
    public PayrollItemResponse toItemResponse(PayrollItemEntity entity, Map<Long, UserEntity> userMap) {
        return toItemResponse(entity, userMap, null, null);
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
