package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.AllowanceCondition;
import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.config.AllowanceRule;
import com.tamabee.api_hr.dto.result.AllowanceItem;
import com.tamabee.api_hr.dto.result.AllowanceResult;
import com.tamabee.api_hr.dto.result.AttendanceSummary;
import com.tamabee.api_hr.enums.AllowanceType;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho AllowanceCalculator
 * Feature: attendance-payroll-backend
 * Property 15: Allowances Sum Invariant
 */
public class AllowanceCalculatorPropertyTest {

    private final AllowanceCalculator calculator = new AllowanceCalculator();

    /**
     * Property 15: Allowances Sum Invariant
     * For any allowance calculation, the sum of individual allowance amounts
     * SHALL equal total allowances.
     */
    @Property(tries = 100)
    void allowancesSumEqualsTotal(
            @ForAll("allowanceConfigs") AllowanceConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance) {

        AllowanceResult result = calculator.calculateAllowances(config, attendance);

        // Tính tổng từ các items đủ điều kiện
        BigDecimal sumFromItems = result.getItems().stream()
                .filter(item -> item.getIneligibleReason() == null)
                .map(AllowanceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, result.getTotalAllowances().compareTo(sumFromItems),
                "Sum of eligible allowance items should equal total allowances");
    }

    /**
     * Property bổ sung: Tổng phụ cấp = phụ cấp chịu thuế + phụ cấp không chịu thuế
     */
    @Property(tries = 100)
    void taxableAndNonTaxableSumEqualsTotal(
            @ForAll("allowanceConfigs") AllowanceConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance) {

        AllowanceResult result = calculator.calculateAllowances(config, attendance);

        BigDecimal sumOfTaxCategories = result.getTaxableAllowances()
                .add(result.getNonTaxableAllowances());

        assertEquals(0, result.getTotalAllowances().compareTo(sumOfTaxCategories),
                "Taxable + Non-taxable allowances should equal total allowances");
    }

    /**
     * Property bổ sung: FIXED allowances luôn được tính (không có điều kiện)
     */
    @Property(tries = 100)
    void fixedAllowancesAlwaysApplied(
            @ForAll("fixedAllowanceConfigs") AllowanceConfig config,
            @ForAll("attendanceSummaries") AttendanceSummary attendance) {

        AllowanceResult result = calculator.calculateAllowances(config, attendance);

        // Tất cả FIXED allowances phải đủ điều kiện
        for (AllowanceItem item : result.getItems()) {
            if (item.getType() == AllowanceType.FIXED) {
                assertNull(item.getIneligibleReason(),
                        "FIXED allowances should always be eligible");
            }
        }
    }

    // === Generators ===

    @Provide
    Arbitrary<AllowanceConfig> allowanceConfigs() {
        return allowanceRuleLists().map(rules -> AllowanceConfig.builder()
                .allowances(rules)
                .build());
    }

    @Provide
    Arbitrary<AllowanceConfig> fixedAllowanceConfigs() {
        return fixedAllowanceRuleLists().map(rules -> AllowanceConfig.builder()
                .allowances(rules)
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

    private Arbitrary<List<AllowanceRule>> allowanceRuleLists() {
        return allowanceRules().list().ofMaxSize(10);
    }

    private Arbitrary<List<AllowanceRule>> fixedAllowanceRuleLists() {
        return fixedAllowanceRules().list().ofMaxSize(10);
    }

    private Arbitrary<AllowanceRule> allowanceRules() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.of(AllowanceType.values()),
                positiveBigDecimals(),
                Arbitraries.of(true, false),
                allowanceConditions().injectNull(0.5))
                .as((code, name, type, amount, taxable, condition) -> AllowanceRule.builder()
                        .code(code)
                        .name(name)
                        .type(type)
                        .amount(amount)
                        .taxable(taxable)
                        .condition(condition)
                        .build());
    }

    private Arbitrary<AllowanceRule> fixedAllowanceRules() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                positiveBigDecimals(),
                Arbitraries.of(true, false))
                .as((code, name, amount, taxable) -> AllowanceRule.builder()
                        .code(code)
                        .name(name)
                        .type(AllowanceType.FIXED)
                        .amount(amount)
                        .taxable(taxable)
                        .condition(null)
                        .build());
    }

    private Arbitrary<AllowanceCondition> allowanceConditions() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 30).injectNull(0.3),
                Arbitraries.integers().between(1, 200).injectNull(0.3),
                Arbitraries.of(true, false, null),
                Arbitraries.of(true, false, null),
                Arbitraries.of(true, false, null))
                .as((minDays, minHours, noAbsence, noLate, noEarly) -> AllowanceCondition.builder()
                        .minWorkingDays(minDays)
                        .minWorkingHours(minHours)
                        .noAbsence(noAbsence)
                        .noLateArrival(noLate)
                        .noEarlyLeave(noEarly)
                        .build());
    }

    private Arbitrary<BigDecimal> positiveBigDecimals() {
        return Arbitraries.doubles().between(100, 100000)
                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
    }
}
