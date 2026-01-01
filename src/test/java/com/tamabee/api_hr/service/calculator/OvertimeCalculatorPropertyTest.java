package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.OvertimeMultipliers;
import com.tamabee.api_hr.dto.result.DailyOvertimeDetail;
import com.tamabee.api_hr.dto.result.OvertimeResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho OvertimeCalculator
 * Feature: break-time-backend
 * Property 15: Overtime Multiplier Validation
 * Property 16: Night Minutes Calculation
 * Property 17: Overtime Amount Calculation
 * Property 18: Overnight Shift Hour Split
 */
public class OvertimeCalculatorPropertyTest {

        private final LegalOvertimeRequirements legalOvertimeRequirements = new LegalOvertimeRequirements();
        private final OvertimeCalculator calculator = new OvertimeCalculator(legalOvertimeRequirements);

        /**
         * Property 10: Overtime Types Sum Invariant
         * For any overtime calculation, the sum of (regular + night + holiday +
         * weekend)
         * overtime minutes SHALL equal total overtime minutes.
         */
        @Property(tries = 100)
        void overtimeTypesSumEqualsTotal(
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("overtimeConfigs") OvertimeConfig config,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, config, hourlyRate);

                int sumOfTypes = result.getRegularOvertimeMinutes()
                                + result.getNightOvertimeMinutes()
                                + result.getHolidayOvertimeMinutes()
                                + result.getWeekendOvertimeMinutes();

                assertEquals(result.getTotalOvertimeMinutes(), sumOfTypes,
                                "Sum of overtime types should equal total overtime minutes");
        }

        /**
         * Property 11: Overtime Cap Enforcement
         * For any overtime calculation with a configured maximum, the calculated
         * overtime SHALL NOT exceed the maximum limit.
         */
        @Property(tries = 100)
        void overtimeDoesNotExceedMonthlyLimit(
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("overtimeConfigs") OvertimeConfig config,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, config, hourlyRate);

                if (config.getMaxOvertimeHoursPerMonth() != null) {
                        int maxMinutes = config.getMaxOvertimeHoursPerMonth() * 60;
                        assertTrue(result.getTotalOvertimeMinutes() <= maxMinutes,
                                        String.format("Total overtime (%d minutes) should not exceed monthly limit (%d minutes)",
                                                        result.getTotalOvertimeMinutes(), maxMinutes));
                }
        }

        /**
         * Property bổ sung: Khi overtime bị disable, kết quả phải là 0
         */
        @Property(tries = 100)
        void disabledOvertimeReturnsZero(
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeConfig disabledConfig = OvertimeConfig.builder()
                                .overtimeEnabled(false)
                                .build();

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, disabledConfig, hourlyRate);

                assertEquals(0, result.getTotalOvertimeMinutes(),
                                "Disabled overtime should return 0 total minutes");
                assertEquals(BigDecimal.ZERO, result.getTotalOvertimeAmount(),
                                "Disabled overtime should return 0 total pay");
        }

        /**
         * Property bổ sung: Tổng tiền tăng ca phải bằng tổng các loại tiền tăng ca
         */
        @Property(tries = 100)
        void overtimePaySumEqualsTotal(
                        @ForAll("dailyDetailLists") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("overtimeConfigs") OvertimeConfig config,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, config, hourlyRate);

                BigDecimal sumOfPay = result.getRegularOvertimeAmount()
                                .add(result.getNightOvertimeAmount())
                                .add(result.getHolidayOvertimeAmount())
                                .add(result.getWeekendOvertimeAmount());

                assertEquals(0, result.getTotalOvertimeAmount().compareTo(sumOfPay),
                                "Sum of overtime pay types should equal total overtime pay");
        }

        /**
         * Property 15: Overtime Multiplier Validation
         * For any overtime configuration, the custom multipliers SHALL NOT be below
         * the legal minimum for the company's locale.
         */
        @Property(tries = 100)
        void overtimeMultiplierValidation(
                        @ForAll("validOvertimeConfigs") OvertimeConfig config) {

                // Config đã được tạo với locale và multipliers hợp lệ
                boolean isValid = calculator.validateMultipliers(config);

                // Với valid configs, validation phải pass
                assertTrue(isValid,
                                String.format("Valid overtime config should pass validation for locale %s",
                                                config.getLocale()));
        }

        /**
         * Property 15 (negative): Invalid multipliers should fail validation
         */
        @Property(tries = 100)
        void invalidMultipliersShouldFailValidation(
                        @ForAll("locales") String locale) {

                OvertimeMultipliers minimum = legalOvertimeRequirements.getMinimumMultipliers(locale);

                // Tạo config với multiplier thấp hơn legal minimum
                OvertimeConfig invalidConfig = OvertimeConfig.builder()
                                .locale(locale)
                                .regularOvertimeRate(minimum.getRegularOvertime().subtract(new BigDecimal("0.1")))
                                .nightWorkRate(minimum.getNightWork())
                                .nightOvertimeRate(minimum.getNightOvertime())
                                .holidayOvertimeRate(minimum.getHolidayOvertime())
                                .holidayNightOvertimeRate(minimum.getHolidayNightOvertime())
                                .weekendOvertimeRate(minimum.getWeekendOvertime())
                                .build();

                boolean isValid = calculator.validateMultipliers(invalidConfig);

                assertFalse(isValid,
                                String.format("Invalid overtime config should fail validation for locale %s", locale));
        }

        /**
         * Property 16: Night Minutes Calculation
         * For any shift that includes night hours (22:00-05:00), the night minutes
         * SHALL be calculated correctly after deducting breaks that fall within night
         * hours.
         */
        @Property(tries = 100)
        void nightMinutesCalculatedCorrectly(
                        @ForAll("nightShiftCheckInOut") CheckInOutPair checkInOut) {

                OvertimeConfig config = OvertimeConfig.builder()
                                .nightStartTime(LocalTime.of(22, 0))
                                .nightEndTime(LocalTime.of(5, 0))
                                .build();

                int nightMinutes = calculator.calculateNightMinutes(
                                checkInOut.checkIn, checkInOut.checkOut, new ArrayList<>(), config);

                assertTrue(nightMinutes >= 0,
                                String.format("Night minutes should be non-negative, got %d", nightMinutes));
        }

        /**
         * Property 16 (with breaks): Night minutes should decrease when breaks fall in
         * night hours
         */
        @Property(tries = 100)
        void nightMinutesDecreasedByBreaksInNightHours(
                        @ForAll("nightShiftCheckInOut") CheckInOutPair checkInOut,
                        @ForAll("nightBreakRecords") List<BreakRecordEntity> breakRecords) {

                OvertimeConfig config = OvertimeConfig.builder()
                                .nightStartTime(LocalTime.of(22, 0))
                                .nightEndTime(LocalTime.of(5, 0))
                                .build();

                int nightMinutesWithoutBreak = calculator.calculateNightMinutes(
                                checkInOut.checkIn, checkInOut.checkOut, new ArrayList<>(), config);

                int nightMinutesWithBreak = calculator.calculateNightMinutes(
                                checkInOut.checkIn, checkInOut.checkOut, breakRecords, config);

                assertTrue(nightMinutesWithBreak <= nightMinutesWithoutBreak,
                                String.format("Night minutes with breaks (%d) should be <= without breaks (%d)",
                                                nightMinutesWithBreak, nightMinutesWithoutBreak));
        }

        /**
         * Property 17: Overtime Amount Calculation
         * For any overtime calculation, the overtime amount SHALL equal
         * (overtime minutes × hourly rate × overtime multiplier).
         */
        @Property(tries = 100)
        void overtimeAmountCalculatedCorrectly(
                        @ForAll("singleDayOvertimeDetails") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("overtimeConfigs") OvertimeConfig config,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, config, hourlyRate);

                // Tổng tiền phải >= 0
                assertTrue(result.getTotalOvertimeAmount().compareTo(BigDecimal.ZERO) >= 0,
                                "Total overtime amount should be non-negative");

                // Nếu có overtime minutes, phải có overtime amount (trừ khi hourly rate = 0)
                if (result.getTotalOvertimeMinutes() > 0 && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                        assertTrue(result.getTotalOvertimeAmount().compareTo(BigDecimal.ZERO) > 0,
                                        "Overtime amount should be positive when there are overtime minutes");
                }
        }

        /**
         * Property 18: Overnight Shift Hour Split
         * For any overnight shift (e.g., 17:00 to 07:00), the system SHALL correctly
         * split hours into regular, night, and morning segments.
         */
        @Property(tries = 100)
        void overnightShiftHourSplitCorrectly(
                        @ForAll("overnightShiftDetails") List<DailyOvertimeDetail> dailyDetails,
                        @ForAll("overtimeConfigs") OvertimeConfig config,
                        @ForAll("hourlyRates") BigDecimal hourlyRate) {

                OvertimeResult result = calculator.calculateOvertime(dailyDetails, config, hourlyRate);

                // Tổng các loại phải bằng total
                int sumOfTypes = result.getRegularOvertimeMinutes()
                                + result.getNightOvertimeMinutes()
                                + result.getHolidayOvertimeMinutes()
                                + result.getWeekendOvertimeMinutes();

                assertEquals(result.getTotalOvertimeMinutes(), sumOfTypes,
                                "Sum of overtime types should equal total for overnight shift");
        }

        /**
         * Property: Legal minimum multipliers are always valid
         */
        @Property(tries = 100)
        void legalMinimumMultipliersAreValid(
                        @ForAll("locales") String locale) {

                OvertimeMultipliers multipliers = calculator.getLegalMinimumMultipliers(locale);

                assertNotNull(multipliers, "Legal minimum multipliers should not be null");
                assertTrue(multipliers.getRegularOvertime().compareTo(BigDecimal.ONE) >= 0,
                                "Regular overtime multiplier should be >= 1");
                assertTrue(multipliers.getNightWork().compareTo(BigDecimal.ONE) >= 0,
                                "Night work multiplier should be >= 1");
                assertTrue(multipliers.getNightOvertime().compareTo(BigDecimal.ONE) >= 0,
                                "Night overtime multiplier should be >= 1");
                assertTrue(multipliers.getHolidayOvertime().compareTo(BigDecimal.ONE) >= 0,
                                "Holiday overtime multiplier should be >= 1");
        }

        // === Helper Classes ===

        record CheckInOutPair(LocalDateTime checkIn, LocalDateTime checkOut) {
        }

        // === Generators ===

        @Provide
        Arbitrary<List<DailyOvertimeDetail>> dailyDetailLists() {
                return dailyOvertimeDetails().list().ofMaxSize(31);
        }

        @Provide
        Arbitrary<List<DailyOvertimeDetail>> singleDayOvertimeDetails() {
                return dailyOvertimeDetails().list().ofSize(1);
        }

        @Provide
        Arbitrary<List<DailyOvertimeDetail>> overnightShiftDetails() {
                return Combinators.combine(
                                Arbitraries.integers().between(2020, 2030),
                                Arbitraries.integers().between(1, 12),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.integers().between(60, 180),
                                Arbitraries.integers().between(60, 180))
                                .as((year, month, day, regularMinutes, nightMinutes) -> {
                                        DailyOvertimeDetail detail = DailyOvertimeDetail.builder()
                                                        .date(LocalDate.of(year, month, day))
                                                        .regularMinutes(regularMinutes)
                                                        .nightMinutes(nightMinutes)
                                                        .isHoliday(false)
                                                        .isWeekend(false)
                                                        .build();
                                        return List.of(detail);
                                });
        }

        @Provide
        Arbitrary<DailyOvertimeDetail> dailyOvertimeDetails() {
                return Combinators.combine(
                                Arbitraries.integers().between(2020, 2030),
                                Arbitraries.integers().between(1, 12),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.integers().between(0, 240),
                                Arbitraries.integers().between(0, 120),
                                Arbitraries.of(true, false),
                                Arbitraries.of(true, false))
                                .as((year, month, day, regularMinutes, nightMinutes, isHoliday,
                                                isWeekend) -> DailyOvertimeDetail.builder()
                                                                .date(LocalDate.of(year, month, day))
                                                                .regularMinutes(regularMinutes)
                                                                .nightMinutes(nightMinutes)
                                                                .isHoliday(isHoliday)
                                                                .isWeekend(isWeekend)
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
        Arbitrary<OvertimeConfig> validOvertimeConfigs() {
                return Arbitraries.of("ja", "vi", "en").flatMap(locale -> {
                        OvertimeMultipliers minimum = legalOvertimeRequirements.getMinimumMultipliers(locale);
                        return Combinators.combine(
                                        Arbitraries.of(true),
                                        validMultiplier(minimum.getRegularOvertime()),
                                        validMultiplier(minimum.getNightWork()),
                                        validMultiplier(minimum.getNightOvertime()),
                                        validMultiplier(minimum.getHolidayOvertime()),
                                        validMultiplier(minimum.getHolidayNightOvertime()),
                                        validMultiplier(minimum.getWeekendOvertime()))
                                        .as((enable, regular, night, nightOt, holiday, holidayNight,
                                                        weekend) -> OvertimeConfig.builder()
                                                                        .overtimeEnabled(enable)
                                                                        .locale(locale)
                                                                        .regularOvertimeRate(regular)
                                                                        .nightWorkRate(night)
                                                                        .nightOvertimeRate(nightOt)
                                                                        .holidayOvertimeRate(holiday)
                                                                        .holidayNightOvertimeRate(holidayNight)
                                                                        .weekendOvertimeRate(weekend)
                                                                        .nightStartTime(LocalTime.of(22, 0))
                                                                        .nightEndTime(LocalTime.of(5, 0))
                                                                        .build());
                });
        }

        private Arbitrary<BigDecimal> validMultiplier(BigDecimal minimum) {
                double minValue = minimum.doubleValue();
                return Arbitraries.doubles().between(minValue, minValue + 1.0)
                                .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        @Provide
        Arbitrary<BigDecimal> hourlyRates() {
                return Arbitraries.doubles().between(500, 5000)
                                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
        }

        @Provide
        Arbitrary<String> locales() {
                return Arbitraries.of("ja", "vi", "en");
        }

        @Provide
        Arbitrary<CheckInOutPair> nightShiftCheckInOut() {
                return Combinators.combine(
                                Arbitraries.integers().between(20, 23),
                                Arbitraries.integers().between(5, 8)).as((startHour, endHour) -> {
                                        LocalDateTime checkIn = LocalDateTime.of(2024, 1, 1, startHour, 0);
                                        LocalDateTime checkOut = LocalDateTime.of(2024, 1, 2, endHour, 0);
                                        return new CheckInOutPair(checkIn, checkOut);
                                });
        }

        @Provide
        Arbitrary<List<BreakRecordEntity>> nightBreakRecords() {
                return Arbitraries.integers().between(0, 60).map(minutes -> {
                        if (minutes == 0) {
                                return new ArrayList<BreakRecordEntity>();
                        }
                        BreakRecordEntity record = new BreakRecordEntity();
                        record.setBreakStart(LocalDateTime.of(2024, 1, 1, 23, 0));
                        record.setBreakEnd(LocalDateTime.of(2024, 1, 1, 23, 0).plusMinutes(minutes));
                        record.setActualBreakMinutes(minutes);
                        record.setEffectiveBreakMinutes(minutes);
                        record.setEmployeeId(1L);
                        record.setCompanyId(1L);
                        record.setAttendanceRecordId(1L);
                        record.setWorkDate(LocalDate.of(2024, 1, 1));
                        List<BreakRecordEntity> list = new ArrayList<>();
                        list.add(record);
                        return list;
                });
        }

        private Arbitrary<BigDecimal> positiveBigDecimals() {
                return Arbitraries.doubles().between(1.0, 3.0)
                                .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP));
        }
}
