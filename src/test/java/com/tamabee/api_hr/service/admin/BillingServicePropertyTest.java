package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.repository.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.impl.BillingServiceImpl;
import com.tamabee.api_hr.service.core.IEmailService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho BillingService
 * Feature: wallet-management, Property 7: Free Trial Calculation
 * Validates: Requirements 9.8
 */
public class BillingServicePropertyTest {

        @Mock
        private WalletRepository walletRepository;

        @Mock
        private WalletTransactionRepository walletTransactionRepository;

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private PlanRepository planRepository;

        @Mock
        private ISettingService settingService;

        @Mock
        private IEmailService emailService;

        @Mock
        private ICommissionService commissionService;

        private WalletTransactionMapper walletTransactionMapper;
        private BillingServiceImpl billingService;

        @BeforeTry
        void setUp() {
                MockitoAnnotations.openMocks(this);
                walletTransactionMapper = new WalletTransactionMapper();
                billingService = new BillingServiceImpl(
                                walletRepository,
                                walletTransactionRepository,
                                companyRepository,
                                planRepository,
                                settingService,
                                emailService,
                                walletTransactionMapper,
                                commissionService);
        }

        /**
         * Property 7: Free Trial Calculation - Không có referral
         * Với bất kỳ company nào không có referral code:
         * freeTrialEndDate = createdAt + freeTrialMonths
         * 
         * Validates: Requirements 9.8
         */
        @Property(tries = 100)
        void freeTrialWithoutReferralShouldAddOnlyFreeTrialMonths(
                        @ForAll("validCreatedAt") LocalDateTime createdAt,
                        @ForAll("validFreeTrialMonths") int freeTrialMonths,
                        @ForAll("validReferralBonusMonths") int referralBonusMonths) {

                // Chuẩn bị mock
                when(settingService.getFreeTrialMonths()).thenReturn(freeTrialMonths);
                when(settingService.getReferralBonusMonths()).thenReturn(referralBonusMonths);

                // Thực thi - không có referral
                LocalDateTime result = billingService.calculateFreeTrialEndDate(createdAt, false);

                // Kiểm tra: chỉ cộng freeTrialMonths, không cộng referralBonusMonths
                LocalDateTime expected = createdAt.plusMonths(freeTrialMonths);

                assertEquals(expected, result,
                                String.format("Với createdAt=%s, freeTrialMonths=%d, không có referral: " +
                                                "freeTrialEndDate phải là %s nhưng nhận được %s",
                                                createdAt, freeTrialMonths, expected, result));
        }

        /**
         * Property 7: Free Trial Calculation - Có referral
         * Với bất kỳ company nào có referral code:
         * freeTrialEndDate = createdAt + freeTrialMonths + referralBonusMonths
         * 
         * Validates: Requirements 9.8
         */
        @Property(tries = 100)
        void freeTrialWithReferralShouldAddBothMonths(
                        @ForAll("validCreatedAt") LocalDateTime createdAt,
                        @ForAll("validFreeTrialMonths") int freeTrialMonths,
                        @ForAll("validReferralBonusMonths") int referralBonusMonths) {

                // Chuẩn bị mock
                when(settingService.getFreeTrialMonths()).thenReturn(freeTrialMonths);
                when(settingService.getReferralBonusMonths()).thenReturn(referralBonusMonths);

                // Thực thi - có referral
                LocalDateTime result = billingService.calculateFreeTrialEndDate(createdAt, true);

                // Kiểm tra: cộng cả freeTrialMonths và referralBonusMonths
                LocalDateTime expected = createdAt.plusMonths(freeTrialMonths + referralBonusMonths);

                assertEquals(expected, result,
                                String.format("Với createdAt=%s, freeTrialMonths=%d, referralBonusMonths=%d, có referral: "
                                                +
                                                "freeTrialEndDate phải là %s nhưng nhận được %s",
                                                createdAt, freeTrialMonths, referralBonusMonths, expected, result));
        }

        /**
         * Property: Free trial với referral luôn dài hơn hoặc bằng không có referral
         * 
         * Validates: Requirements 9.8
         */
        @Property(tries = 100)
        void freeTrialWithReferralShouldBeGreaterOrEqualToWithoutReferral(
                        @ForAll("validCreatedAt") LocalDateTime createdAt,
                        @ForAll("validFreeTrialMonths") int freeTrialMonths,
                        @ForAll("validReferralBonusMonths") int referralBonusMonths) {

                // Chuẩn bị mock
                when(settingService.getFreeTrialMonths()).thenReturn(freeTrialMonths);
                when(settingService.getReferralBonusMonths()).thenReturn(referralBonusMonths);

                // Thực thi
                LocalDateTime withoutReferral = billingService.calculateFreeTrialEndDate(createdAt, false);
                LocalDateTime withReferral = billingService.calculateFreeTrialEndDate(createdAt, true);

                // Kiểm tra: có referral phải >= không có referral
                assertTrue(withReferral.isAfter(withoutReferral) || withReferral.isEqual(withoutReferral),
                                String.format("Free trial với referral (%s) phải >= không có referral (%s)",
                                                withReferral, withoutReferral));
        }

        /**
         * Property: Khoảng cách giữa có và không có referral phải đúng bằng
         * referralBonusMonths (tính theo số tháng, không phải ngày chính xác)
         * 
         * Lưu ý: Java's plusMonths() có thể cho kết quả khác nhau với các ngày cuối
         * tháng
         * Ví dụ: March 31 + 1 month = April 30 (không phải April 31)
         * Do đó, (date + N months) + M months có thể khác date + (N+M) months
         * 
         * Test này verify rằng implementation tính đúng theo công thức:
         * withReferral = createdAt + (freeTrialMonths + referralBonusMonths)
         * 
         * Validates: Requirements 9.8
         */
        @Property(tries = 100)
        void differenceBetweenReferralAndNoReferralShouldEqualBonusMonths(
                        @ForAll("validCreatedAt") LocalDateTime createdAt,
                        @ForAll("validFreeTrialMonths") int freeTrialMonths,
                        @ForAll("validReferralBonusMonths") int referralBonusMonths) {

                // Chuẩn bị mock
                when(settingService.getFreeTrialMonths()).thenReturn(freeTrialMonths);
                when(settingService.getReferralBonusMonths()).thenReturn(referralBonusMonths);

                // Thực thi
                LocalDateTime withoutReferral = billingService.calculateFreeTrialEndDate(createdAt, false);
                LocalDateTime withReferral = billingService.calculateFreeTrialEndDate(createdAt, true);

                // Kiểm tra: withReferral phải bằng createdAt + (freeTrialMonths +
                // referralBonusMonths)
                // Đây là cách implementation tính, và là cách đúng theo business logic
                LocalDateTime expectedWithReferral = createdAt.plusMonths(freeTrialMonths + referralBonusMonths);

                assertEquals(expectedWithReferral, withReferral,
                                String.format("withReferral phải bằng createdAt + %d tháng (freeTrialMonths=%d + referralBonusMonths=%d)",
                                                freeTrialMonths + referralBonusMonths, freeTrialMonths,
                                                referralBonusMonths));

                // Kiểm tra thêm: withoutReferral phải bằng createdAt + freeTrialMonths
                LocalDateTime expectedWithoutReferral = createdAt.plusMonths(freeTrialMonths);
                assertEquals(expectedWithoutReferral, withoutReferral,
                                String.format("withoutReferral phải bằng createdAt + %d tháng", freeTrialMonths));
        }

        // === Generators ===

        @Provide
        Arbitrary<LocalDateTime> validCreatedAt() {
                // Tạo ngày trong khoảng 2 năm gần đây
                return Arbitraries.longs()
                                .between(0, 730) // 0-730 ngày
                                .map(days -> LocalDateTime.now().minusDays(days).truncatedTo(ChronoUnit.SECONDS));
        }

        @Provide
        Arbitrary<Integer> validFreeTrialMonths() {
                // Số tháng miễn phí từ 1-12
                return Arbitraries.integers().between(1, 12);
        }

        @Provide
        Arbitrary<Integer> validReferralBonusMonths() {
                // Số tháng bonus từ 0-6
                return Arbitraries.integers().between(0, 6);
        }
}
