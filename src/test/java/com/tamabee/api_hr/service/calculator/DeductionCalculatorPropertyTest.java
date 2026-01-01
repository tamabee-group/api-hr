package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.config.DeductionRule;
import com.tamabee.api_hr.dto.result.AttendanceSummary;
import com.tamabee.api_hr.dto.result.DeductionItem;
import com.tamabee.api_hr.dto.result.DeductionResult;
import com.tamabee.api_hr.enums.DeductionType;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho DeductionCalculator
 * Feature: attendance-payroll-backend
 * Property 16: Deductions Sum Invariant
 */
public class DeductionCalculatorPropertyTest {

    private final DeductionCalculator calculator = new DeductionCalculator();

    /**
     * Property 16: Deductions Sum Invariant
     * For any deduction calculation, the sum of individual deduction amounts
     * SHALL equal total deductions.
     */
    @Property(tries = 100)
    void deductionsSumEqualsTotal(
            @ForAll("deductionConfigs") DeductionConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance,
            @ForAll("grossSalaries") BigDecimal grossSalary) {

        DeductionResult result = calculator.calculateDeductions(config, attendance, grossSalary);

        // Tính tổng từ các items
        BigDecimal sumFromItems = result.getItems().stream()
                .map(DeductionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, result.getTotalDeductions().compareTo(sumFromItems),
                "Sum of deduction items should equal total deductions");
    }

    /**
     * Property bổ sung: Khấu trừ không được âm
     */
    @Property(tries = 100)
    void deductionsAreNonNegative(
            @ForAll("deductionConfigs") DeductionConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance,
            @ForAll("grossSalaries") BigDecimal grossSalary) {

        DeductionResult result = calculator.calculateDeductions(config, attendance, grossSalary);

        assertTrue(result.getTotalDeductions().compareTo(BigDecimal.ZERO) >= 0,
                "Total deductions should be non-negative");

        for (DeductionItem item : result.getItems()) {
            assertTrue(item.getAmount().compareTo(BigDecimal.ZERO) >= 0,
                    "Each deduction item should be non-negative");
        }
    }

    /**
     * Property bổ sung: Late penalty + Early leave penalty nằm trong tổng
     */
    @Property(tries = 100)
    void penaltiesIncludedInTotal(
            @ForAll("deductionConfigs") DeductionConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance,
            @ForAll("grossSalaries") BigDecimal grossSalary) {

        DeductionResult result = calculator.calculateDeductions(config, attendance, grossSalary);

        BigDecimal penalties = result.getLatePenalty().add(result.getEarlyLeavePenalty());

        assertTrue(result.getTotalDeductions().compareTo(penalties) >= 0,
                "Total deductions should be >= sum of penalties");
    }

    // === Generators ===

    @Provide
    Arbitrary<DeductionConfig> deductionConfigs() {
        return Combinators.combine(
                deductionRuleLists(),
                Arbitraries.of(true, false),
                positiveBigDecimals(),
                Arbitraries.of(true, false),
                positiveBigDecimals(),
                Arbitraries.of(true, false))
                .as((rules, enableLate, latePenalty, enableEarly, earlyPenalty, enableAbsence) -> DeductionConfig
                        .builder()
                        .deductions(rules)
                        .enableLatePenalty(enableLate)
                        .latePenaltyPerMinute(latePenalty)
                        .enableEarlyLeavePenalty(enableEarly)
                        .earlyLeavePenaltyPerMinute(earlyPenalty)
                        .enableAbsenceDeduction(enableAbsence)
                        .build());
    }

    @Provide
    Arbitrary<AttendanceSummary> attendanceSummaries() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 31),
                Arbitraries.integers().between(0, 250),
                Arbitraries.integers().between(0, 10),
                Arbitraries.integers().between(0, 20),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 20),
                Arbitraries.integers().between(0, 300),
                Arbitraries.integers().between(0, 2400))
                .as((workingDays, workingHours, absenceDays, lateCount, lateMinutes,
                        earlyCount, earlyMinutes, overtimeMinutes) -> AttendanceSummary.builder()
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
    Arbitrary<BigDecimal> grossSalaries() {
        return Arbitraries.doubles().between(100000, 10000000)
                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
    }

    private Arbitrary<List<DeductionRule>> deductionRuleLists() {
        return deductionRules().list().ofMaxSize(10);
    }

    private Arbitrary<DeductionRule> deductionRules() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.of(DeductionType.values()),
                positiveBigDecimals().injectNull(0.3),
                Arbitraries.doubles().between(0.1, 30.0)
                        .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP))
                        .injectNull(0.3),
                Arbitraries.integers().between(1, 10))
                .as((code, name, type, amount, percentage, order) -> DeductionRule.builder()
                        .code(code)
                        .name(name)
                        .type(type)
                        .amount(amount)
                        .percentage(percentage)
                        .order(order)
                        .build());
    }

    private Arbitrary<BigDecimal> positiveBigDecimals() {
        return Arbitraries.doubles().between(10, 1000)
                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
    }
}
