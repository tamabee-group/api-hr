package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.BreakType;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho BreakCalculator
 * Feature: break-time-backend
 */
public class BreakCalculatorPropertyTest {

    private final LegalBreakRequirements legalBreakRequirements = new LegalBreakRequirements();
    private final BreakCalculator calculator = new BreakCalculator(legalBreakRequirements);

    /**
     * Property 1: Break Duration Non-Negative
     * For any break record, the break duration SHALL be non-negative.
     */
    @Property(tries = 100)
    void breakDurationNonNegative(
            @ForAll("breakStartEndPairs") BreakStartEndPair pair) {

        int breakMinutes = calculator.calculateBreakMinutes(pair.start, pair.end);

        assertTrue(breakMinutes >= 0,
                String.format("Break duration should be non-negative, got %d", breakMinutes));
    }

    /**
     * Property 5: Working Hours Calculation with Unpaid Break
     * For any attendance record with unpaid break, the net working hours
     * SHALL equal (gross working hours - effective break duration).
     */
    @Property(tries = 100)
    void workingHoursWithUnpaidBreak(
            @ForAll("grossWorkingMinutes") int grossMinutes,
            @ForAll("breakMinutes") int breakMinutes) {

        BreakConfig config = BreakConfig.builder()
                .breakEnabled(true)
                .breakType(BreakType.UNPAID)
                .build();

        int netMinutes = calculator.calculateNetWorkingMinutes(grossMinutes, breakMinutes, config);
        int expected = Math.max(0, grossMinutes - breakMinutes);

        assertEquals(expected, netMinutes,
                String.format("Net working minutes should be %d, got %d", expected, netMinutes));
    }

    /**
     * Property 6: Working Hours Calculation with Paid Break
     * For any attendance record with paid break, the net working hours
     * SHALL equal gross working hours (no deduction).
     */
    @Property(tries = 100)
    void workingHoursWithPaidBreak(
            @ForAll("grossWorkingMinutes") int grossMinutes,
            @ForAll("breakMinutes") int breakMinutes) {

        BreakConfig config = BreakConfig.builder()
                .breakEnabled(true)
                .breakType(BreakType.PAID)
                .build();

        int netMinutes = calculator.calculateNetWorkingMinutes(grossMinutes, breakMinutes, config);

        assertEquals(grossMinutes, netMinutes,
                "Paid break should not deduct from working hours");
    }

    /**
     * Property 8: Effective Break Capping
     * For any break record where actual break exceeds maximum,
     * the effective break SHALL be capped at maximum.
     */
    @Property(tries = 100)
    void effectiveBreakCappedAtMaximum(
            @ForAll("actualBreakMinutes") int actualBreak,
            @ForAll("breakConfigs") BreakConfig config,
            @ForAll("workingHours") int workingHours,
            @ForAll boolean isNightShift) {

        int effectiveBreak = calculator.calculateEffectiveBreakMinutes(
                actualBreak, config, workingHours, isNightShift);

        int maximum = config.getMaximumBreakMinutes() != null
                ? config.getMaximumBreakMinutes()
                : Integer.MAX_VALUE;

        assertTrue(effectiveBreak <= maximum,
                String.format("Effective break (%d) should not exceed maximum (%d)",
                        effectiveBreak, maximum));
    }

    /**
     * Property 10: Total Break Minutes Invariant
     * For any attendance record with multiple break records,
     * the total break minutes SHALL equal the sum of individual break durations.
     */
    @Property(tries = 100)
    void totalBreakMinutesEqualsSum(
            @ForAll("breakRecordLists") List<BreakRecordEntity> breakRecords) {

        int totalBreak = calculator.calculateTotalBreakMinutes(breakRecords);

        int expectedSum = breakRecords.stream()
                .filter(r -> r != null && r.getActualBreakMinutes() != null)
                .mapToInt(BreakRecordEntity::getActualBreakMinutes)
                .sum();

        assertEquals(expectedSum, totalBreak,
                "Total break minutes should equal sum of individual breaks");
    }

    /**
     * Property 11: Night Shift Detection
     * For any work schedule where start time is after end time (e.g., 17:00 to
     * 07:00),
     * the system SHALL correctly identify it as an overnight shift.
     */
    @Property(tries = 100)
    void overnightShiftDetectedAsNightShift(
            @ForAll("overnightShiftTimes") ShiftTimePair shiftTimes) {

        BreakConfig config = BreakConfig.builder()
                .nightShiftStartTime(LocalTime.of(22, 0))
                .nightShiftEndTime(LocalTime.of(5, 0))
                .build();

        boolean isNightShift = calculator.isNightShift(
                shiftTimes.start, shiftTimes.end, config);

        assertTrue(isNightShift,
                String.format("Overnight shift %s to %s should be detected as night shift",
                        shiftTimes.start, shiftTimes.end));
    }

    /**
     * Property 12: Overnight Working Hours Calculation
     * For any overnight shift, the working hours SHALL be calculated correctly
     * across midnight.
     */
    @Property(tries = 100)
    void overnightWorkingHoursCalculatedCorrectly(
            @ForAll("overnightCheckInOut") CheckInOutPair checkInOut) {

        int workingMinutes = calculator.calculateWorkingMinutesForOvernightShift(
                checkInOut.checkIn, checkInOut.checkOut);

        // Tính expected duration
        long expectedMinutes = java.time.Duration.between(
                checkInOut.checkIn, checkInOut.checkOut).toMinutes();

        assertEquals((int) expectedMinutes, workingMinutes,
                String.format("Working minutes for %s to %s should be %d, got %d",
                        checkInOut.checkIn, checkInOut.checkOut, expectedMinutes, workingMinutes));
    }

    /**
     * Property 13: Night Shift Break Requirements
     * For any shift that falls within night hours (22:00-05:00),
     * the system SHALL apply night shift break requirements.
     */
    @Property(tries = 100)
    void nightShiftBreakRequirementsApplied(
            @ForAll("workingHoursOver6") int workingHours) {

        BreakConfig config = BreakConfig.builder()
                .breakEnabled(true)
                .useLegalMinimum(true)
                .locale("vi")
                .nightShiftMinimumBreakMinutes(45)
                .minimumBreakMinutes(30)
                .maximumBreakMinutes(90)
                .build();

        // Với Vietnamese law, night shift cần 45 phút break
        int effectiveBreak = calculator.calculateEffectiveBreakMinutes(
                0, config, workingHours, true);

        assertTrue(effectiveBreak >= 45,
                String.format("Night shift break should be at least 45 minutes, got %d", effectiveBreak));
    }

    /**
     * Property 14: Break Period Across Midnight
     * For any break period that spans midnight (e.g., 23:30 to 00:30),
     * the break duration SHALL be calculated correctly.
     */
    @Property(tries = 100)
    void breakPeriodAcrossMidnightCalculatedCorrectly(
            @ForAll("midnightBreakPairs") BreakStartEndPair pair) {

        int breakMinutes = calculator.calculateBreakMinutes(pair.start, pair.end);

        long expectedMinutes = java.time.Duration.between(pair.start, pair.end).toMinutes();

        assertEquals((int) expectedMinutes, breakMinutes,
                String.format("Break from %s to %s should be %d minutes, got %d",
                        pair.start, pair.end, expectedMinutes, breakMinutes));
    }

    // === Helper Classes ===

    record BreakStartEndPair(LocalDateTime start, LocalDateTime end) {
    }

    record ShiftTimePair(LocalTime start, LocalTime end) {
    }

    record CheckInOutPair(LocalDateTime checkIn, LocalDateTime checkOut) {
    }

    // === Generators ===

    @Provide
    Arbitrary<Integer> grossWorkingMinutes() {
        return Arbitraries.integers().between(60, 720); // 1-12 hours
    }

    @Provide
    Arbitrary<Integer> breakMinutes() {
        return Arbitraries.integers().between(0, 120); // 0-2 hours
    }

    @Provide
    Arbitrary<Integer> actualBreakMinutes() {
        return Arbitraries.integers().between(0, 180); // 0-3 hours
    }

    @Provide
    Arbitrary<Integer> workingHours() {
        return Arbitraries.integers().between(0, 16);
    }

    @Provide
    Arbitrary<Integer> workingHoursOver6() {
        return Arbitraries.integers().between(7, 16);
    }

    @Provide
    Arbitrary<BreakConfig> breakConfigs() {
        return Combinators.combine(
                Arbitraries.of(true),
                Arbitraries.of(BreakType.PAID, BreakType.UNPAID),
                Arbitraries.integers().between(15, 60),
                Arbitraries.integers().between(60, 180),
                Arbitraries.of(true, false),
                Arbitraries.of("ja", "vi", "en")).as((enabled, breakType, minBreak, maxBreak, useLegalMin, locale) -> {
                    int actualMin = Math.min(minBreak, maxBreak);
                    int actualMax = Math.max(minBreak, maxBreak);

                    return BreakConfig.builder()
                            .breakEnabled(enabled)
                            .breakType(breakType)
                            .minimumBreakMinutes(actualMin)
                            .maximumBreakMinutes(actualMax)
                            .defaultBreakMinutes((actualMin + actualMax) / 2)
                            .useLegalMinimum(useLegalMin)
                            .locale(locale)
                            .nightShiftStartTime(LocalTime.of(22, 0))
                            .nightShiftEndTime(LocalTime.of(5, 0))
                            .nightShiftMinimumBreakMinutes(45)
                            .build();
                });
    }

    @Provide
    Arbitrary<List<BreakRecordEntity>> breakRecordLists() {
        return breakRecords().list().ofMaxSize(5);
    }

    @Provide
    Arbitrary<BreakRecordEntity> breakRecords() {
        return Arbitraries.integers().between(0, 120)
                .map(minutes -> {
                    BreakRecordEntity record = new BreakRecordEntity();
                    record.setActualBreakMinutes(minutes);
                    record.setEffectiveBreakMinutes(minutes);
                    record.setEmployeeId(1L);
                    record.setCompanyId(1L);
                    record.setAttendanceRecordId(1L);
                    record.setWorkDate(LocalDate.now());
                    return record;
                });
    }

    @Provide
    Arbitrary<BreakStartEndPair> breakStartEndPairs() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 23),
                Arbitraries.integers().between(0, 59),
                Arbitraries.integers().between(1, 120)).as((hour, minute, durationMinutes) -> {
                    LocalDateTime start = LocalDateTime.of(2024, 1, 1, hour, minute);
                    LocalDateTime end = start.plusMinutes(durationMinutes);
                    return new BreakStartEndPair(start, end);
                });
    }

    @Provide
    Arbitrary<ShiftTimePair> overnightShiftTimes() {
        return Combinators.combine(
                Arbitraries.integers().between(17, 23),
                Arbitraries.integers().between(5, 10)).as(
                        (startHour, endHour) -> new ShiftTimePair(
                                LocalTime.of(startHour, 0),
                                LocalTime.of(endHour, 0)));
    }

    @Provide
    Arbitrary<CheckInOutPair> overnightCheckInOut() {
        return Combinators.combine(
                Arbitraries.integers().between(17, 23),
                Arbitraries.integers().between(5, 10)).as((startHour, endHour) -> {
                    LocalDateTime checkIn = LocalDateTime.of(2024, 1, 1, startHour, 0);
                    LocalDateTime checkOut = LocalDateTime.of(2024, 1, 2, endHour, 0);
                    return new CheckInOutPair(checkIn, checkOut);
                });
    }

    @Provide
    Arbitrary<BreakStartEndPair> midnightBreakPairs() {
        return Combinators.combine(
                Arbitraries.integers().between(23, 23),
                Arbitraries.integers().between(0, 59),
                Arbitraries.integers().between(30, 90)).as((hour, minute, durationMinutes) -> {
                    LocalDateTime start = LocalDateTime.of(2024, 1, 1, hour, minute);
                    LocalDateTime end = start.plusMinutes(durationMinutes);
                    return new BreakStartEndPair(start, end);
                });
    }
}
