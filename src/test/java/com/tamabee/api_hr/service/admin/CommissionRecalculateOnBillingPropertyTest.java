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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Recalculate Commission on Billing
 * Property 5: Recalculate commission khi có billing mới
 */
@Tag("Feature: tamabee-role-redesign")
public class CommissionRecalculateOnBillingPropertyTest {

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
         * Property 5: Recalculate commission khi có billing mới
         * For any billing transaction mới, hệ thống SHALL recalculate eligibility
         * cho tất cả pending commissions của company liên quan.
         * Khi billing > commission amount, status phải chuyển sang ELIGIBLE.
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 5: Recalculate commission khi có billing mới")
        void recalculateShouldUpdateEligibleCommissions(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("billingGreaterThanCommission") BigDecimal[] billingAndCommission) {

                BigDecimal totalBilling = billingAndCommission[0];
                BigDecimal commissionAmount = billingAndCommission[1];

                // Chuẩn bị dữ liệu - 1 pending commission
                EmployeeCommissionEntity commission = createPendingCommission(1L, companyId, commissionAmount);
                List<EmployeeCommissionEntity> pendingCommissions = Collections.singletonList(commission);
                WalletEntity wallet = createWallet(companyId, totalBilling);

                when(commissionRepository.findPendingByCompanyId(companyId))
                                .thenReturn(pendingCommissions);
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(commissionRepository.save(any(EmployeeCommissionEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Thực thi
                commissionService.recalculateOnBilling(companyId);

                // Kiểm tra: commission phải được cập nhật thành ELIGIBLE
                assertEquals(CommissionStatus.ELIGIBLE, commission.getStatus(),
                                "Status phải là ELIGIBLE khi billing > commission");
                verify(commissionRepository, times(1)).save(commission);
        }

        /**
         * Property 5: Khi billing <= commission, status phải giữ PENDING
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 5: Recalculate commission khi có billing mới")
        void recalculateShouldKeepPendingWhenBillingNotEnough(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("billingLessOrEqualCommission") BigDecimal[] billingAndCommission) {

                BigDecimal totalBilling = billingAndCommission[0];
                BigDecimal commissionAmount = billingAndCommission[1];

                // Chuẩn bị dữ liệu - 1 pending commission
                EmployeeCommissionEntity commission = createPendingCommission(1L, companyId, commissionAmount);
                List<EmployeeCommissionEntity> pendingCommissions = Collections.singletonList(commission);
                WalletEntity wallet = createWallet(companyId, totalBilling);

                when(commissionRepository.findPendingByCompanyId(companyId))
                                .thenReturn(pendingCommissions);
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                // Thực thi
                commissionService.recalculateOnBilling(companyId);

                // Kiểm tra: commission phải giữ PENDING
                assertEquals(CommissionStatus.PENDING, commission.getStatus(),
                                "Status phải giữ PENDING khi billing <= commission");
                verify(commissionRepository, never()).save(any());
        }

        /**
         * Property 5: Khi không có pending commissions, không có gì thay đổi
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 5: Recalculate commission khi có billing mới")
        void recalculateShouldDoNothingWhenNoPendingCommissions(
                        @ForAll("validCompanyIds") Long companyId) {

                // Chuẩn bị dữ liệu - không có pending commission
                when(commissionRepository.findPendingByCompanyId(companyId))
                                .thenReturn(Collections.emptyList());

                // Thực thi
                commissionService.recalculateOnBilling(companyId);

                // Kiểm tra: không có gì được save
                verify(commissionRepository, never()).save(any());
                verify(walletRepository, never()).findByCompanyId(any());
        }

        /**
         * Property 5: Recalculate nhiều commissions cùng lúc
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 5: Recalculate commission khi có billing mới")
        void recalculateShouldUpdateAllEligibleCommissions(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("multipleCommissionsWithBilling") Object[] data) {

                BigDecimal totalBilling = (BigDecimal) data[0];
                @SuppressWarnings("unchecked")
                List<BigDecimal> commissionAmounts = (List<BigDecimal>) data[1];

                // Chuẩn bị dữ liệu - nhiều pending commissions
                List<EmployeeCommissionEntity> pendingCommissions = new ArrayList<>();
                for (int i = 0; i < commissionAmounts.size(); i++) {
                        pendingCommissions.add(
                                        createPendingCommission((long) (i + 1), companyId, commissionAmounts.get(i)));
                }
                WalletEntity wallet = createWallet(companyId, totalBilling);

                when(commissionRepository.findPendingByCompanyId(companyId))
                                .thenReturn(pendingCommissions);
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(commissionRepository.save(any(EmployeeCommissionEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Thực thi
                commissionService.recalculateOnBilling(companyId);

                // Kiểm tra: mỗi commission phải được cập nhật đúng
                int expectedEligibleCount = 0;
                for (int i = 0; i < pendingCommissions.size(); i++) {
                        EmployeeCommissionEntity commission = pendingCommissions.get(i);
                        BigDecimal amount = commissionAmounts.get(i);

                        if (totalBilling.compareTo(amount) > 0) {
                                assertEquals(CommissionStatus.ELIGIBLE, commission.getStatus(),
                                                "Commission " + i + " phải ELIGIBLE khi billing > amount");
                                expectedEligibleCount++;
                        } else {
                                assertEquals(CommissionStatus.PENDING, commission.getStatus(),
                                                "Commission " + i + " phải PENDING khi billing <= amount");
                        }
                }

                verify(commissionRepository, times(expectedEligibleCount)).save(any());
        }

        // === Generators ===

        @Provide
        Arbitrary<Long> validCompanyIds() {
                return Arbitraries.longs().between(1L, 10000L);
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

        /**
         * Generator cho nhiều commissions với billing
         */
        @Provide
        Arbitrary<Object[]> multipleCommissionsWithBilling() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(5000), BigDecimal.valueOf(50000))
                                .ofScale(0)
                                .flatMap(billing -> Arbitraries.bigDecimals()
                                                .between(BigDecimal.valueOf(1000), BigDecimal.valueOf(20000))
                                                .ofScale(0)
                                                .list()
                                                .ofMinSize(1)
                                                .ofMaxSize(5)
                                                .map(amounts -> new Object[] { billing, amounts }));
        }

        // === Helper methods ===

        private EmployeeCommissionEntity createPendingCommission(Long id, Long companyId, BigDecimal amount) {
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
