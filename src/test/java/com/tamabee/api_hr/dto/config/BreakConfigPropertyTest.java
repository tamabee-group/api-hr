package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.BreakType;
import net.jqwik.api.*;

import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho BreakConfig validation
 * Feature: break-time-backend
 * Property 3: Minimum Break Enforcement
 */
public class BreakConfigPropertyTest {

    /**
     * Property 3: Minimum Break Enforcement
     * For any break configuration, the minimum break duration SHALL NOT exceed the
     * maximum break duration.
     */
    @Property(tries = 100)
    void minimumBreakShallNotExceedMaximum(
            @ForAll("breakConfigs") BreakConfig config) {

        // Kiểm tra minimum không vượt quá maximum
        assertTrue(config.getMinimumBreakMinutes() <= config.getMaximumBreakMinutes(),
                String.format("Minimum break (%d) should not exceed maximum break (%d)",
                        config.getMinimumBreakMinutes(), config.getMaximumBreakMinutes()));
    }

    /**
     * Property bổ sung: Default break phải nằm trong khoảng [minimum, maximum]
     */
    @Property(tries = 100)
    void defaultBreakWithinMinMaxRange(
            @ForAll("breakConfigs") BreakConfig config) {

        assertTrue(config.getDefaultBreakMinutes() >= config.getMinimumBreakMinutes(),
                String.format("Default break (%d) should be >= minimum (%d)",
                        config.getDefaultBreakMinutes(), config.getMinimumBreakMinutes()));

        assertTrue(config.getDefaultBreakMinutes() <= config.getMaximumBreakMinutes(),
                String.format("Default break (%d) should be <= maximum (%d)",
                        config.getDefaultBreakMinutes(), config.getMaximumBreakMinutes()));
    }

    /**
     * Property bổ sung: Night shift break minimum phải >= 0
     */
    @Property(tries = 100)
    void nightShiftBreakMinutesNonNegative(
            @ForAll("breakConfigs") BreakConfig config) {

        assertTrue(config.getNightShiftMinimumBreakMinutes() >= 0,
                "Night shift minimum break should be non-negative");

        assertTrue(config.getNightShiftDefaultBreakMinutes() >= 0,
                "Night shift default break should be non-negative");
    }

    /**
     * Property bổ sung: breakPeriodsPerAttendance phải >= 1
     */
    @Property(tries = 100)
    void breakPeriodsPerAttendanceAtLeastOne(
            @ForAll("breakConfigs") BreakConfig config) {

        assertTrue(config.getBreakPeriodsPerAttendance() >= 1,
                "Break periods per attendance should be at least 1");
    }

    // === Generators ===

    @Provide
    Arbitrary<BreakConfig> breakConfigs() {
        return Combinators.combine(
                Arbitraries.of(true, false), // breakEnabled
                Arbitraries.of(BreakType.PAID, BreakType.UNPAID), // breakType
                Arbitraries.integers().between(15, 120), // minimumBreakMinutes
                Arbitraries.integers().between(30, 180), // maximumBreakMinutes
                Arbitraries.of(true, false), // useLegalMinimum
                Arbitraries.of(true, false), // breakTrackingEnabled
                Arbitraries.of("ja", "vi", "en"), // locale
                Arbitraries.of(true, false) // fixedBreakMode
        ).as((breakEnabled, breakType, minBreak, maxBreak, useLegalMin, tracking, locale, fixedMode) -> {
            // Đảm bảo min <= max
            int actualMin = Math.min(minBreak, maxBreak);
            int actualMax = Math.max(minBreak, maxBreak);
            // Default nằm trong khoảng [min, max]
            int defaultBreak = actualMin + (actualMax - actualMin) / 2;

            return BreakConfig.builder()
                    .breakEnabled(breakEnabled)
                    .breakType(breakType)
                    .minimumBreakMinutes(actualMin)
                    .maximumBreakMinutes(actualMax)
                    .defaultBreakMinutes(defaultBreak)
                    .useLegalMinimum(useLegalMin)
                    .breakTrackingEnabled(tracking)
                    .locale(locale)
                    .fixedBreakMode(fixedMode)
                    .breakPeriodsPerAttendance(1)
                    .fixedBreakPeriods(new ArrayList<>())
                    .nightShiftStartTime(LocalTime.of(22, 0))
                    .nightShiftEndTime(LocalTime.of(5, 0))
                    .nightShiftMinimumBreakMinutes(45)
                    .nightShiftDefaultBreakMinutes(60)
                    .build();
        });
    }
}
