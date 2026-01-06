package com.tamabee.api_hr.util;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests cho TenantDomainValidator.
 * 
 * Property 1: Tenant Domain Validation
 * - For any input string, validation SHALL return true only if:
 * + Contains only lowercase letters, numbers, and hyphens
 * + Has length between 3-30 characters
 * + Does not start or end with a hyphen
 * 
 * Property 2: Reserved Domain Rejection
 * - For any reserved domain (admin, api, www, app, mail, tamabee),
 * the domain availability check SHALL return false
 */
class TenantDomainValidatorPropertyTest {

    // ==================== Property 1: Tenant Domain Validation
    // ====================

    /**
     * Property 1.1: Valid domains phải pass validation
     * For any string matching valid pattern, isValidFormat SHALL return true
     */
    @Property(tries = 100)
    void validDomain_shouldPassValidation(
            @ForAll("validTenantDomains") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Valid domain '%s' should pass validation", domain)
                .isTrue();
    }

    /**
     * Property 1.2: Domains với uppercase letters phải fail validation
     * For any string containing uppercase letters, isValidFormat SHALL return false
     */
    @Property(tries = 100)
    void domainWithUppercase_shouldFailValidation(
            @ForAll("domainsWithUppercase") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Domain with uppercase '%s' should fail validation", domain)
                .isFalse();
    }

    /**
     * Property 1.3: Domains bắt đầu bằng hyphen phải fail validation
     * For any string starting with hyphen, isValidFormat SHALL return false
     */
    @Property(tries = 100)
    void domainStartingWithHyphen_shouldFailValidation(
            @ForAll("domainsStartingWithHyphen") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Domain starting with hyphen '%s' should fail validation", domain)
                .isFalse();
    }

    /**
     * Property 1.4: Domains kết thúc bằng hyphen phải fail validation
     * For any string ending with hyphen, isValidFormat SHALL return false
     */
    @Property(tries = 100)
    void domainEndingWithHyphen_shouldFailValidation(
            @ForAll("domainsEndingWithHyphen") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Domain ending with hyphen '%s' should fail validation", domain)
                .isFalse();
    }

    /**
     * Property 1.5: Domains quá ngắn (< 3 chars) phải fail validation
     * For any string with length < 3, isValidFormat SHALL return false
     */
    @Property(tries = 100)
    void domainTooShort_shouldFailValidation(
            @ForAll("shortDomains") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Short domain '%s' (length=%d) should fail validation", domain, domain.length())
                .isFalse();
    }

    /**
     * Property 1.6: Domains quá dài (> 30 chars) phải fail validation
     * For any string with length > 30, isValidFormat SHALL return false
     */
    @Property(tries = 100)
    void domainTooLong_shouldFailValidation(
            @ForAll("longDomains") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Long domain '%s' (length=%d) should fail validation", domain, domain.length())
                .isFalse();
    }

    /**
     * Property 1.7: Domains với special characters phải fail validation
     * For any string containing special characters, isValidFormat SHALL return
     * false
     */
    @Property(tries = 100)
    void domainWithSpecialChars_shouldFailValidation(
            @ForAll("domainsWithSpecialChars") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Domain with special chars '%s' should fail validation", domain)
                .isFalse();
    }

    /**
     * Property 1.8: Null và empty domains phải fail validation
     * For null or empty string, isValidFormat SHALL return false
     */
    @Property(tries = 10)
    void nullOrEmptyDomain_shouldFailValidation(
            @ForAll("nullOrEmptyDomains") String domain) {

        boolean result = TenantDomainValidator.isValidFormat(domain);

        assertThat(result)
                .as("Null or empty domain should fail validation")
                .isFalse();
    }

    // ==================== Property 2: Reserved Domain Rejection
    // ====================

    /**
     * Property 2.1: Reserved domains phải bị reject
     * For any reserved domain, isReserved SHALL return true
     */
    @Property(tries = 100)
    void reservedDomain_shouldBeRejected(
            @ForAll("reservedDomains") String domain) {

        boolean result = TenantDomainValidator.isReserved(domain);

        assertThat(result)
                .as("Reserved domain '%s' should be rejected", domain)
                .isTrue();
    }

    /**
     * Property 2.2: Non-reserved valid domains không bị reject
     * For any non-reserved valid domain, isReserved SHALL return false
     */
    @Property(tries = 100)
    void nonReservedDomain_shouldNotBeRejected(
            @ForAll("nonReservedValidDomains") String domain) {

        boolean result = TenantDomainValidator.isReserved(domain);

        assertThat(result)
                .as("Non-reserved domain '%s' should not be rejected", domain)
                .isFalse();
    }

    /**
     * Property 2.3: Full validation cho reserved domains phải fail
     * For any reserved domain, validate SHALL return invalid with RESERVED error
     */
    @Property(tries = 100)
    void reservedDomain_fullValidation_shouldReturnReservedError(
            @ForAll("reservedDomains") String domain) {

        TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(domain);

        assertThat(result.isValid())
                .as("Reserved domain '%s' should fail full validation", domain)
                .isFalse();
        assertThat(result.getError())
                .as("Reserved domain '%s' should have RESERVED error", domain)
                .isEqualTo(TenantDomainValidator.ValidationError.RESERVED);
    }

    /**
     * Property 2.4: Full validation cho valid non-reserved domains phải pass
     * For any valid non-reserved domain, validate SHALL return valid
     */
    @Property(tries = 100)
    void validNonReservedDomain_fullValidation_shouldPass(
            @ForAll("nonReservedValidDomains") String domain) {

        TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(domain);

        assertThat(result.isValid())
                .as("Valid non-reserved domain '%s' should pass full validation", domain)
                .isTrue();
        assertThat(result.getError())
                .as("Valid domain '%s' should have no error", domain)
                .isNull();
    }

    /**
     * Property 2.5: Full validation cho invalid format domains phải fail với
     * INVALID_FORMAT error
     * For any invalid format domain, validate SHALL return invalid with
     * INVALID_FORMAT error
     */
    @Property(tries = 100)
    void invalidFormatDomain_fullValidation_shouldReturnInvalidFormatError(
            @ForAll("invalidFormatDomains") String domain) {

        TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(domain);

        assertThat(result.isValid())
                .as("Invalid format domain '%s' should fail full validation", domain)
                .isFalse();
        assertThat(result.getError())
                .as("Invalid format domain '%s' should have INVALID_FORMAT error", domain)
                .isEqualTo(TenantDomainValidator.ValidationError.INVALID_FORMAT);
    }

    // ==================== Providers ====================

    /**
     * Provider cho valid tenant domains (lowercase, numbers, hyphens, 3-30 chars)
     * Không bắt đầu hoặc kết thúc bằng hyphen
     */
    @Provide
    Arbitrary<String> validTenantDomains() {
        // Tạo domain với format: [a-z0-9][a-z0-9-]*[a-z0-9] hoặc [a-z0-9]{3}
        Arbitrary<String> startChar = Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                        '4', '5', '6', '7', '8', '9')
                .ofLength(1);

        Arbitrary<String> middlePart = Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                        '4', '5', '6', '7', '8', '9', '-')
                .ofMinLength(1)
                .ofMaxLength(28);

        Arbitrary<String> endChar = Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                        '4', '5', '6', '7', '8', '9')
                .ofLength(1);

        return Combinators.combine(startChar, middlePart, endChar)
                .as((start, middle, end) -> start + middle + end)
                .filter(s -> s.length() >= 3 && s.length() <= 30)
                .filter(s -> !TenantDomainValidator.isReserved(s));
    }

    /**
     * Provider cho domains với uppercase letters
     */
    @Provide
    Arbitrary<String> domainsWithUppercase() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'A', 'B', 'C', '0', '1', '2', '-')
                .ofMinLength(3)
                .ofMaxLength(30)
                .filter(s -> !s.startsWith("-") && !s.endsWith("-"))
                .filter(s -> s.chars().anyMatch(Character::isUpperCase));
    }

    /**
     * Provider cho domains bắt đầu bằng hyphen
     */
    @Provide
    Arbitrary<String> domainsStartingWithHyphen() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', '0', '1', '2', '-')
                .ofMinLength(2)
                .ofMaxLength(29)
                .map(s -> "-" + s)
                .filter(s -> s.length() >= 3 && s.length() <= 30);
    }

    /**
     * Provider cho domains kết thúc bằng hyphen
     */
    @Provide
    Arbitrary<String> domainsEndingWithHyphen() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', '0', '1', '2', '-')
                .ofMinLength(2)
                .ofMaxLength(29)
                .map(s -> s + "-")
                .filter(s -> s.length() >= 3 && s.length() <= 30);
    }

    /**
     * Provider cho domains quá ngắn (< 3 chars)
     */
    @Provide
    Arbitrary<String> shortDomains() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', '0', '1', '2')
                .ofMinLength(1)
                .ofMaxLength(2);
    }

    /**
     * Provider cho domains quá dài (> 30 chars)
     */
    @Provide
    Arbitrary<String> longDomains() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', 'd', 'e', '0', '1', '2')
                .ofMinLength(31)
                .ofMaxLength(50);
    }

    /**
     * Provider cho domains với special characters
     */
    @Provide
    Arbitrary<String> domainsWithSpecialChars() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', '!', '@', '#', '$', '%', '^', '&', '*', '.', '_', ' ')
                .ofMinLength(3)
                .ofMaxLength(30)
                .filter(s -> s.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && c != '-'));
    }

    /**
     * Provider cho null hoặc empty domains
     */
    @Provide
    Arbitrary<String> nullOrEmptyDomains() {
        return Arbitraries.of(null, "", "  ");
    }

    /**
     * Provider cho reserved domains
     */
    @Provide
    Arbitrary<String> reservedDomains() {
        return Arbitraries.of("admin", "api", "www", "app", "mail", "tamabee");
    }

    /**
     * Provider cho non-reserved valid domains
     */
    @Provide
    Arbitrary<String> nonReservedValidDomains() {
        return Arbitraries.of(
                "acme", "company1", "my-company", "test123", "abc-def-ghi",
                "company-name", "tenant1", "org123", "myapp", "demo");
    }

    /**
     * Provider cho invalid format domains (không phải reserved)
     */
    @Provide
    Arbitrary<String> invalidFormatDomains() {
        return Arbitraries.of(
                "-invalid", "invalid-", "IN-VALID", "ab", "a",
                "has space", "has.dot", "has_underscore", "has@symbol");
    }
}
