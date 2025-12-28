package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.EmployeeCommissionRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.mapper.admin.EmployeeCommissionMapper;
import com.tamabee.api_hr.service.admin.impl.CommissionServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Commission Eligibility
 * Property 4: Commission eligibility dựa trên billing
 */
@Tag("Feature: tamabee-role-redesign")
public class CommissionEligibilityPropertyTest {

        @Mock
        private EmployeeCommissionRepository commissionRepository;

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private WalletRepository walletRepository;

        @Mock
        private ISettingService settingService;

        @Mock
        private EmployeeCommissionMapper commissionMapper;

        private CommissionServiceImpl commissionService;

        @BeforeTry
        void setUp() {
                MockitoAnnotations.openMocks(this);
                commissionService = new CommissionServiceImpl(
                                commissionRepository,
                                companyRepository,
                                userRepository,
                                walletRepository,
                                settingService,
                                commissionMapper);
        }

        /**
         * Property 4: Commission eligibility dựa trên billing
         * For any commission record và company, commission status SHALL là ELIGIBLE
         * khi và chỉ khi company's total_billing > commission amount.
         * Ngược lại, status SHALL là PENDING.
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 4: Commission eligibility dựa trên billing")
        void commissionShouldBeEligibleWhenBillingGreaterThanAmount(
                        @ForAll("validCommissionIds") Long commissionId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("billingGreaterThanCommission") BigDecimal[] billingAndCommission) {

                BigDecimal totalBilling = billingAndCommission[0];
                BigDecimal commissionAmount = billingAndCommission[1];

                // Chuẩn bị dữ liệu
                EmployeeCommissionEntity commission = createCommission(commissionId, companyId, commissionAmount);
                WalletEntity wallet = createWallet(companyId, totalBilling);

                when(commissionRepository.findByIdAndDeletedFalse(commissionId))
                                .thenReturn(Optional.of(commission));
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(commissionRepository.save(any(EmployeeCommissionEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Thực thi
                boolean isEligible = commissionService.calculateEligibility(commissionId);

                // Kiểm tra: billing > commission → ELIGIBLE
                assertTrue(isEligible, "Commission phải eligible khi billing > commission amount");
                assertEquals(CommissionStatus.ELIGIBLE, commission.getStatus(),
                                "Status phải là ELIGIBLE khi billing > commission");
        }

        /**
         * Property 4: Commission eligibility dựa trên billing
         * Khi billing <= commission amount, status phải là PENDING
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 4: Commission eligibility dựa trên billing")
        void commissionShouldBePendingWhenBillingLessOrEqualAmount(
                        @ForAll("validCommissionIds") Long commissionId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("billingLessOrEqualCommission") BigDecimal[] billingAndCommission) {

                BigDecimal totalBilling = billingAndCommission[0];
                BigDecimal commissionAmount = billingAndCommission[1];

                // Chuẩn bị dữ liệu
                EmployeeCommissionEntity commission = createCommission(commissionId, companyId, commissionAmount);
                WalletEntity wallet = createWallet(companyId, totalBilling);

                when(commissionRepository.findByIdAndDeletedFalse(commissionId))
                                .thenReturn(Optional.of(commission));
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                // Thực thi
                boolean isEligible = commissionService.calculateEligibility(commissionId);

                // Kiểm tra: billing <= commission → PENDING (không thay đổi)
                assertFalse(isEligible, "Commission không eligible khi billing <= commission amount");
                assertEquals(CommissionStatus.PENDING, commission.getStatus(),
                                "Status phải giữ PENDING khi billing <= commission");
        }

        /**
         * Property 4: Commission đã PAID không cần tính lại eligibility
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 4: Commission eligibility dựa trên billing")
        void paidCommissionShouldAlwaysReturnTrue(
                        @ForAll("validCommissionIds") Long commissionId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("validAmounts") BigDecimal commissionAmount) {

                // Chuẩn bị dữ liệu - commission đã PAID
                EmployeeCommissionEntity commission = createCommission(commissionId, companyId, commissionAmount);
                commission.setStatus(CommissionStatus.PAID);

                when(commissionRepository.findByIdAndDeletedFalse(commissionId))
                                .thenReturn(Optional.of(commission));

                // Thực thi
                boolean isEligible = commissionService.calculateEligibility(commissionId);

                // Kiểm tra: PAID commission luôn trả về true
                assertTrue(isEligible, "PAID commission phải luôn trả về true");
                assertEquals(CommissionStatus.PAID, commission.getStatus(),
                                "Status phải giữ nguyên PAID");
        }

        // === Generators ===

        @Provide
        Arbitrary<Long> validCommissionIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<Long> validCompanyIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<BigDecimal> validAmounts() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000))
                                .ofScale(0);
        }

        /**
         * Generator cho billing > commission amount
         */
        @Provide
        Arbitrary<BigDecimal[]> billingGreaterThanCommission() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000))
                                .ofScale(0)
                                .flatMap(commission -> Arbitraries.bigDecimals()
                                                .between(commission.add(BigDecimal.ONE),
                                                                commission.multiply(BigDecimal.valueOf(10)))
                                                .ofScale(0)
                                                .map(billing -> new BigDecimal[] { billing, commission }));
        }

        /**
         * Generator cho billing <= commission amount
         */
        @Provide
        Arbitrary<BigDecimal[]> billingLessOrEqualCommission() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000))
                                .ofScale(0)
                                .flatMap(commission -> Arbitraries.bigDecimals()
                                                .between(BigDecimal.ZERO, commission)
                                                .ofScale(0)
                                                .map(billing -> new BigDecimal[] { billing, commission }));
        }

        // === Helper methods ===

        private EmployeeCommissionEntity createCommission(Long id, Long companyId, BigDecimal amount) {
                EmployeeCommissionEntity commission = new EmployeeCommissionEntity();
                commission.setId(id);
                commission.setCompanyId(companyId);
                commission.setEmployeeCode("EMP001");
                commission.setAmount(amount);
                commission.setStatus(CommissionStatus.PENDING);
                commission.setCompanyBillingAtCreation(BigDecimal.ZERO);
                return commission;
        }

        private WalletEntity createWallet(Long companyId, BigDecimal totalBilling) {
                WalletEntity wallet = new WalletEntity();
                wallet.setId(companyId);
                wallet.setCompanyId(companyId);
                wallet.setBalance(BigDecimal.valueOf(100000));
                wallet.setTotalBilling(totalBilling);
                return wallet;
        }
}
