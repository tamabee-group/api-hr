package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.BadRequestException;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho WalletService
 * Feature: wallet-management, Property 1: Balance Consistency
 * Validates: Requirements 7.2, 7.4, 9.1, 9.3, 10.1, 10.4
 */
public class WalletServicePropertyTest {

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
     * Property 1: Balance Consistency - DEPOSIT
     * Với bất kỳ giao dịch DEPOSIT nào, balanceAfter PHẢI bằng balanceBefore +
     * amount
     * 
     * Validates: Requirements 7.2, 7.4
     */
    @Property(tries = 100)
    void depositShouldIncreaseBalanceByExactAmount(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("validBalances") BigDecimal initialBalance,
            @ForAll("validAmounts") BigDecimal depositAmount) {

        // Chuẩn bị dữ liệu
        WalletEntity wallet = createWalletEntity(companyId, initialBalance);

        when(walletRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<WalletTransactionEntity> transactionCaptor = ArgumentCaptor
                .forClass(WalletTransactionEntity.class);
        when(walletTransactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(invocation -> {
                    WalletTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        // Thực thi
        WalletTransactionResponse response = walletService.addBalance(
                companyId, depositAmount, "Test deposit", TransactionType.DEPOSIT, null);

        // Kiểm tra: Balance Consistency Property
        WalletTransactionEntity savedTransaction = transactionCaptor.getValue();
        BigDecimal expectedBalanceAfter = initialBalance.add(depositAmount);

        assertEquals(initialBalance, savedTransaction.getBalanceBefore(),
                "balanceBefore phải bằng số dư ban đầu");
        assertEquals(expectedBalanceAfter, savedTransaction.getBalanceAfter(),
                "balanceAfter phải bằng balanceBefore + amount");
        assertEquals(depositAmount, savedTransaction.getAmount(),
                "amount phải bằng số tiền nạp");
        assertEquals(TransactionType.DEPOSIT, savedTransaction.getTransactionType(),
                "loại giao dịch phải là DEPOSIT");

        // Kiểm tra số dư wallet được cập nhật
        assertEquals(expectedBalanceAfter, wallet.getBalance(),
                "số dư wallet phải được cập nhật thành balanceAfter");
    }

    /**
     * Property 1: Balance Consistency - REFUND
     * Với bất kỳ giao dịch REFUND nào, balanceAfter PHẢI bằng balanceBefore +
     * amount
     * 
     * Validates: Requirements 10.1, 10.4
     */
    @Property(tries = 100)
    void refundShouldIncreaseBalanceByExactAmount(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("validBalances") BigDecimal initialBalance,
            @ForAll("validAmounts") BigDecimal refundAmount) {

        // Chuẩn bị dữ liệu
        WalletEntity wallet = createWalletEntity(companyId, initialBalance);

        when(walletRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<WalletTransactionEntity> transactionCaptor = ArgumentCaptor
                .forClass(WalletTransactionEntity.class);
        when(walletTransactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(invocation -> {
                    WalletTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        // Thực thi
        WalletTransactionResponse response = walletService.addBalance(
                companyId, refundAmount, "Test refund", TransactionType.REFUND, null);

        // Kiểm tra: Balance Consistency Property
        WalletTransactionEntity savedTransaction = transactionCaptor.getValue();
        BigDecimal expectedBalanceAfter = initialBalance.add(refundAmount);

        assertEquals(initialBalance, savedTransaction.getBalanceBefore(),
                "balanceBefore phải bằng số dư ban đầu");
        assertEquals(expectedBalanceAfter, savedTransaction.getBalanceAfter(),
                "balanceAfter phải bằng balanceBefore + amount");
        assertEquals(refundAmount, savedTransaction.getAmount(),
                "amount phải bằng số tiền hoàn");
        assertEquals(TransactionType.REFUND, savedTransaction.getTransactionType(),
                "loại giao dịch phải là REFUND");
    }

    /**
     * Property 1: Balance Consistency - BILLING
     * Với bất kỳ giao dịch BILLING nào, balanceAfter PHẢI bằng balanceBefore -
     * amount
     * 
     * Validates: Requirements 9.1, 9.3
     */
    @Property(tries = 100)
    void billingShouldDecreaseBalanceByExactAmount(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("sufficientBalanceAndAmount") BigDecimal[] balanceAndAmount) {

        BigDecimal initialBalance = balanceAndAmount[0];
        BigDecimal billingAmount = balanceAndAmount[1];

        // Chuẩn bị dữ liệu
        WalletEntity wallet = createWalletEntity(companyId, initialBalance);

        when(walletRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(WalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<WalletTransactionEntity> transactionCaptor = ArgumentCaptor
                .forClass(WalletTransactionEntity.class);
        when(walletTransactionRepository.save(transactionCaptor.capture()))
                .thenAnswer(invocation -> {
                    WalletTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        // Thực thi
        WalletTransactionResponse response = walletService.deductBalance(
                companyId, billingAmount, "Test billing", TransactionType.BILLING, null);

        // Kiểm tra: Balance Consistency Property
        WalletTransactionEntity savedTransaction = transactionCaptor.getValue();
        BigDecimal expectedBalanceAfter = initialBalance.subtract(billingAmount);

        assertEquals(initialBalance, savedTransaction.getBalanceBefore(),
                "balanceBefore phải bằng số dư ban đầu");
        assertEquals(expectedBalanceAfter, savedTransaction.getBalanceAfter(),
                "balanceAfter phải bằng balanceBefore - amount");
        assertEquals(billingAmount, savedTransaction.getAmount(),
                "amount phải bằng số tiền billing");
        assertEquals(TransactionType.BILLING, savedTransaction.getTransactionType(),
                "loại giao dịch phải là BILLING");

        // Kiểm tra số dư wallet được cập nhật
        assertEquals(expectedBalanceAfter, wallet.getBalance(),
                "số dư wallet phải được cập nhật thành balanceAfter");
    }

    /**
     * Property: Số tiền không hợp lệ (<=0) phải throw BadRequestException
     */
    @Property(tries = 100)
    void invalidAmountShouldThrowBadRequest(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("invalidAmounts") BigDecimal invalidAmount) {

        // Chuẩn bị dữ liệu
        WalletEntity wallet = createWalletEntity(companyId, BigDecimal.valueOf(1000));

        when(walletRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(wallet));

        // Kiểm tra addBalance
        assertThrows(BadRequestException.class,
                () -> walletService.addBalance(companyId, invalidAmount, "Test", TransactionType.DEPOSIT, null));

        // Kiểm tra deductBalance
        assertThrows(BadRequestException.class,
                () -> walletService.deductBalance(companyId, invalidAmount, "Test", TransactionType.BILLING, null));

        // Xác nhận không có transaction nào được lưu
        verify(walletTransactionRepository, never()).save(any(WalletTransactionEntity.class));
    }

    /**
     * Property: Số dư không đủ phải throw BadRequestException khi trừ tiền
     */
    @Property(tries = 100)
    void insufficientBalanceShouldThrowBadRequest(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("insufficientBalanceAndAmount") BigDecimal[] balanceAndAmount) {

        BigDecimal initialBalance = balanceAndAmount[0];
        BigDecimal deductAmount = balanceAndAmount[1];

        // Chuẩn bị dữ liệu
        WalletEntity wallet = createWalletEntity(companyId, initialBalance);

        when(walletRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(wallet));

        // Kiểm tra
        assertThrows(BadRequestException.class,
                () -> walletService.deductBalance(companyId, deductAmount, "Test", TransactionType.BILLING, null));

        // Xác nhận số dư wallet không thay đổi
        assertEquals(initialBalance, wallet.getBalance());

        // Xác nhận không có transaction nào được lưu
        verify(walletTransactionRepository, never()).save(any(WalletTransactionEntity.class));
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
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1000000))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> invalidAmounts() {
        return Arbitraries.bigDecimals()
                .lessOrEqual(BigDecimal.ZERO)
                .ofScale(2);
    }

    /**
     * Generator cho balance và amount sao cho balance >= amount
     */
    @Provide
    Arbitrary<BigDecimal[]> sufficientBalanceAndAmount() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(1000000))
                .ofScale(2)
                .flatMap(balance -> Arbitraries.bigDecimals()
                        .between(BigDecimal.valueOf(0.01), balance)
                        .ofScale(2)
                        .map(amount -> new BigDecimal[] { balance, amount }));
    }

    /**
     * Generator cho balance và amount sao cho balance < amount
     */
    @Provide
    Arbitrary<BigDecimal[]> insufficientBalanceAndAmount() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1000))
                .ofScale(2)
                .flatMap(balance -> Arbitraries.bigDecimals()
                        .between(balance.add(BigDecimal.valueOf(0.01)), balance.add(BigDecimal.valueOf(10000)))
                        .ofScale(2)
                        .map(amount -> new BigDecimal[] { balance, amount }));
    }

    // === Helper methods ===

    private WalletEntity createWalletEntity(Long companyId, BigDecimal balance) {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(companyId);
        wallet.setCompanyId(companyId);
        wallet.setBalance(balance);
        return wallet;
    }
}
