package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.mapper.admin.WalletMapper;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.repository.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.impl.WalletServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Admin Direct Wallet Operations
 * Property 1: Admin có quyền thao tác wallet trực tiếp
 */
@Tag("Feature: tamabee-role-redesign")
public class AdminWalletDirectOperationsPropertyTest {

        @Mock
        private WalletRepository walletRepository;

        @Mock
        private WalletTransactionRepository walletTransactionRepository;

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private PlanRepository planRepository;

        @Mock
        private UserRepository userRepository;

        private WalletMapper walletMapper;
        private WalletTransactionMapper walletTransactionMapper;
        private WalletServiceImpl walletService;

        @BeforeTry
        void setUp() {
                MockitoAnnotations.openMocks(this);
                walletMapper = new WalletMapper();
                walletTransactionMapper = new WalletTransactionMapper();
                walletService = new WalletServiceImpl(
                                walletRepository,
                                walletTransactionRepository,
                                companyRepository,
                                planRepository,
                                userRepository,
                                walletMapper,
                                walletTransactionMapper);
        }

        /**
         * Property 1: Admin có quyền thao tác wallet trực tiếp - ADD BALANCE
         * For any company wallet và Admin Tamabee user, khi Admin thực hiện
         * addBalanceDirect,
         * hệ thống SHALL thực hiện operation ngay lập tức và tạo transaction log với
         * đầy đủ thông tin
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 1: Admin có quyền thao tác wallet trực tiếp")
        void adminAddBalanceDirectShouldCreateTransactionWithFullInfo(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("validBalances") BigDecimal initialBalance,
                        @ForAll("validAmounts") BigDecimal addAmount,
                        @ForAll("validDescriptions") String description) {

                // Setup Admin user trong SecurityContext
                setupAdminSecurityContext();

                // Chuẩn bị dữ liệu
                WalletEntity wallet = createWalletEntity(companyId, initialBalance);
                UserEntity adminUser = createAdminUser();

                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(walletRepository.save(any(WalletEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(userRepository.findByEmailAndDeletedFalse("admin@tamabee.com"))
                                .thenReturn(Optional.of(adminUser));

                ArgumentCaptor<WalletTransactionEntity> transactionCaptor = ArgumentCaptor
                                .forClass(WalletTransactionEntity.class);
                when(walletTransactionRepository.save(transactionCaptor.capture()))
                                .thenAnswer(invocation -> {
                                        WalletTransactionEntity entity = invocation.getArgument(0);
                                        entity.setId(1L);
                                        return entity;
                                });

                // Thực thi
                WalletTransactionResponse response = walletService.addBalanceDirect(companyId, addAmount, description);

                // Kiểm tra: Transaction được tạo với đầy đủ thông tin
                WalletTransactionEntity savedTransaction = transactionCaptor.getValue();
                BigDecimal expectedBalanceAfter = initialBalance.add(addAmount);

                // Verify balance consistency
                assertEquals(initialBalance, savedTransaction.getBalanceBefore(),
                                "balanceBefore phải bằng số dư ban đầu");
                assertEquals(expectedBalanceAfter, savedTransaction.getBalanceAfter(),
                                "balanceAfter phải bằng balanceBefore + amount");
                assertEquals(addAmount, savedTransaction.getAmount(),
                                "amount phải bằng số tiền thêm");
                assertEquals(TransactionType.DEPOSIT, savedTransaction.getTransactionType(),
                                "loại giao dịch phải là DEPOSIT");

                // Verify operator info trong description
                assertTrue(savedTransaction.getDescription().contains("[ADMIN DIRECT]"),
                                "description phải chứa marker [ADMIN DIRECT]");
                assertTrue(savedTransaction.getDescription().contains("Thực hiện bởi:"),
                                "description phải chứa thông tin operator");

                // Verify wallet balance được cập nhật
                assertEquals(expectedBalanceAfter, wallet.getBalance(),
                                "số dư wallet phải được cập nhật");

                // Verify response không null
                assertNotNull(response, "response không được null");
        }

        /**
         * Property 1: Admin có quyền thao tác wallet trực tiếp - DEDUCT BALANCE
         * For any company wallet và Admin Tamabee user, khi Admin thực hiện
         * deductBalanceDirect,
         * hệ thống SHALL thực hiện operation ngay lập tức và tạo transaction log với
         * đầy đủ thông tin
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 1: Admin có quyền thao tác wallet trực tiếp")
        void adminDeductBalanceDirectShouldCreateTransactionWithFullInfo(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("sufficientBalanceAndAmount") BigDecimal[] balanceAndAmount,
                        @ForAll("validDescriptions") String description) {

                BigDecimal initialBalance = balanceAndAmount[0];
                BigDecimal deductAmount = balanceAndAmount[1];

                // Setup Admin user trong SecurityContext
                setupAdminSecurityContext();

                // Chuẩn bị dữ liệu
                WalletEntity wallet = createWalletEntity(companyId, initialBalance);
                UserEntity adminUser = createAdminUser();

                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(walletRepository.save(any(WalletEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(userRepository.findByEmailAndDeletedFalse("admin@tamabee.com"))
                                .thenReturn(Optional.of(adminUser));

                ArgumentCaptor<WalletTransactionEntity> transactionCaptor = ArgumentCaptor
                                .forClass(WalletTransactionEntity.class);
                when(walletTransactionRepository.save(transactionCaptor.capture()))
                                .thenAnswer(invocation -> {
                                        WalletTransactionEntity entity = invocation.getArgument(0);
                                        entity.setId(1L);
                                        return entity;
                                });

                // Thực thi
                WalletTransactionResponse response = walletService.deductBalanceDirect(companyId, deductAmount,
                                description);

                // Kiểm tra: Transaction được tạo với đầy đủ thông tin
                WalletTransactionEntity savedTransaction = transactionCaptor.getValue();
                BigDecimal expectedBalanceAfter = initialBalance.subtract(deductAmount);

                // Verify balance consistency
                assertEquals(initialBalance, savedTransaction.getBalanceBefore(),
                                "balanceBefore phải bằng số dư ban đầu");
                assertEquals(expectedBalanceAfter, savedTransaction.getBalanceAfter(),
                                "balanceAfter phải bằng balanceBefore - amount");
                assertEquals(deductAmount, savedTransaction.getAmount(),
                                "amount phải bằng số tiền trừ");
                assertEquals(TransactionType.BILLING, savedTransaction.getTransactionType(),
                                "loại giao dịch phải là BILLING");

                // Verify operator info trong description
                assertTrue(savedTransaction.getDescription().contains("[ADMIN DIRECT]"),
                                "description phải chứa marker [ADMIN DIRECT]");
                assertTrue(savedTransaction.getDescription().contains("Thực hiện bởi:"),
                                "description phải chứa thông tin operator");

                // Verify wallet balance được cập nhật
                assertEquals(expectedBalanceAfter, wallet.getBalance(),
                                "số dư wallet phải được cập nhật");

                // Verify response không null
                assertNotNull(response, "response không được null");
        }

        // === Generators ===

        @Provide
        Arbitrary<Long> validCompanyIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<BigDecimal> validBalances() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.ZERO, BigDecimal.valueOf(10000000))
                                .ofScale(0);
        }

        @Provide
        Arbitrary<BigDecimal> validAmounts() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(1), BigDecimal.valueOf(1000000))
                                .ofScale(0);
        }

        @Provide
        Arbitrary<String> validDescriptions() {
                return Arbitraries.strings()
                                .alpha()
                                .ofMinLength(5)
                                .ofMaxLength(100);
        }

        /**
         * Generator cho balance và amount sao cho balance >= amount
         */
        @Provide
        Arbitrary<BigDecimal[]> sufficientBalanceAndAmount() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(1000000))
                                .ofScale(0)
                                .flatMap(balance -> Arbitraries.bigDecimals()
                                                .between(BigDecimal.valueOf(1), balance)
                                                .ofScale(0)
                                                .map(amount -> new BigDecimal[] { balance, amount }));
        }

        // === Helper methods ===

        private void setupAdminSecurityContext() {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "admin@tamabee.com",
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_TAMABEE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        private WalletEntity createWalletEntity(Long companyId, BigDecimal balance) {
                WalletEntity wallet = new WalletEntity();
                wallet.setId(companyId);
                wallet.setCompanyId(companyId);
                wallet.setBalance(balance);
                return wallet;
        }

        private UserEntity createAdminUser() {
                UserEntity user = new UserEntity();
                user.setId(1L);
                user.setEmployeeCode("ADM001");
                user.setEmail("admin@tamabee.com");
                user.setRole(UserRole.ADMIN_TAMABEE);
                user.setCompanyId(0L);
                return user;
        }
}
