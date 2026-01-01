package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.RoundingConfig;
import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;
import net.jqwik.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho TimeRoundingCalculator
 * Feature: attendance-payroll-backend, Property 5: Time Rounding Determinism
 */
public class TimeRoundingCalculatorPropertyTest {

    private final TimeRoundingCalculator calculator = new TimeRoundingCalculator();

    /**
     * Property 5: Time Rounding Determinism
     * For any time value and rounding configuration, applying the rounding function
     * multiple times SHALL produce the same result (idempotent).
     */
    @Property(tries = 100)
    void timeRoundingIsIdempotent(
            @ForAll("localDateTimes") LocalDateTime time,
            @ForAll("roundingConfigs") RoundingConfig config) {

        // Áp dụng rounding lần đầu
        LocalDateTime firstRound = calculator.roundTime(time, config);

        // Áp dụng rounding lần thứ hai trên kết quả
        LocalDateTime secondRound = calculator.roundTime(firstRound, config);

        // Kết quả phải giống nhau (idempotent)
        assertEquals(firstRound, secondRound,
                "Rounding should be idempotent: applying twice should give same result");
    }

    /**
     * Property bổ sung: Kết quả làm tròn phải nằm trong khoảng hợp lệ
     * Thời gian sau khi làm tròn không được lệch quá interval so với thời gian gốc
     */
    @Property(tries = 100)
    void roundedTimeIsWithinInterval(
            @ForAll("localDateTimes") LocalDateTime time,
            @ForAll("roundingConfigs") RoundingConfig config) {

        LocalDateTime rounded = calculator.roundTime(time, config);
        int intervalMinutes = config.getInterval().getMinutes();

        // Tính khoảng cách giữa thời gian gốc và thời gian đã làm tròn (tính bằng phút)
        long diffMinutes = Math.abs(
                java.time.Duration.between(time.withSecond(0).withNano(0), rounded).toMinutes());

        // Khoảng cách không được vượt quá interval
        assertTrue(diffMinutes <= intervalMinutes,
                String.format("Rounded time should be within %d minutes of original. " +
                        "Original: %s, Rounded: %s, Diff: %d minutes",
                        intervalMinutes, time, rounded, diffMinutes));
    }

    /**
     * Property bổ sung: Phút của thời gian đã làm tròn phải chia hết cho interval
     */
    @Property(tries = 100)
    void roundedMinuteIsDivisibleByInterval(
            @ForAll("localDateTimes") LocalDateTime time,
            @ForAll("roundingConfigs") RoundingConfig config) {

        LocalDateTime rounded = calculator.roundTime(time, config);
        int intervalMinutes = config.getInterval().getMinutes();

        int roundedMinute = rounded.getMinute();
        assertEquals(0, roundedMinute % intervalMinutes,
                String.format("Rounded minute (%d) should be divisible by interval (%d)",
                        roundedMinute, intervalMinutes));
    }

    // === Generators ===

    @Provide
    Arbitrary<LocalDateTime> localDateTimes() {
        return Combinators.combine(
                Arbitraries.integers().between(2020, 2030),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1, 28),
                Arbitraries.integers().between(0, 23),
                Arbitraries.integers().between(0, 59),
                Arbitraries.integers().between(0, 59))
                .as((year, month, day, hour, minute, second) -> LocalDateTime.of(year, month, day, hour, minute,
                        second));
    }

    @Provide
    Arbitrary<RoundingConfig> roundingConfigs() {
        return Combinators.combine(
                Arbitraries.of(RoundingInterval.values()),
                Arbitraries.of(RoundingDirection.values()))
                .as((interval, direction) -> RoundingConfig.builder()
                        .interval(interval)
                        .direction(direction)
                        .build());
    }
}
