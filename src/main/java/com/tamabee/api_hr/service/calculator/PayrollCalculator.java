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
 * Calculator tính toán lương tổng hợp.
 * Tích hợp: OvertimeCalculator, AllowanceCalculator, DeductionCalculator.
 * Hỗ trợ các loại lương: MONTHLY, DAILY, HOURLY, SHIFT_BASED.
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

        // 2. Tính lương giờ để tính overtime và break deduction
        BigDecimal hourlyRate = calculateHourlyRate(salaryInfo, payrollConfig);

        // 3. Tính tăng ca
        OvertimeResult overtimeResult = calculateOvertime(dailyDetails, overtimeConfig, hourlyRate);
        BigDecimal totalOvertimePay = overtimeResult.getTotalOvertimePay();

        // 4. Tính phụ cấp
        AllowanceResult allowanceResult = calculateAllowances(allowanceConfig, attendance);
        BigDecimal totalAllowances = allowanceResult.getTotalAllowances();

        // 5. Tính break deduction
        BigDecimal breakDeductionAmount = calculateBreakDeduction(
                attendance != null ? attendance.getTotalBreakMinutes() : null,
                hourlyRate, breakConfig);

        // 6. Tính lương gộp
        BigDecimal grossSalary = calculateGrossSalary(baseSalary, totalOvertimePay, totalAllowances);

        // 7. Tính khấu trừ (bao gồm penalties)
        DeductionResult deductionResult = calculateDeductions(deductionConfig, attendance, grossSalary);
        BigDecimal totalDeductions = deductionResult.getTotalDeductions().add(breakDeductionAmount);

        // 8. Tính lương thực nhận
        BigDecimal netSalary = calculateNetSalary(grossSalary, totalDeductions, payrollConfig);

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

    @Override
    public BigDecimal calculateBaseSalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            PayrollConfig config) {

        if (salaryInfo == null) {
            return BigDecimal.ZERO;
        }

        SalaryType salaryType = salaryInfo.getSalaryType();
        if (salaryType == null) {
            salaryType = config != null && config.getDefaultSalaryType() != null
                    ? config.getDefaultSalaryType()
                    : SalaryType.MONTHLY;
        }

        return switch (salaryType) {
            case MONTHLY -> calculateMonthlySalary(salaryInfo, attendance, config);
            case DAILY -> calculateDailySalary(salaryInfo, attendance);
            case HOURLY -> calculateHourlySalary(salaryInfo, attendance);
            case SHIFT_BASED -> calculateShiftBasedSalary(salaryInfo, attendance);
        };
    }

    @Override
    public OvertimeResult calculateOvertime(
            List<DailyOvertimeDetail> dailyDetails,
            OvertimeConfig overtimeConfig,
            BigDecimal hourlyRate) {

        return overtimeCalculator.calculateOvertime(dailyDetails, overtimeConfig, hourlyRate);
    }

    @Override
    public BigDecimal calculateBreakDeduction(
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

    @Override
    public AllowanceResult calculateAllowances(
            AllowanceConfig allowanceConfig,
            AttendanceSummary attendance) {

        return allowanceCalculator.calculateAllowances(allowanceConfig, attendance);
    }

    @Override
    public DeductionResult calculateDeductions(
            DeductionConfig deductionConfig,
            AttendanceSummary attendance,
            BigDecimal grossSalary) {

        return deductionCalculator.calculateDeductions(deductionConfig, attendance, grossSalary);
    }

    @Override
    public BigDecimal calculateGrossSalary(
            BigDecimal baseSalary,
            BigDecimal overtimePay,
            BigDecimal totalAllowances) {

        BigDecimal gross = baseSalary != null ? baseSalary : BigDecimal.ZERO;

        if (overtimePay != null) {
            gross = gross.add(overtimePay);
        }

        if (totalAllowances != null) {
            gross = gross.add(totalAllowances);
        }

        return gross;
    }

    @Override
    public BigDecimal calculateNetSalary(
            BigDecimal grossSalary,
            BigDecimal totalDeductions,
            PayrollConfig payrollConfig) {

        BigDecimal net = grossSalary != null ? grossSalary : BigDecimal.ZERO;

        if (totalDeductions != null) {
            net = net.subtract(totalDeductions);
        }

        return roundSalary(net, payrollConfig);
    }

    @Override
    public BigDecimal calculateHourlyRate(
            EmployeeSalaryInfo salaryInfo,
            PayrollConfig config) {

        if (salaryInfo == null) {
            return BigDecimal.ZERO;
        }

        // Ưu tiên hourlyRate nếu có
        if (salaryInfo.getHourlyRate() != null) {
            return salaryInfo.getHourlyRate();
        }

        int hoursPerDay = config != null && config.getStandardWorkingHoursPerDay() != null
                ? config.getStandardWorkingHoursPerDay()
                : 8;

        // Tính từ dailyRate
        if (salaryInfo.getDailyRate() != null) {
            return salaryInfo.getDailyRate()
                    .divide(BigDecimal.valueOf(hoursPerDay), 4, RoundingMode.HALF_UP);
        }

        // Tính từ monthlySalary
        if (salaryInfo.getMonthlySalary() != null) {
            int daysPerMonth = config != null && config.getStandardWorkingDaysPerMonth() != null
                    ? config.getStandardWorkingDaysPerMonth()
                    : 22;
            int hoursPerMonth = daysPerMonth * hoursPerDay;

            return salaryInfo.getMonthlySalary()
                    .divide(BigDecimal.valueOf(hoursPerMonth), 4, RoundingMode.HALF_UP);
        }

        // Tính từ shiftRate (giả định 1 ca = 8 giờ)
        if (salaryInfo.getShiftRate() != null) {
            return salaryInfo.getShiftRate()
                    .divide(BigDecimal.valueOf(hoursPerDay), 4, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculatePenalties(
            DeductionConfig deductionConfig,
            AttendanceSummary attendance) {

        if (deductionConfig == null || attendance == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPenalty = BigDecimal.ZERO;

        // Phạt đi muộn
        if (Boolean.TRUE.equals(deductionConfig.getEnableLatePenalty())
                && deductionConfig.getLatePenaltyPerMinute() != null
                && attendance.getTotalLateMinutes() != null
                && attendance.getTotalLateMinutes() > 0) {

            BigDecimal latePenalty = deductionConfig.getLatePenaltyPerMinute()
                    .multiply(BigDecimal.valueOf(attendance.getTotalLateMinutes()))
                    .setScale(0, RoundingMode.HALF_UP);
            totalPenalty = totalPenalty.add(latePenalty);
        }

        // Phạt về sớm
        if (Boolean.TRUE.equals(deductionConfig.getEnableEarlyLeavePenalty())
                && deductionConfig.getEarlyLeavePenaltyPerMinute() != null
                && attendance.getTotalEarlyLeaveMinutes() != null
                && attendance.getTotalEarlyLeaveMinutes() > 0) {

            BigDecimal earlyLeavePenalty = deductionConfig.getEarlyLeavePenaltyPerMinute()
                    .multiply(BigDecimal.valueOf(attendance.getTotalEarlyLeaveMinutes()))
                    .setScale(0, RoundingMode.HALF_UP);
            totalPenalty = totalPenalty.add(earlyLeavePenalty);
        }

        return totalPenalty;
    }

    // === Private helper methods ===

    /**
     * Lương tháng = monthlySalary × (workingDays / standardWorkingDays)
     * Prorate theo số ngày làm việc thực tế
     */
    private BigDecimal calculateMonthlySalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            PayrollConfig config) {

        if (salaryInfo.getMonthlySalary() == null) {
            return BigDecimal.ZERO;
        }

        // Nếu không có attendance hoặc không cần prorate, trả về full salary
        if (attendance == null || attendance.getWorkingDays() == null) {
            return salaryInfo.getMonthlySalary();
        }

        int standardDays = config != null && config.getStandardWorkingDaysPerMonth() != null
                ? config.getStandardWorkingDaysPerMonth()
                : 22;

        // Prorate: monthlySalary × (actualDays / standardDays)
        return salaryInfo.getMonthlySalary()
                .multiply(BigDecimal.valueOf(attendance.getWorkingDays()))
                .divide(BigDecimal.valueOf(standardDays), 0, RoundingMode.HALF_UP);
    }

    /**
     * Lương ngày = dailyRate × workingDays
     */
    private BigDecimal calculateDailySalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance) {

        if (salaryInfo.getDailyRate() == null) {
            return BigDecimal.ZERO;
        }

        if (attendance == null || attendance.getWorkingDays() == null) {
            return BigDecimal.ZERO;
        }

        return salaryInfo.getDailyRate()
                .multiply(BigDecimal.valueOf(attendance.getWorkingDays()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Lương giờ = hourlyRate × workingHours
     */
    private BigDecimal calculateHourlySalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance) {

        if (salaryInfo.getHourlyRate() == null) {
            return BigDecimal.ZERO;
        }

        if (attendance == null || attendance.getWorkingHours() == null) {
            return BigDecimal.ZERO;
        }

        return salaryInfo.getHourlyRate()
                .multiply(BigDecimal.valueOf(attendance.getWorkingHours()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Lương theo ca = shiftRate × numberOfShifts
     */
    private BigDecimal calculateShiftBasedSalary(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance) {

        if (salaryInfo.getShiftRate() == null) {
            return BigDecimal.ZERO;
        }

        if (attendance == null || attendance.getNumberOfShifts() == null) {
            return BigDecimal.ZERO;
        }

        return salaryInfo.getShiftRate()
                .multiply(BigDecimal.valueOf(attendance.getNumberOfShifts()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Làm tròn lương theo cấu hình
     */
    private BigDecimal roundSalary(BigDecimal salary, PayrollConfig config) {
        if (salary == null) {
            return BigDecimal.ZERO;
        }

        if (config == null || config.getSalaryRounding() == null) {
            return salary.setScale(0, RoundingMode.HALF_UP);
        }

        return switch (config.getSalaryRounding()) {
            case UP -> salary.setScale(0, RoundingMode.CEILING);
            case DOWN -> salary.setScale(0, RoundingMode.FLOOR);
            case NEAREST -> salary.setScale(0, RoundingMode.HALF_UP);
        };
    }
}
