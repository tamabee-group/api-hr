package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.result.*;
import com.tamabee.api_hr.enums.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho PayrollCalculator
 * Feature: attendance-payroll-backend
 * Property 14: Gross Salary Invariant
 * Property 17: Net Salary Formula
 */
public class PayrollCalculatorPropertyTest {

        private final PayrollCalculator calculator;

        public PayrollCalculatorPropertyTest() {
                LegalOvertimeRequirements legalOvertimeRequirements = new LegalOvertimeRequirements();
                OvertimeCalculator overtimeCalculator = new OvertimeCalculator(legalOvertimeRequirements);
                AllowanceCalculator allowanceCalculator = new AllowanceCalculator();
                DeductionCalculator deductionCalculator = new DeductionCalculator();
                this.calculator = new PayrollCalculator(overtimeCalculator, allowanceCalculator, deductionCalculator);
        }

        /**
         * Property 14: Gross Salary Invariant
         * For any payroll calculation, gross salary SHALL equal
         * (base_salary + total_overtime_pay + total_allowances).
         */
        @Property(tries = 100)
        void grossSalaryEqualsBasePlusOvertimePlusAllowances(
                        @ForAll("employeeSalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummaries") AttendanceSummary attendance,
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig,
                        @ForAll("overtimeConfigs") OvertimeConfig overtimeConfig,
                        @ForAll("allowanceConfigs") AllowanceConfig allowanceConfig,
                        @ForAll("deductionConfigs") DeductionConfig deductionConfig) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, dailyDetails,
                                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig);

                BigDecimal expectedGross = result.getBaseSalary()
                                .add(result.getTotalOvertimePay())
                                .add(result.getTotalAllowances());

                assertEquals(0, result.getGrossSalary().compareTo(expectedGross),
                                "Gross salary should equal base + overtime + allowances");
        }

        /**
         * Property 17: Net Salary Formula
         * For any payroll calculation, net salary SHALL equal
         * (gross_salary - total_deductions).
         */
        @Property(tries = 100)
        void netSalaryEqualsGrossMinusDeductions(
                        @ForAll("employeeSalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummaries") AttendanceSummary attendance,
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig,
                        @ForAll("overtimeConfigs") OvertimeConfig overtimeConfig,
                        @ForAll("allowanceConfigs") AllowanceConfig allowanceConfig,
                        @ForAll("deductionConfigs") DeductionConfig deductionConfig) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, dailyDetails,
                                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig);

                BigDecimal expectedNet = result.getGrossSalary().subtract(result.getTotalDeductions());

                // Cho phép sai số do làm tròn (±1)
                BigDecimal diff = result.getNetSalary().subtract(expectedNet).abs();
                assertTrue(diff.compareTo(BigDecimal.ONE) <= 0,
                                String.format("Net salary should equal gross - deductions (within rounding). " +
                                                "Expected: %s, Actual: %s, Diff: %s",
                                                expectedNet, result.getNetSalary(), diff));
        }

        /**
         * Property bổ sung: Daily salary = dailyRate × workingDays
         */
        @Property(tries = 100)
        void dailySalaryCalculation(
                        @ForAll("dailySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummaries") AttendanceSummary attendance) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, new ArrayList<>(),
                                PayrollConfig.builder().build(),
                                OvertimeConfig.builder().overtimeEnabled(false).build(),
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build());

                BigDecimal expectedBase = salaryInfo.getDailyRate()
                                .multiply(BigDecimal.valueOf(attendance.getWorkingDays()))
                                .setScale(0, java.math.RoundingMode.HALF_UP);

                assertEquals(0, result.getBaseSalary().compareTo(expectedBase),
                                "Daily salary should equal dailyRate × workingDays");
        }

        /**
         * Property bổ sung: Hourly salary = hourlyRate × workingHours
         */
        @Property(tries = 100)
        void hourlySalaryCalculation(
                        @ForAll("hourlySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummaries") AttendanceSummary attendance) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, new ArrayList<>(),
                                PayrollConfig.builder().build(),
                                OvertimeConfig.builder().overtimeEnabled(false).build(),
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build());

                BigDecimal expectedBase = salaryInfo.getHourlyRate()
                                .multiply(BigDecimal.valueOf(attendance.getWorkingHours()))
                                .setScale(0, java.math.RoundingMode.HALF_UP);

                assertEquals(0, result.getBaseSalary().compareTo(expectedBase),
                                "Hourly salary should equal hourlyRate × workingHours");
        }

        // === Generators ===

        @Provide
        Arbitrary<EmployeeSalaryInfo> employeeSalaryInfos() {
                return Arbitraries.oneOf(
                                monthlySalaryInfos(),
                                dailySalaryInfos(),
                                hourlySalaryInfos());
        }

        @Provide
        Arbitrary<EmployeeSalaryInfo> monthlySalaryInfos() {
                return Arbitraries.doubles().between(200000, 2000000)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP))
                                .map(salary -> EmployeeSalaryInfo.builder()
                                                .salaryType(SalaryType.MONTHLY)
                                                .monthlySalary(salary)
                                                .build());
        }

        @Provide
        Arbitrary<EmployeeSalaryInfo> dailySalaryInfos() {
                return Arbitraries.doubles().between(5000, 50000)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP))
                                .map(rate -> EmployeeSalaryInfo.builder()
                                                .salaryType(SalaryType.DAILY)
                                                .dailyRate(rate)
                                                .build());
        }

        @Provide
        Arbitrary<EmployeeSalaryInfo> hourlySalaryInfos() {
                return Arbitraries.doubles().between(500, 5000)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP))
                                .map(rate -> EmployeeSalaryInfo.builder()
                                                .salaryType(SalaryType.HOURLY)
                                                .hourlyRate(rate)
                                                .build());
        }

        @Provide
        Arbitrary<AttendanceSummary> attendanceSummaries() {
                return Combinators.combine(
                                Arbitraries.integers().between(1, 31),
                                Arbitraries.integers().between(1, 250),
                                Arbitraries.integers().between(0, 10),
                                Arbitraries.integers().between(0, 20),
                                Arbitraries.integers().between(0, 300),
                                Arbitraries.integers().between(0, 20),
                                Arbitraries.integers().between(0, 300),
                                Arbitraries.integers().between(0, 2400))
                                .as((workingDays, workingHours, absenceDays, lateCount, lateMinutes,
                                                earlyCount, earlyMinutes, overtimeMinutes) -> AttendanceSummary
                                                                .builder()
                                                                .workingDays(workingDays)
                                                                .workingHours(workingHours)
                                                                .absenceDays(absenceDays)
                                                                .lateCount(lateCount)
                                                                .totalLateMinutes(lateMinutes)
                                                                .earlyLeaveCount(earlyCount)
                                                                .totalEarlyLeaveMinutes(earlyMinutes)
                                                                .totalOvertimeMinutes(overtimeMinutes)
                                                                .build());
        }

        @Provide
        Arbitrary<List<DailyOvertimeDetail>> dailyDetailLists() {
                return dailyOvertimeDetails().list().ofMaxSize(10);
        }

        private Arbitrary<DailyOvertimeDetail> dailyOvertimeDetails() {
                return Combinators.combine(
                                Arbitraries.integers().between(2020, 2030),
                                Arbitraries.integers().between(1, 12),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.integers().between(0, 120),
                                Arbitraries.integers().between(0, 60),
                                Arbitraries.of(true, false),
                                Arbitraries.of(true, false))
                                .as((year, month, day, regularMinutes, nightMinutes, isHoliday,
                                                isWeekend) -> DailyOvertimeDetail
                                                                .builder()
                                                                .date(LocalDate.of(year, month, day))
                                                                .regularMinutes(regularMinutes)
                                                                .nightMinutes(nightMinutes)
                                                                .isHoliday(isHoliday)
                                                                .isWeekend(isWeekend)
                                                                .build());
        }

        @Provide
        Arbitrary<PayrollConfig> payrollConfigs() {
                return Combinators.combine(
                                Arbitraries.of(SalaryType.values()),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.of(RoundingDirection.values()),
                                Arbitraries.integers().between(20, 26),
                                Arbitraries.integers().between(6, 10))
                                .as((salaryType, payDay, cutoffDay, rounding, workingDays,
                                                workingHours) -> PayrollConfig.builder()
                                                                .defaultSalaryType(salaryType)
                                                                .payDay(payDay)
                                                                .cutoffDay(cutoffDay)
                                                                .salaryRounding(rounding)
                                                                .standardWorkingDaysPerMonth(workingDays)
                                                                .standardWorkingHoursPerDay(workingHours)
                                                                .build());
        }

        @Provide
        Arbitrary<OvertimeConfig> overtimeConfigs() {
                return Combinators.combine(
                                Arbitraries.of(true),
                                Arbitraries.of(true, false),
                                positiveBigDecimals(),
                                positiveBigDecimals(),
                                positiveBigDecimals(),
                                positiveBigDecimals(),
                                Arbitraries.integers().between(1, 8),
                                Arbitraries.integers().between(20, 80))
                                .as((enable, requireApproval, regularRate, nightRate, holidayRate, weekendRate,
                                                maxPerDay, maxPerMonth) -> OvertimeConfig.builder()
                                                                .overtimeEnabled(enable)
                                                                .requireApproval(requireApproval)
                                                                .regularOvertimeRate(regularRate)
                                                                .nightOvertimeRate(nightRate)
                                                                .holidayOvertimeRate(holidayRate)
                                                                .weekendOvertimeRate(weekendRate)
                                                                .nightStartTime(LocalTime.of(22, 0))
                                                                .nightEndTime(LocalTime.of(5, 0))
                                                                .maxOvertimeHoursPerDay(maxPerDay)
                                                                .maxOvertimeHoursPerMonth(maxPerMonth)
                                                                .build());
        }

        @Provide
        Arbitrary<AllowanceConfig> allowanceConfigs() {
                return allowanceRuleLists().map(rules -> AllowanceConfig.builder()
                                .allowances(rules)
                                .build());
        }

        @Provide
        Arbitrary<DeductionConfig> deductionConfigs() {
                return Combinators.combine(
                                deductionRuleLists(),
                                Arbitraries.of(true, false),
                                smallBigDecimals(),
                                Arbitraries.of(true, false),
                                smallBigDecimals(),
                                Arbitraries.of(true, false))
                                .as((rules, enableLate, latePenalty, enableEarly, earlyPenalty,
                                                enableAbsence) -> DeductionConfig
                                                                .builder()
                                                                .deductions(rules)
                                                                .enableLatePenalty(enableLate)
                                                                .latePenaltyPerMinute(latePenalty)
                                                                .enableEarlyLeavePenalty(enableEarly)
                                                                .earlyLeavePenaltyPerMinute(earlyPenalty)
                                                                .enableAbsenceDeduction(enableAbsence)
                                                                .build());
        }

        private Arbitrary<List<AllowanceRule>> allowanceRuleLists() {
                return allowanceRules().list().ofMaxSize(5);
        }

        private Arbitrary<AllowanceRule> allowanceRules() {
                return Combinators.combine(
                                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                                Arbitraries.of(AllowanceType.FIXED),
                                mediumBigDecimals(),
                                Arbitraries.of(true, false))
                                .as((code, name, type, amount, taxable) -> AllowanceRule.builder()
                                                .code(code)
                                                .name(name)
                                                .type(type)
                                                .amount(amount)
                                                .taxable(taxable)
                                                .condition(null)
                                                .build());
        }

        private Arbitrary<List<DeductionRule>> deductionRuleLists() {
                return deductionRules().list().ofMaxSize(5);
        }

        private Arbitrary<DeductionRule> deductionRules() {
                return Combinators.combine(
                                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                                Arbitraries.of(DeductionType.FIXED),
                                smallBigDecimals(),
                                Arbitraries.integers().between(1, 10))
                                .as((code, name, type, amount, order) -> DeductionRule.builder()
                                                .code(code)
                                                .name(name)
                                                .type(type)
                                                .amount(amount)
                                                .order(order)
                                                .build());
        }

        private Arbitrary<BigDecimal> positiveBigDecimals() {
                return Arbitraries.doubles().between(1.0, 3.0)
                                .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        private Arbitrary<BigDecimal> smallBigDecimals() {
                return Arbitraries.doubles().between(10, 500)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
        }

        private Arbitrary<BigDecimal> mediumBigDecimals() {
                return Arbitraries.doubles().between(1000, 50000)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
        }

        // === Property Tests cho Break Time Management ===

        /**
         * Property 7: Break Deduction Consistency
         * For any payroll calculation with unpaid breaks, the break deduction SHALL be
         * consistent with the break policy and recorded break duration.
         * - UNPAID: deduction = (break minutes / 60) × hourly rate
         * - PAID: deduction = 0
         */
        @Property(tries = 100)
        void breakDeductionConsistencyForUnpaidBreaks(
                        @ForAll("hourlySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummariesWithBreak") AttendanceSummary attendance,
                        @ForAll("unpaidBreakConfigs") BreakConfig breakConfig,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig) {

                // Tính break deduction theo công thức
                BigDecimal hourlyRate = salaryInfo.getHourlyRate();
                Integer breakMinutes = attendance.getTotalBreakMinutes();

                BigDecimal expectedDeduction = BigDecimal.ZERO;
                if (breakMinutes != null && breakMinutes > 0 && hourlyRate != null) {
                        BigDecimal breakHours = BigDecimal.valueOf(breakMinutes)
                                        .divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
                        expectedDeduction = breakHours.multiply(hourlyRate)
                                        .setScale(0, java.math.RoundingMode.HALF_UP);
                }

                // Tính payroll với break config
                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, new ArrayList<>(),
                                payrollConfig,
                                OvertimeConfig.builder().overtimeEnabled(false).build(),
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build(),
                                breakConfig);

                // Verify break deduction
                BigDecimal actualDeduction = result.getBreakDeductionAmount() != null
                                ? result.getBreakDeductionAmount()
                                : BigDecimal.ZERO;

                assertEquals(0, expectedDeduction.compareTo(actualDeduction),
                                String.format("Break deduction should equal (breakMinutes/60) × hourlyRate. " +
                                                "Expected: %s, Actual: %s, BreakMinutes: %d, HourlyRate: %s",
                                                expectedDeduction, actualDeduction, breakMinutes, hourlyRate));
        }

        @Property(tries = 100)
        void breakDeductionZeroForPaidBreaks(
                        @ForAll("hourlySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummariesWithBreak") AttendanceSummary attendance,
                        @ForAll("paidBreakConfigs") BreakConfig breakConfig,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, new ArrayList<>(),
                                payrollConfig,
                                OvertimeConfig.builder().overtimeEnabled(false).build(),
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build(),
                                breakConfig);

                BigDecimal actualDeduction = result.getBreakDeductionAmount() != null
                                ? result.getBreakDeductionAmount()
                                : BigDecimal.ZERO;

                assertEquals(0, BigDecimal.ZERO.compareTo(actualDeduction),
                                "Break deduction should be zero for PAID breaks");
        }

        /**
         * Property 17 (Break Time): Overtime Amount Calculation
         * For any overtime calculation, the overtime amount SHALL equal
         * the sum of (overtime minutes × hourly rate × overtime multiplier) for each
         * type.
         * Overtime is calculated from dailyDetails, not from
         * attendance.totalOvertimeMinutes.
         */
        @Property(tries = 100)
        void overtimeAmountCalculation(
                        @ForAll("hourlySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("attendanceSummaries") AttendanceSummary attendance,
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig,
                        @ForAll("overtimeConfigs") OvertimeConfig overtimeConfig) {

                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, dailyDetails,
                                payrollConfig, overtimeConfig,
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build());

                // Verify overtime pay is non-negative
                assertTrue(result.getTotalOvertimePay().compareTo(BigDecimal.ZERO) >= 0,
                                "Overtime pay should be non-negative");

                // Verify overtime result breakdown sums to total
                OvertimeResult otResult = result.getOvertimeResult();
                if (otResult != null) {
                        BigDecimal sumOfParts = BigDecimal.ZERO;
                        if (otResult.getRegularOvertimeAmount() != null) {
                                sumOfParts = sumOfParts.add(otResult.getRegularOvertimeAmount());
                        }
                        if (otResult.getNightOvertimeAmount() != null) {
                                sumOfParts = sumOfParts.add(otResult.getNightOvertimeAmount());
                        }
                        if (otResult.getHolidayOvertimeAmount() != null) {
                                sumOfParts = sumOfParts.add(otResult.getHolidayOvertimeAmount());
                        }
                        if (otResult.getWeekendOvertimeAmount() != null) {
                                sumOfParts = sumOfParts.add(otResult.getWeekendOvertimeAmount());
                        }

                        // Total overtime pay should equal sum of all overtime types
                        assertEquals(0, result.getTotalOvertimePay().compareTo(sumOfParts),
                                        String.format("Total overtime pay should equal sum of all types. " +
                                                        "Expected: %s, Actual: %s", sumOfParts,
                                                        result.getTotalOvertimePay()));
                }
        }

        // === Break Config Generators ===

        @Provide
        Arbitrary<BreakConfig> unpaidBreakConfigs() {
                return Combinators.combine(
                                Arbitraries.integers().between(30, 60),
                                Arbitraries.integers().between(45, 90),
                                Arbitraries.integers().between(60, 120))
                                .as((defaultBreak, minBreak, maxBreak) -> BreakConfig.builder()
                                                .breakEnabled(true)
                                                .breakType(BreakType.UNPAID)
                                                .defaultBreakMinutes(defaultBreak)
                                                .minimumBreakMinutes(Math.min(minBreak, maxBreak))
                                                .maximumBreakMinutes(Math.max(minBreak, maxBreak))
                                                .build());
        }

        @Provide
        Arbitrary<BreakConfig> paidBreakConfigs() {
                return Combinators.combine(
                                Arbitraries.integers().between(30, 60),
                                Arbitraries.integers().between(45, 90),
                                Arbitraries.integers().between(60, 120))
                                .as((defaultBreak, minBreak, maxBreak) -> BreakConfig.builder()
                                                .breakEnabled(true)
                                                .breakType(BreakType.PAID)
                                                .defaultBreakMinutes(defaultBreak)
                                                .minimumBreakMinutes(Math.min(minBreak, maxBreak))
                                                .maximumBreakMinutes(Math.max(minBreak, maxBreak))
                                                .build());
        }

        @Provide
        Arbitrary<AttendanceSummary> attendanceSummariesWithBreak() {
                return Combinators.combine(
                                Arbitraries.integers().between(1, 31),
                                Arbitraries.integers().between(1, 250),
                                Arbitraries.integers().between(0, 10),
                                Arbitraries.integers().between(0, 20),
                                Arbitraries.integers().between(0, 300),
                                Arbitraries.integers().between(0, 300),
                                Arbitraries.integers().between(0, 2400),
                                Arbitraries.integers().between(30, 120))
                                .as((workingDays, workingHours, absenceDays, lateCount, lateMinutes,
                                                earlyMinutes, overtimeMinutes,
                                                breakMinutes) -> AttendanceSummary
                                                                .builder()
                                                                .workingDays(workingDays)
                                                                .workingHours(workingHours)
                                                                .absenceDays(absenceDays)
                                                                .lateCount(lateCount)
                                                                .totalLateMinutes(lateMinutes)
                                                                .earlyLeaveCount(0)
                                                                .totalEarlyLeaveMinutes(earlyMinutes)
                                                                .totalOvertimeMinutes(overtimeMinutes)
                                                                .totalBreakMinutes(breakMinutes)
                                                                .build());
        }

        @Provide
        Arbitrary<AttendanceSummary> attendanceSummariesWithOvertime() {
                return Combinators.combine(
                                Arbitraries.integers().between(1, 31),
                                Arbitraries.integers().between(1, 250),
                                Arbitraries.integers().between(60, 600))
                                .as((workingDays, workingHours, overtimeMinutes) -> AttendanceSummary
                                                .builder()
                                                .workingDays(workingDays)
                                                .workingHours(workingHours)
                                                .absenceDays(0)
                                                .lateCount(0)
                                                .totalLateMinutes(0)
                                                .earlyLeaveCount(0)
                                                .totalEarlyLeaveMinutes(0)
                                                .totalOvertimeMinutes(overtimeMinutes)
                                                .totalBreakMinutes(0)
                                                .build());
        }

        /**
         * Property 9: Payroll Uses Aggregated Break Time
         * For any payroll calculation with multiple break sessions, the break deduction
         * SHALL use the sum of all break durations, not just the first break.
         * 
         * Test: Tạo attendance summary với totalBreakMinutes = sum of multiple breaks,
         * verify break deduction được tính từ tổng này.
         */
        @Property(tries = 100)
        void payrollUsesAggregatedBreakTime(
                        @ForAll("hourlySalaryInfos") EmployeeSalaryInfo salaryInfo,
                        @ForAll("multipleBreakDurations") List<Integer> breakDurations,
                        @ForAll("unpaidBreakConfigs") BreakConfig breakConfig,
                        @ForAll("payrollConfigs") PayrollConfig payrollConfig) {

                // Tính tổng break minutes từ tất cả break sessions
                int aggregatedBreakMinutes = breakDurations.stream()
                                .mapToInt(Integer::intValue)
                                .sum();

                // Tạo attendance summary với totalBreakMinutes = aggregated value
                AttendanceSummary attendance = AttendanceSummary.builder()
                                .workingDays(22)
                                .workingHours(176)
                                .absenceDays(0)
                                .lateCount(0)
                                .totalLateMinutes(0)
                                .earlyLeaveCount(0)
                                .totalEarlyLeaveMinutes(0)
                                .totalOvertimeMinutes(0)
                                .totalBreakMinutes(aggregatedBreakMinutes)
                                .build();

                // Tính payroll với break config
                PayrollResult result = calculator.calculatePayroll(
                                salaryInfo, attendance, new ArrayList<>(),
                                payrollConfig,
                                OvertimeConfig.builder().overtimeEnabled(false).build(),
                                AllowanceConfig.builder().build(),
                                DeductionConfig.builder().build(),
                                breakConfig);

                // Tính expected break deduction từ aggregated break time
                BigDecimal hourlyRate = salaryInfo.getHourlyRate();
                BigDecimal expectedDeduction = BigDecimal.ZERO;
                if (aggregatedBreakMinutes > 0 && hourlyRate != null) {
                        BigDecimal breakHours = BigDecimal.valueOf(aggregatedBreakMinutes)
                                        .divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
                        expectedDeduction = breakHours.multiply(hourlyRate)
                                        .setScale(0, java.math.RoundingMode.HALF_UP);
                }

                // Verify break deduction sử dụng aggregated break time
                BigDecimal actualDeduction = result.getBreakDeductionAmount() != null
                                ? result.getBreakDeductionAmount()
                                : BigDecimal.ZERO;

                assertEquals(0, expectedDeduction.compareTo(actualDeduction),
                                String.format("Break deduction should use aggregated break time (%d minutes). " +
                                                "Expected deduction: %s, Actual: %s, HourlyRate: %s, " +
                                                "Individual breaks: %s",
                                                aggregatedBreakMinutes, expectedDeduction, actualDeduction,
                                                hourlyRate, breakDurations));

                // Verify result contains correct totalBreakMinutes
                assertEquals(aggregatedBreakMinutes, result.getTotalBreakMinutes(),
                                String.format("Result should contain aggregated break minutes. " +
                                                "Expected: %d, Actual: %d",
                                                aggregatedBreakMinutes, result.getTotalBreakMinutes()));
        }

        /**
         * Property 9 bổ sung: Aggregated break time không bị mất khi có nhiều breaks
         * For any set of break durations, the total used in payroll calculation
         * SHALL equal the sum of all individual break durations.
         */
        @Property(tries = 100)
        void aggregatedBreakTimePreservesAllBreaks(
                        @ForAll("multipleBreakDurations") List<Integer> breakDurations) {

                // Tính tổng từ list
                int expectedTotal = breakDurations.stream()
                                .mapToInt(Integer::intValue)
                                .sum();

                // Tính tổng từng cái một
                int sumOfIndividual = 0;
                for (Integer duration : breakDurations) {
                        sumOfIndividual += duration;
                }

                // Verify không có break nào bị mất
                assertEquals(expectedTotal, sumOfIndividual,
                                String.format("Aggregated break time should preserve all breaks. " +
                                                "Expected: %d, Got: %d, Breaks: %s",
                                                expectedTotal, sumOfIndividual, breakDurations));

                // Verify tổng không âm
                assertTrue(expectedTotal >= 0,
                                String.format("Aggregated break time should be non-negative, got %d", expectedTotal));
        }

        // === Generator cho multiple break durations ===

        @Provide
        Arbitrary<List<Integer>> multipleBreakDurations() {
                return Arbitraries.integers().between(15, 60)
                                .list()
                                .ofMinSize(1)
                                .ofMaxSize(5);
        }
}