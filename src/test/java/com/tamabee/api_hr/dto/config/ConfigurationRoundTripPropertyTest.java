package com.tamabee.api_hr.dto.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tamabee.api_hr.enums.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho Configuration DTOs round-trip
 * Feature: attendance-payroll-backend, Property 1: Configuration Round-Trip
 * Consistency
 */
public class ConfigurationRoundTripPropertyTest {

    private final ObjectMapper objectMapper;

    public ConfigurationRoundTripPropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Property 1: Configuration Round-Trip Consistency
     * For any valid AttendanceConfig, serializing to JSON then deserializing back
     * SHALL produce an equivalent configuration object.
     */
    @Property(tries = 100)
    void attendanceConfigRoundTrip(@ForAll("attendanceConfigs") AttendanceConfig original)
            throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        AttendanceConfig deserialized = objectMapper.readValue(json, AttendanceConfig.class);
        assertEquals(original, deserialized);
    }

    /**
     * Property 1: Configuration Round-Trip Consistency
     * For any valid PayrollConfig, serializing to JSON then deserializing back
     * SHALL produce an equivalent configuration object.
     */
    @Property(tries = 100)
    void payrollConfigRoundTrip(@ForAll("payrollConfigs") PayrollConfig original) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        PayrollConfig deserialized = objectMapper.readValue(json, PayrollConfig.class);
        assertEquals(original, deserialized);
    }

    /**
     * Property 1: Configuration Round-Trip Consistency
     * For any valid OvertimeConfig, serializing to JSON then deserializing back
     * SHALL produce an equivalent configuration object.
     */
    @Property(tries = 100)
    void overtimeConfigRoundTrip(@ForAll("overtimeConfigs") OvertimeConfig original) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        OvertimeConfig deserialized = objectMapper.readValue(json, OvertimeConfig.class);
        assertEquals(original, deserialized);
    }

    /**
     * Property 1: Configuration Round-Trip Consistency
     * For any valid AllowanceConfig, serializing to JSON then deserializing back
     * SHALL produce an equivalent configuration object.
     */
    @Property(tries = 100)
    void allowanceConfigRoundTrip(@ForAll("allowanceConfigs") AllowanceConfig original) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        AllowanceConfig deserialized = objectMapper.readValue(json, AllowanceConfig.class);
        assertEquals(original, deserialized);
    }

    /**
     * Property 1: Configuration Round-Trip Consistency
     * For any valid DeductionConfig, serializing to JSON then deserializing back
     * SHALL produce an equivalent configuration object.
     */
    @Property(tries = 100)
    void deductionConfigRoundTrip(@ForAll("deductionConfigs") DeductionConfig original) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        DeductionConfig deserialized = objectMapper.readValue(json, DeductionConfig.class);
        assertEquals(original, deserialized);
    }

    // === Generators ===

    @Provide
    Arbitrary<AttendanceConfig> attendanceConfigs() {
        return Combinators.combine(
                localTimes(),
                localTimes(),
                Arbitraries.integers().between(0, 120),
                Arbitraries.of(true, false),
                roundingConfigs().injectNull(0.3),
                roundingConfigs().injectNull(0.3),
                Arbitraries.integers().between(0, 30),
                Arbitraries.integers().between(0, 30))
                .flatAs((startTime, endTime, breakMinutes, enableRounding, checkInRounding, checkOutRounding,
                        lateGrace, earlyGrace) -> Combinators.combine(
                                Arbitraries.of(true, false),
                                Arbitraries.of(true, false),
                                Arbitraries.integers().between(50, 500),
                                Arbitraries.of(true, false),
                                Arbitraries.of(true, false))
                                .as((requireDevice, requireGeo, geoRadius, allowMobile, allowWeb) -> AttendanceConfig
                                        .builder()
                                        .defaultWorkStartTime(startTime)
                                        .defaultWorkEndTime(endTime)
                                        .defaultBreakMinutes(breakMinutes)
                                        .enableRounding(enableRounding)
                                        .checkInRounding(checkInRounding)
                                        .checkOutRounding(checkOutRounding)
                                        .lateGraceMinutes(lateGrace)
                                        .earlyLeaveGraceMinutes(earlyGrace)
                                        .requireDeviceRegistration(requireDevice)
                                        .requireGeoLocation(requireGeo)
                                        .geoFenceRadiusMeters(geoRadius)
                                        .allowMobileCheckIn(allowMobile)
                                        .allowWebCheckIn(allowWeb)
                                        .build()));
    }

    @Provide
    Arbitrary<RoundingConfig> roundingConfigs() {
        return Combinators.combine(
                Arbitraries.of(RoundingInterval.values()),
                Arbitraries.of(RoundingDirection.values())).as(
                        (interval, direction) -> RoundingConfig.builder()
                                .interval(interval)
                                .direction(direction)
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
                Arbitraries.integers().between(6, 12))
                .as((salaryType, payDay, cutoffDay, rounding, workingDays, workingHours) -> PayrollConfig.builder()
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
                Arbitraries.of(true, false),
                Arbitraries.of(true, false),
                positiveBigDecimals(),
                positiveBigDecimals(),
                positiveBigDecimals(),
                positiveBigDecimals(),
                localTimes(),
                localTimes()).flatAs(
                        (enable, requireApproval, regularRate, nightRate, holidayRate, weekendRate,
                                nightStart, nightEnd) -> Combinators.combine(
                                        Arbitraries.integers().between(1, 8),
                                        Arbitraries.integers().between(20, 80))
                                        .as((maxPerDay, maxPerMonth) -> OvertimeConfig.builder()
                                                .overtimeEnabled(enable)
                                                .requireApproval(requireApproval)
                                                .regularOvertimeRate(regularRate)
                                                .nightOvertimeRate(nightRate)
                                                .holidayOvertimeRate(holidayRate)
                                                .weekendOvertimeRate(weekendRate)
                                                .nightStartTime(nightStart)
                                                .nightEndTime(nightEnd)
                                                .maxOvertimeHoursPerDay(maxPerDay)
                                                .maxOvertimeHoursPerMonth(maxPerMonth)
                                                .build()));
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

    // === Helper Generators ===

    private Arbitrary<LocalTime> localTimes() {
        return Arbitraries.integers().between(0, 23)
                .flatMap(hour -> Arbitraries.integers().between(0, 59)
                        .map(minute -> LocalTime.of(hour, minute)));
    }

    private Arbitrary<BigDecimal> positiveBigDecimals() {
        return Arbitraries.doubles().between(0.01, 10.0)
                .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private Arbitrary<List<AllowanceRule>> allowanceRuleLists() {
        return allowanceRules().list().ofMaxSize(5);
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

    private Arbitrary<List<DeductionRule>> deductionRuleLists() {
        return deductionRules().list().ofMaxSize(5);
    }

    private Arbitrary<DeductionRule> deductionRules() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.of(DeductionType.values()),
                positiveBigDecimals().injectNull(0.3),
                positiveBigDecimals().injectNull(0.3),
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
}
