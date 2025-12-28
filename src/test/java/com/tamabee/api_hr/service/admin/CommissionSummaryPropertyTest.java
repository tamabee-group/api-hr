package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.CommissionSummaryResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.repository.*;
import com.tamabee.api_hr.service.admin.impl.EmployeeReferralServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Commission Summary
 * Property 9: Commission summary tính đúng
 */
@Tag("Feature: tamabee-role-redesign")
public class CommissionSummaryPropertyTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private EmployeeCommissionRepository commissionRepository;

    @Mock
    private PlanRepository planRepository;

    private EmployeeReferralServiceImpl employeeReferralService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        employeeReferralService = new EmployeeReferralServiceImpl(
                companyRepository,
                userRepository,
                walletRepository,
                walletTransactionRepository,
                commissionRepository,
                planRepository);
    }

    /**
     * Property 9: Commission summary tính đúng
     * For any Employee Tamabee user, commission summary SHALL có:
     * - totalPendingAmount = sum of all PENDING commissions
     * - totalEligibleAmount = sum of all ELIGIBLE commissions
     * - totalPaidAmount = sum of all PAID commissions
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 9: Commission summary tính đúng")
    void commissionSummaryShouldCalculateCorrectly(
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmployeeIds") Long employeeId,
            @ForAll("validAmounts") BigDecimal pendingAmount,
            @ForAll("validAmounts") BigDecimal eligibleAmount,
            @ForAll("validAmounts") BigDecimal paidAmount,
            @ForAll("validCounts") long pendingCount,
            @ForAll("validCounts") long eligibleCount,
            @ForAll("validCounts") long paidCount,
            @ForAll("validReferralCounts") int totalReferrals) {

        // Chuẩn bị employee
        UserEntity employee = createEmployee(employeeId, employeeCode);

        // Mock repositories
        when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(employee));

        // Mock referral count
        when(companyRepository.countByReferredByEmployeeId(employeeId))
                .thenReturn(totalReferrals);

        // Mock commission counts
        long totalCommissions = pendingCount + eligibleCount + paidCount;
        when(commissionRepository.countByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(totalCommissions);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PENDING))
                .thenReturn(pendingCount);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(eligibleCount);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PAID))
                .thenReturn(paidCount);

        // Mock commission amounts
        BigDecimal totalAmount = pendingAmount.add(eligibleAmount).add(paidAmount);
        when(commissionRepository.sumAmountByEmployeeCode(employeeCode))
                .thenReturn(totalAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PENDING))
                .thenReturn(pendingAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(eligibleAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PAID))
                .thenReturn(paidAmount);

        // Thực thi
        CommissionSummaryResponse summary = employeeReferralService.getCommissionSummary(employeeCode);

        // Kiểm tra: summary phải tính đúng
        assertNotNull(summary, "Summary không được null");

        // Kiểm tra employee info
        assertEquals(employeeCode, summary.getEmployeeCode(), "employeeCode phải khớp");

        // Kiểm tra total referrals
        assertEquals(totalReferrals, summary.getTotalReferrals(), "totalReferrals phải khớp");

        // Kiểm tra total commissions
        assertEquals(totalCommissions, summary.getTotalCommissions(), "totalCommissions phải khớp");

        // Kiểm tra total amount
        assertEquals(totalAmount, summary.getTotalAmount(), "totalAmount phải khớp");

        // Kiểm tra pending
        assertEquals(pendingCount, summary.getPendingCommissions(), "pendingCommissions phải khớp");
        assertEquals(pendingAmount, summary.getPendingAmount(), "pendingAmount phải khớp");

        // Kiểm tra eligible
        assertEquals(eligibleCount, summary.getEligibleCommissions(), "eligibleCommissions phải khớp");
        assertEquals(eligibleAmount, summary.getEligibleAmount(), "eligibleAmount phải khớp");

        // Kiểm tra paid
        assertEquals(paidCount, summary.getPaidCommissions(), "paidCommissions phải khớp");
        assertEquals(paidAmount, summary.getPaidAmount(), "paidAmount phải khớp");
    }

    /**
     * Property 9: Commission summary với employee không có commission
     * Khi employee không có commission, tất cả amounts phải là 0
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 9: Commission summary tính đúng")
    void commissionSummaryShouldBeZeroWhenNoCommissions(
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmployeeIds") Long employeeId) {

        // Chuẩn bị employee
        UserEntity employee = createEmployee(employeeId, employeeCode);

        // Mock repositories - không có commission
        when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(employee));

        when(companyRepository.countByReferredByEmployeeId(employeeId))
                .thenReturn(0);

        when(commissionRepository.countByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(0L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PENDING))
                .thenReturn(0L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(0L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PAID))
                .thenReturn(0L);

        when(commissionRepository.sumAmountByEmployeeCode(employeeCode))
                .thenReturn(BigDecimal.ZERO);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PENDING))
                .thenReturn(BigDecimal.ZERO);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(BigDecimal.ZERO);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PAID))
                .thenReturn(BigDecimal.ZERO);

        // Thực thi
        CommissionSummaryResponse summary = employeeReferralService.getCommissionSummary(employeeCode);

        // Kiểm tra: tất cả amounts phải là 0
        assertNotNull(summary, "Summary không được null");
        assertEquals(0, summary.getTotalReferrals(), "totalReferrals phải là 0");
        assertEquals(0L, summary.getTotalCommissions(), "totalCommissions phải là 0");
        assertEquals(BigDecimal.ZERO, summary.getTotalAmount(), "totalAmount phải là 0");
        assertEquals(0L, summary.getPendingCommissions(), "pendingCommissions phải là 0");
        assertEquals(BigDecimal.ZERO, summary.getPendingAmount(), "pendingAmount phải là 0");
        assertEquals(0L, summary.getEligibleCommissions(), "eligibleCommissions phải là 0");
        assertEquals(BigDecimal.ZERO, summary.getEligibleAmount(), "eligibleAmount phải là 0");
        assertEquals(0L, summary.getPaidCommissions(), "paidCommissions phải là 0");
        assertEquals(BigDecimal.ZERO, summary.getPaidAmount(), "paidAmount phải là 0");
    }

    /**
     * Property 9: Tổng các amounts phải bằng totalAmount
     * pendingAmount + eligibleAmount + paidAmount = totalAmount
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 9: Commission summary tính đúng")
    void sumOfAmountsShouldEqualTotalAmount(
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmployeeIds") Long employeeId,
            @ForAll("validAmounts") BigDecimal pendingAmount,
            @ForAll("validAmounts") BigDecimal eligibleAmount,
            @ForAll("validAmounts") BigDecimal paidAmount) {

        // Chuẩn bị employee
        UserEntity employee = createEmployee(employeeId, employeeCode);

        BigDecimal totalAmount = pendingAmount.add(eligibleAmount).add(paidAmount);

        // Mock repositories
        when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(employee));

        when(companyRepository.countByReferredByEmployeeId(employeeId))
                .thenReturn(5);

        when(commissionRepository.countByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(10L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PENDING))
                .thenReturn(3L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(4L);

        when(commissionRepository.countByEmployeeCodeAndStatusAndDeletedFalse(employeeCode, CommissionStatus.PAID))
                .thenReturn(3L);

        when(commissionRepository.sumAmountByEmployeeCode(employeeCode))
                .thenReturn(totalAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PENDING))
                .thenReturn(pendingAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.ELIGIBLE))
                .thenReturn(eligibleAmount);

        when(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PAID))
                .thenReturn(paidAmount);

        // Thực thi
        CommissionSummaryResponse summary = employeeReferralService.getCommissionSummary(employeeCode);

        // Kiểm tra: tổng các amounts phải bằng totalAmount
        BigDecimal sumOfAmounts = summary.getPendingAmount()
                .add(summary.getEligibleAmount())
                .add(summary.getPaidAmount());

        assertEquals(summary.getTotalAmount(), sumOfAmounts,
                "Tổng pendingAmount + eligibleAmount + paidAmount phải bằng totalAmount");
    }

    // === Generators ===

    @Provide
    Arbitrary<String> validEmployeeCodes() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofLength(6)
                .map(s -> "EMP" + s.substring(0, 3));
    }

    @Provide
    Arbitrary<Long> validEmployeeIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, BigDecimal.valueOf(1000000))
                .ofScale(0);
    }

    @Provide
    Arbitrary<Long> validCounts() {
        return Arbitraries.longs().between(0L, 100L);
    }

    @Provide
    Arbitrary<Integer> validReferralCounts() {
        return Arbitraries.integers().between(0, 50);
    }

    // === Helper methods ===

    private UserEntity createEmployee(Long id, String employeeCode) {
        UserEntity employee = new UserEntity();
        employee.setId(id);
        employee.setEmployeeCode(employeeCode);
        employee.setEmail(employeeCode.toLowerCase() + "@tamabee.com");
        employee.setRole(UserRole.EMPLOYEE_TAMABEE);
        employee.setCompanyId(0L);
        return employee;
    }
}
