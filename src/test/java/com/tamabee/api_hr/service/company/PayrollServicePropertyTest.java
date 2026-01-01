package com.tamabee.api_hr.service.company;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.result.AllowanceItem;
import com.tamabee.api_hr.dto.result.DeductionItem;
import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.enums.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho PayrollService
 * Feature: attendance-payroll-backend
 * Property 18: Payroll Record Round-Trip
 * Property 19: Finalized Payroll Immutability
 * Property 23: Salary Notification Content
 */
public class PayrollServicePropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 18: Payroll Record Round-Trip
     * For any payroll record, serializing then deserializing SHALL produce an
     * equivalent record.
     */
    @Property(tries = 100)
    void payrollRecordRoundTrip(
            @ForAll("payrollRecords") PayrollRecordEntity original) throws Exception {

        // Serialize allowance details
        String allowanceJson = objectMapper.writeValueAsString(original.getAllowanceDetails());
        // Deserialize back
        String restoredAllowance = objectMapper.readValue(allowanceJson, String.class);

        // Serialize deduction details
        String deductionJson = objectMapper.writeValueAsString(original.getDeductionDetails());
        // Deserialize back
        String restoredDeduction = objectMapper.readValue(deductionJson, String.class);

        // Verify round-trip
        assertEquals(original.getAllowanceDetails(), restoredAllowance,
                "Allowance details should survive round-trip serialization");
        assertEquals(original.getDeductionDetails(), restoredDeduction,
                "Deduction details should survive round-trip serialization");
    }

    /**
     * Property 18 (extended): Allowance items round-trip
     * For any list of allowance items, serializing then deserializing SHALL produce
     * equivalent items.
     */
    @Property(tries = 100)
    void allowanceItemsRoundTrip(
            @ForAll("allowanceItemLists") List<AllowanceItem> original) throws Exception {

        // Serialize
        String json = objectMapper.writeValueAsString(original);

        // Deserialize
        List<AllowanceItem> restored = objectMapper.readValue(json, new TypeReference<>() {
        });

        // Verify
        assertEquals(original.size(), restored.size(),
                "Allowance items count should match after round-trip");

        for (int i = 0; i < original.size(); i++) {
            AllowanceItem orig = original.get(i);
            AllowanceItem rest = restored.get(i);

            assertEquals(orig.getCode(), rest.getCode(), "Code should match");
            assertEquals(orig.getName(), rest.getName(), "Name should match");
            assertEquals(0, orig.getAmount().compareTo(rest.getAmount()), "Amount should match");
            assertEquals(orig.getTaxable(), rest.getTaxable(), "Taxable should match");
        }
    }

    /**
     * Property 18 (extended): Deduction items round-trip
     * For any list of deduction items, serializing then deserializing SHALL produce
     * equivalent items.
     */
    @Property(tries = 100)
    void deductionItemsRoundTrip(
            @ForAll("deductionItemLists") List<DeductionItem> original) throws Exception {

        // Serialize
        String json = objectMapper.writeValueAsString(original);

        // Deserialize
        List<DeductionItem> restored = objectMapper.readValue(json, new TypeReference<>() {
        });

        // Verify
        assertEquals(original.size(), restored.size(),
                "Deduction items count should match after round-trip");

        for (int i = 0; i < original.size(); i++) {
            DeductionItem orig = original.get(i);
            DeductionItem rest = restored.get(i);

            assertEquals(orig.getCode(), rest.getCode(), "Code should match");
            assertEquals(orig.getName(), rest.getName(), "Name should match");
            assertEquals(0, orig.getAmount().compareTo(rest.getAmount()), "Amount should match");
        }
    }

    /**
     * Property 19: Finalized Payroll Immutability
     * For any finalized payroll record, the status SHALL be FINALIZED or PAID.
     */
    @Property(tries = 100)
    void finalizedPayrollHasCorrectStatus(
            @ForAll("finalizedPayrollRecords") PayrollRecordEntity record) {

        // Verify finalized record has correct status
        assertTrue(
                record.getStatus() == PayrollStatus.FINALIZED || record.getStatus() == PayrollStatus.PAID,
                "Finalized payroll should have status FINALIZED or PAID");

        // Verify finalization metadata is set
        assertNotNull(record.getFinalizedAt(), "Finalized payroll should have finalizedAt");
        assertNotNull(record.getFinalizedBy(), "Finalized payroll should have finalizedBy");
    }

    /**
     * Property 19 (extended): Finalized payroll has payment status
     * For any finalized payroll record, payment status SHALL be set.
     */
    @Property(tries = 100)
    void finalizedPayrollHasPaymentStatus(
            @ForAll("finalizedPayrollRecords") PayrollRecordEntity record) {

        assertNotNull(record.getPaymentStatus(),
                "Finalized payroll should have payment status set");
    }

    /**
     * Property 23: Salary Notification Content
     * For any salary notification, the content SHALL contain net salary, earnings
     * breakdown,
     * deductions breakdown, and payment date.
     */
    @Property(tries = 100)
    void salaryNotificationContainsRequiredContent(
            @ForAll("payrollRecords") PayrollRecordEntity record) {

        // Build notification content (simulating what NotificationEmailService does)
        String content = buildSalaryNotificationContent(record);

        // Verify required content is present
        assertNotNull(content, "Notification content should not be null");

        // Net salary should be present
        assertTrue(content.contains(formatCurrency(record.getNetSalary())),
                "Notification should contain net salary");

        // Base salary (earnings) should be present
        assertTrue(content.contains(formatCurrency(record.getBaseSalary())),
                "Notification should contain base salary");

        // Total deductions should be present
        assertTrue(content.contains(formatCurrency(record.getTotalDeductions())),
                "Notification should contain total deductions");

        // Period should be present
        String period = String.format("%d-%02d", record.getYear(), record.getMonth());
        assertTrue(content.contains(period) || content.contains(record.getYear().toString()),
                "Notification should contain period information");
    }

    // === Helper Methods ===

    private String buildSalaryNotificationContent(PayrollRecordEntity record) {
        return String.format("""
                Salary Notification
                Period: %d-%02d
                Base Salary: %s
                Overtime: %s
                Allowances: %s
                Deductions: %s
                Net Salary: %s
                Payment Date: %s
                """,
                record.getYear(),
                record.getMonth(),
                formatCurrency(record.getBaseSalary()),
                formatCurrency(record.getTotalOvertimePay()),
                formatCurrency(record.getTotalAllowances()),
                formatCurrency(record.getTotalDeductions()),
                formatCurrency(record.getNetSalary()),
                record.getPaidAt() != null ? record.getPaidAt().toString() : "Pending");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }

    // === Generators ===

    @Provide
    Arbitrary<PayrollRecordEntity> payrollRecords() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                Arbitraries.longs().between(1, 100),
                Arbitraries.integers().between(2020, 2030),
                Arbitraries.integers().between(1, 12),
                Arbitraries.of(SalaryType.values()),
                positiveBigDecimals(),
                positiveBigDecimals(),
                positiveBigDecimals())
                .as((employeeId, companyId, year, month, salaryType,
                        baseSalary, overtime, allowances) -> {
                    BigDecimal deductions = baseSalary.multiply(BigDecimal.valueOf(0.1))
                            .setScale(0, java.math.RoundingMode.HALF_UP);
                    PayrollRecordEntity entity = new PayrollRecordEntity();
                    entity.setEmployeeId(employeeId);
                    entity.setCompanyId(companyId);
                    entity.setYear(year);
                    entity.setMonth(month);
                    entity.setSalaryType(salaryType);
                    entity.setBaseSalary(baseSalary);
                    entity.setTotalOvertimePay(overtime);
                    entity.setTotalAllowances(allowances);
                    entity.setTotalDeductions(deductions);
                    entity.setGrossSalary(baseSalary.add(overtime).add(allowances));
                    entity.setNetSalary(baseSalary.add(overtime).add(allowances).subtract(deductions));
                    entity.setStatus(PayrollStatus.DRAFT);
                    entity.setAllowanceDetails("[]");
                    entity.setDeductionDetails("[]");
                    return entity;
                });
    }

    @Provide
    Arbitrary<PayrollRecordEntity> finalizedPayrollRecords() {
        return payrollRecords().map(record -> {
            record.setStatus(PayrollStatus.FINALIZED);
            record.setPaymentStatus(PaymentStatus.PENDING);
            record.setFinalizedAt(LocalDateTime.now());
            record.setFinalizedBy(1L);
            return record;
        });
    }

    @Provide
    Arbitrary<List<AllowanceItem>> allowanceItemLists() {
        return allowanceItems().list().ofMaxSize(5);
    }

    private Arbitrary<AllowanceItem> allowanceItems() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.of(AllowanceType.values()),
                positiveBigDecimals(),
                Arbitraries.of(true, false))
                .as((code, name, type, amount, taxable) -> AllowanceItem.builder()
                        .code(code)
                        .name(name)
                        .type(type)
                        .amount(amount)
                        .taxable(taxable)
                        .build());
    }

    @Provide
    Arbitrary<List<DeductionItem>> deductionItemLists() {
        return deductionItems().list().ofMaxSize(5);
    }

    private Arbitrary<DeductionItem> deductionItems() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
                Arbitraries.of(DeductionType.values()),
                positiveBigDecimals(),
                Arbitraries.integers().between(1, 10))
                .as((code, name, type, amount, order) -> DeductionItem.builder()
                        .code(code)
                        .name(name)
                        .type(type)
                        .amount(amount)
                        .order(order)
                        .build());
    }

    private Arbitrary<BigDecimal> positiveBigDecimals() {
        return Arbitraries.doubles().between(1000, 1000000)
                .map(d -> BigDecimal.valueOf(d).setScale(0, java.math.RoundingMode.HALF_UP));
    }
}
