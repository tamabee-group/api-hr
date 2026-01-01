package com.tamabee.api_hr.service.calculator;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho LegalBreakRequirements
 * Feature: break-time-backend
 * Property 4: Legal Minimum Compliance
 */
public class LegalBreakRequirementsPropertyTest {

    private final LegalBreakRequirements legalBreakRequirements = new LegalBreakRequirements();

    /**
     * Property 4: Legal Minimum Compliance
     * For any break configuration with useLegalMinimum=true, the effective minimum
     * break
     * SHALL be at least the legal minimum for the company's locale.
     */
    @Property(tries = 100)
    void legalMinimumBreakIsNonNegative(
            @ForAll("locales") String locale,
            @ForAll("workingHours") int workingHours,
            @ForAll boolean isNightShift) {

        int minimumBreak = legalBreakRequirements.getMinimumBreak(locale, workingHours, isNightShift);

        assertTrue(minimumBreak >= 0,
                String.format(
                        "Legal minimum break should be non-negative, got %d for locale=%s, hours=%d, nightShift=%s",
                        minimumBreak, locale, workingHours, isNightShift));
    }

    /**
     * Property: Japanese law - làm trên 6 giờ phải có break
     */
    @Property(tries = 100)
    void japaneseBreakRequiredForOver6Hours(
            @ForAll("workingHoursOver6") int workingHours) {

        int minimumBreak = legalBreakRequirements.getJapaneseMinimumBreak(workingHours);

        assertTrue(minimumBreak >= 45,
                String.format("Japanese law requires at least 45 min break for %d hours work, got %d",
                        workingHours, minimumBreak));
    }

    /**
     * Property: Japanese law - làm trên 8 giờ phải có 60 phút break
     */
    @Property(tries = 100)
    void japaneseBreak60MinutesForOver8Hours(
            @ForAll("workingHoursOver8") int workingHours) {

        int minimumBreak = legalBreakRequirements.getJapaneseMinimumBreak(workingHours);

        assertEquals(60, minimumBreak,
                String.format("Japanese law requires 60 min break for %d hours work", workingHours));
    }

    /**
     * Property: Japanese law - làm 6 giờ hoặc ít hơn không bắt buộc break
     */
    @Property(tries = 100)
    void japaneseNoBreakRequiredFor6HoursOrLess(
            @ForAll("workingHours6OrLess") int workingHours) {

        int minimumBreak = legalBreakRequirements.getJapaneseMinimumBreak(workingHours);

        assertEquals(0, minimumBreak,
                String.format("Japanese law does not require break for %d hours work", workingHours));
    }

    /**
     * Property: Vietnamese law - làm trên 6 giờ phải có break
     */
    @Property(tries = 100)
    void vietnameseBreakRequiredForOver6Hours(
            @ForAll("workingHoursOver6") int workingHours,
            @ForAll boolean isNightShift) {

        int minimumBreak = legalBreakRequirements.getVietnameseMinimumBreak(workingHours, isNightShift);

        assertTrue(minimumBreak >= 30,
                String.format("Vietnamese law requires at least 30 min break for %d hours work, got %d",
                        workingHours, minimumBreak));
    }

    /**
     * Property: Vietnamese law - ca đêm phải có 45 phút break
     */
    @Property(tries = 100)
    void vietnameseNightShiftBreak45Minutes(
            @ForAll("workingHoursOver6") int workingHours) {

        int minimumBreak = legalBreakRequirements.getVietnameseMinimumBreak(workingHours, true);

        assertEquals(45, minimumBreak,
                String.format("Vietnamese law requires 45 min break for night shift with %d hours work", workingHours));
    }

    /**
     * Property: Vietnamese law - làm 6 giờ hoặc ít hơn không bắt buộc break
     */
    @Property(tries = 100)
    void vietnameseNoBreakRequiredFor6HoursOrLess(
            @ForAll("workingHours6OrLess") int workingHours,
            @ForAll boolean isNightShift) {

        int minimumBreak = legalBreakRequirements.getVietnameseMinimumBreak(workingHours, isNightShift);

        assertEquals(0, minimumBreak,
                String.format("Vietnamese law does not require break for %d hours work", workingHours));
    }

    /**
     * Property: Default locale - làm trên 6 giờ phải có 30 phút break
     */
    @Property(tries = 100)
    void defaultBreakRequiredForOver6Hours(
            @ForAll("workingHoursOver6") int workingHours) {

        int minimumBreak = legalBreakRequirements.getDefaultMinimumBreak(workingHours);

        assertEquals(30, minimumBreak,
                String.format("Default requires 30 min break for %d hours work", workingHours));
    }

    /**
     * Property: Null locale falls back to default
     */
    @Property(tries = 100)
    void nullLocaleFallsBackToDefault(
            @ForAll("workingHours") int workingHours,
            @ForAll boolean isNightShift) {

        int minimumBreak = legalBreakRequirements.getMinimumBreak(null, workingHours, isNightShift);
        int defaultBreak = legalBreakRequirements.getDefaultMinimumBreak(workingHours);

        assertEquals(defaultBreak, minimumBreak,
                "Null locale should fall back to default break requirements");
    }

    /**
     * Property: Unknown locale falls back to default
     */
    @Property(tries = 100)
    void unknownLocaleFallsBackToDefault(
            @ForAll("unknownLocales") String locale,
            @ForAll("workingHours") int workingHours,
            @ForAll boolean isNightShift) {

        int minimumBreak = legalBreakRequirements.getMinimumBreak(locale, workingHours, isNightShift);
        int defaultBreak = legalBreakRequirements.getDefaultMinimumBreak(workingHours);

        assertEquals(defaultBreak, minimumBreak,
                String.format("Unknown locale '%s' should fall back to default break requirements", locale));
    }

    // === Generators ===

    @Provide
    Arbitrary<String> locales() {
        return Arbitraries.of("ja", "vi", "en", "JA", "VI", "EN");
    }

    @Provide
    Arbitrary<String> unknownLocales() {
        return Arbitraries.of("fr", "de", "es", "kr", "cn", "unknown", "xyz");
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
    Arbitrary<Integer> workingHoursOver8() {
        return Arbitraries.integers().between(9, 16);
    }

    @Provide
    Arbitrary<Integer> workingHours6OrLess() {
        return Arbitraries.integers().between(0, 6);
    }
}
