package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.result.*;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculator tính toán lương tổng hợp
 * Tích hợp: OvertimeCalculator, AllowanceCalculator, DeductionCalculator
 * Tính: base salary, gross salary, net salary
 */
@Component
@RequiredArgsConstructor
public class PayrollCalculator implements IPayrollCalculator {

    private final IOvertimeCalculator overtimeCalculator;
    private final IAllowanceCalculator allowanceCalculator;
    private final IDeductionCalculator deductionCalculator;

    private static final int MINUTES_PER_HOUR = 60;

    @Override
    public PayrollResult calculatePayroll(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            List<DailyOvertimeDetail> dailyDetails,
            PayrollConfig payrollConfig,
            OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig,
            DeductionConfig deductionConfig) {

        // Gọi overload method với breakConfig = null
        return calculatePayroll(salaryInfo, attendance, dailyDetails,
                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig, null);
    }

    @Override
    public PayrollResult calculatePayroll(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            List<DailyOvertimeDetail> dailyDetails,
            PayrollConfig payrollConfig,
            OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig,
            DeductionConfig deductionConfig,
            BreakConfig breakConfig) {

        if (salaryInfo == null) {
            return PayrollResult.builder().build();
        }

        // 1. Tính lương cơ bản
        BigDecimal baseSalary = calculateBaseSalary(salaryInfo, attendance, payrollConfig);

        // 2. Tính lương giờ để tính overtime
        BigDecimal hourlyRate = calculateHourlyRate(salaryInfo, payrollConfig);

        // 3. Tính tăng ca
        OvertimeResult overtimeResult = overtimeCalculator.calculateOvertime(
                dailyDetails, overtimeConfig, hourlyRate);
        BigDecimal totalOvertimePay = overtimeResult.getTotalOvertimePay();

        // 4. Tính phụ cấp
        AllowanceResult allowanceResult = allowanceCalculator.calculateAllowances(
                allowanceConfig, attendance);
        BigDecimal totalAllowances = allowanceResult.getTotalAllowances();

        // 5. Tính break deduction nếu có
        BigDecimal breakDeductionAmount = calculateBreakDeduction(
                attendance != null ? attendance.getTotalBreakMinutes() : null,
                hourlyRate, breakConfig);

        // 6. Tính lương gộp = base + overtime + allowances
        BigDecimal grossSalary = baseSalary
                .add(totalOvertimePay)
                .add(totalAllowances);

        // 7. Tính khấu trừ (dựa trên gross salary)
        DeductionResult deductionResult = deductionCalculator.calculateDeductions(
                deductionConfig, attendance, grossSalary);
        BigDecimal totalDeductions = deductionResult.getTotalDeductions()
                .add(breakDeductionAmount);

        // 8. Tính lương thực nhận = gross - deductions
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        // Làm tròn lương nếu cần
        netSalary = roundSalary(netSalary, payrollConfig);

        return PayrollResult.builder()
                .salaryType(salaryInfo.getSalaryType())
                .baseSalary(baseSalary)
                .totalOvertimePay(totalOvertimePay)
                .overtimeResult(overtimeResult)
                .totalAllowances(totalAllowances)
                .allowanceResult(allowanceResult)
                .totalDeductions(totalDeductions)
                .deductionResult(deductionResult)
                .grossSalary(grossSalary)
                .netSalary(netSalary)
                .totalBreakMinutes(attendance != null ? attendance.getTotalBreakMinutes() : null)
                .breakType(breakConfig != null ? breakConfig.getBreakType() : null)
                .breakDeductionAmount(breakDeductionAmount)
                .build();
    }

    /**
     * Tính break deduction amount
     * UNPAID: deduction = (break minutes / 60) × hourly rate
     * PAID: deduction = 0
     */
    private BigDecimal calculateBreakDeduction(
            Integer totalBreakMinutes,
            BigDecimal hourlyRate,
            BreakConfig breakConfig) {

        // Chỉ tính deduction nếu break là UNPAID
        if (breakConfig == null || breakConfig.getBreakType() != BreakType.UNPAID) {
            return BigDecimal.ZERO;
        }

        if (totalBreakMinutes == null || totalBreakMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Break deduction = (break minutes / 60) * hourly rate
        BigDecimal breakHours = BigDecimal.valueOf(totalBreakMinutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 4, RoundingMode.HALF_UP);

        return breakHours.multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính lương cơ bản theo loại lương
     */
    private BigDecimal calculateBaseSalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            PayrollConfig config) {

        SalaryType salaryType = salaryInfo.getSalaryType();
        if (salaryType == null) {
            salaryType = config != null && config.getDefaultSalaryType() != null
                    ? config.getDefaultSalaryType()
                    : SalaryType.MONTHLY;
        }

        return switch (salaryType) {
            case MONTHLY -> calculateMonthlySalary(salaryInfo);
            case DAILY -> calculateDailySalary(salaryInfo, attendance);
            case HOURLY -> calculateHourlySalary(salaryInfo, attendance);
        };
    }

    /**
     * Lương tháng = monthlySalary (cố định)
     */
    private BigDecimal calculateMonthlySalary(EmployeeSalaryInfo salaryInfo) {
        return salaryInfo.getMonthlySalary() != null
                ? salaryInfo.getMonthlySalary()
                : BigDecimal.ZERO;
    }

    /**
     * Lương ngày = dailyRate × workingDays
     */
    private BigDecimal calculateDailySalary(EmployeeSalaryInfo salaryInfo, AttendanceSummary attendance) {
        if (salaryInfo.getDailyRate() == null || attendance == null || attendance.getWorkingDays() == null) {
            return BigDecimal.ZERO;
        }

        return salaryInfo.getDailyRate()
                .multiply(BigDecimal.valueOf(attendance.getWorkingDays()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Lương giờ = hourlyRate × workingHours
     */
    private BigDecimal calculateHourlySalary(EmployeeSalaryInfo salaryInfo, AttendanceSummary attendance) {
        if (salaryInfo.getHourlyRate() == null || attendance == null || attendance.getWorkingHours() == null) {
            return BigDecimal.ZERO;
        }

        return salaryInfo.getHourlyRate()
                .multiply(BigDecimal.valueOf(attendance.getWorkingHours()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính lương giờ từ thông tin lương
     */
    private BigDecimal calculateHourlyRate(EmployeeSalaryInfo salaryInfo, PayrollConfig config) {
        if (salaryInfo.getHourlyRate() != null) {
            return salaryInfo.getHourlyRate();
        }

        if (salaryInfo.getDailyRate() != null && config != null) {
            int hoursPerDay = config.getStandardWorkingHoursPerDay() != null
                    ? config.getStandardWorkingHoursPerDay()
                    : 8;
            return salaryInfo.getDailyRate()
                    .divide(BigDecimal.valueOf(hoursPerDay), 4, RoundingMode.HALF_UP);
        }

        if (salaryInfo.getMonthlySalary() != null && config != null) {
            int daysPerMonth = config.getStandardWorkingDaysPerMonth() != null
                    ? config.getStandardWorkingDaysPerMonth()
                    : 22;
            int hoursPerDay = config.getStandardWorkingHoursPerDay() != null
                    ? config.getStandardWorkingHoursPerDay()
                    : 8;
            int hoursPerMonth = daysPerMonth * hoursPerDay;

            return salaryInfo.getMonthlySalary()
                    .divide(BigDecimal.valueOf(hoursPerMonth), 4, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Làm tròn lương theo cấu hình
     */
    private BigDecimal roundSalary(BigDecimal salary, PayrollConfig config) {
        if (salary == null || config == null || config.getSalaryRounding() == null) {
            return salary != null ? salary.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }

        return switch (config.getSalaryRounding()) {
            case UP -> salary.setScale(0, RoundingMode.CEILING);
            case DOWN -> salary.setScale(0, RoundingMode.FLOOR);
            case NEAREST -> salary.setScale(0, RoundingMode.HALF_UP);
        };
    }
}
