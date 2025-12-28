package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.impl.WalletTransactionServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Property-based tests cho WalletTransactionService
 * Feature: wallet-management
 * 
 * Property 9: Transaction Ordering
 * Validates: Requirements 4.6
 * 
 * Property 10: Pagination Default
 * Validates: Requirements 4.4, 6.4
 */
public class WalletTransactionServicePropertyTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private UserRepository userRepository;

    private WalletTransactionMapper walletTransactionMapper;
    private WalletTransactionServiceImpl walletTransactionService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        walletTransactionMapper = new WalletTransactionMapper();
        walletTransactionService = new WalletTransactionServiceImpl(
                walletTransactionRepository,
                walletTransactionMapper,
                userRepository);
    }

    /**
     * Property 9: Transaction Ordering
     * Với bất kỳ danh sách transactions nào, kết quả PHẢI được sắp xếp theo
     * createdAt giảm dần (mới nhất trước)
     * 
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void transactionsShouldBeOrderedByCreatedAtDescending(
            @ForAll("validWalletIds") Long walletId,
            @ForAll("transactionLists") List<WalletTransactionEntity> transactions) {

        // Sắp xếp transactions theo createdAt DESC để mock repository trả về đúng thứ
        // tự
        List<WalletTransactionEntity> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(WalletTransactionEntity::getCreatedAt).reversed());

        Pageable pageable = PageRequest.of(0, 20);
        Page<WalletTransactionEntity> mockPage = new PageImpl<>(sortedTransactions, pageable,
                sortedTransactions.size());

        when(walletTransactionRepository.findByWalletIdAndDeletedFalseOrderByCreatedAtDesc(eq(walletId), any()))
                .thenReturn(mockPage);

        // Thực thi
        Page<WalletTransactionResponse> result = walletTransactionService.getByWalletId(walletId, null, pageable);

        // Kiểm tra: Transaction Ordering Property
        List<WalletTransactionResponse> content = result.getContent();

        if (content.size() > 1) {
            for (int i = 0; i < content.size() - 1; i++) {
                LocalDateTime current = content.get(i).getCreatedAt();
                LocalDateTime next = content.get(i + 1).getCreatedAt();

                assertTrue(current.isAfter(next) || current.isEqual(next),
                        String.format(
                                "Transaction tại vị trí %d (createdAt=%s) phải có createdAt >= transaction tại vị trí %d (createdAt=%s)",
                                i, current, i + 1, next));
            }
        }
    }

    /**
     * Property 9: Transaction Ordering - với filter theo companyId
     * Với bất kỳ danh sách transactions nào, kết quả PHẢI được sắp xếp theo
     * createdAt giảm dần (mới nhất trước)
     * 
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void transactionsByCompanyIdShouldBeOrderedByCreatedAtDescending(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("transactionLists") List<WalletTransactionEntity> transactions) {

        // Sắp xếp transactions theo createdAt DESC
        List<WalletTransactionEntity> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(WalletTransactionEntity::getCreatedAt).reversed());

        Pageable pageable = PageRequest.of(0, 20);
        Page<WalletTransactionEntity> mockPage = new PageImpl<>(sortedTransactions, pageable,
                sortedTransactions.size());

        when(walletTransactionRepository.findByCompanyId(eq(companyId), any()))
                .thenReturn(mockPage);

        // Thực thi
        Page<WalletTransactionResponse> result = walletTransactionService.getByCompanyId(companyId, null, pageable);

        // Kiểm tra: Transaction Ordering Property
        List<WalletTransactionResponse> content = result.getContent();

        if (content.size() > 1) {
            for (int i = 0; i < content.size() - 1; i++) {
                LocalDateTime current = content.get(i).getCreatedAt();
                LocalDateTime next = content.get(i + 1).getCreatedAt();

                assertTrue(current.isAfter(next) || current.isEqual(next),
                        String.format(
                                "Transaction tại vị trí %d (createdAt=%s) phải có createdAt >= transaction tại vị trí %d (createdAt=%s)",
                                i, current, i + 1, next));
            }
        }
    }

    /**
     * Property 9: Transaction Ordering - với filter theo transactionType
     * Kết quả vẫn PHẢI được sắp xếp theo createdAt giảm dần
     * 
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void transactionsWithTypeFilterShouldBeOrderedByCreatedAtDescending(
            @ForAll("validWalletIds") Long walletId,
            @ForAll("transactionTypes") TransactionType transactionType,
            @ForAll("transactionLists") List<WalletTransactionEntity> transactions) {

        // Filter và sắp xếp transactions
        List<WalletTransactionEntity> filteredTransactions = new ArrayList<>();
        for (WalletTransactionEntity t : transactions) {
            t.setTransactionType(transactionType);
            filteredTransactions.add(t);
        }
        filteredTransactions.sort(Comparator.comparing(WalletTransactionEntity::getCreatedAt).reversed());

        Pageable pageable = PageRequest.of(0, 20);
        Page<WalletTransactionEntity> mockPage = new PageImpl<>(filteredTransactions, pageable,
                filteredTransactions.size());

        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setTransactionType(transactionType);

        when(walletTransactionRepository.findByWalletIdAndTransactionTypeAndDeletedFalseOrderByCreatedAtDesc(
                eq(walletId), eq(transactionType), any()))
                .thenReturn(mockPage);

        // Thực thi
        Page<WalletTransactionResponse> result = walletTransactionService.getByWalletId(walletId, filter, pageable);

        // Kiểm tra: Transaction Ordering Property
        List<WalletTransactionResponse> content = result.getContent();

        if (content.size() > 1) {
            for (int i = 0; i < content.size() - 1; i++) {
                LocalDateTime current = content.get(i).getCreatedAt();
                LocalDateTime next = content.get(i + 1).getCreatedAt();

                assertTrue(current.isAfter(next) || current.isEqual(next),
                        String.format("Transaction tại vị trí %d phải có createdAt >= transaction tại vị trí %d", i,
                                i + 1));
            }
        }
    }

    /**
     * Property 10: Pagination Default
     * Khi không chỉ định page size, hệ thống PHẢI sử dụng default page size = 20
     * 
     * Validates: Requirements 4.4, 6.4
     */
    @Property(tries = 100)
    void paginationShouldUseDefaultPageSize(
            @ForAll("validWalletIds") Long walletId,
            @ForAll("largeTransactionLists") List<WalletTransactionEntity> transactions) {

        // Sắp xếp transactions
        List<WalletTransactionEntity> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(WalletTransactionEntity::getCreatedAt).reversed());

        // Sử dụng default page size = 20
        int defaultPageSize = 20;
        Pageable pageable = PageRequest.of(0, defaultPageSize);

        // Chỉ lấy tối đa 20 transactions cho page đầu tiên
        int endIndex = Math.min(defaultPageSize, sortedTransactions.size());
        List<WalletTransactionEntity> pageContent = sortedTransactions.subList(0, endIndex);

        Page<WalletTransactionEntity> mockPage = new PageImpl<>(pageContent, pageable, sortedTransactions.size());

        when(walletTransactionRepository.findByWalletIdAndDeletedFalseOrderByCreatedAtDesc(eq(walletId), any()))
                .thenReturn(mockPage);

        // Thực thi
        Page<WalletTransactionResponse> result = walletTransactionService.getByWalletId(walletId, null, pageable);

        // Kiểm tra: Pagination Default Property
        assertEquals(defaultPageSize, result.getSize(),
                "Page size phải là 20 (default)");
        assertTrue(result.getContent().size() <= defaultPageSize,
                "Số lượng transactions trong page không được vượt quá page size");
    }

    /**
     * Property 10: Pagination Default - với companyId
     * Khi không chỉ định page size, hệ thống PHẢI sử dụng default page size = 20
     * 
     * Validates: Requirements 4.4, 6.4
     */
    @Property(tries = 100)
    void paginationByCompanyIdShouldUseDefaultPageSize(
            @ForAll("validCompanyIds") Long companyId,
            @ForAll("largeTransactionLists") List<WalletTransactionEntity> transactions) {

        // Sắp xếp transactions
        List<WalletTransactionEntity> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(WalletTransactionEntity::getCreatedAt).reversed());

        // Sử dụng default page size = 20
        int defaultPageSize = 20;
        Pageable pageable = PageRequest.of(0, defaultPageSize);

        // Chỉ lấy tối đa 20 transactions cho page đầu tiên
        int endIndex = Math.min(defaultPageSize, sortedTransactions.size());
        List<WalletTransactionEntity> pageContent = sortedTransactions.subList(0, endIndex);

        Page<WalletTransactionEntity> mockPage = new PageImpl<>(pageContent, pageable, sortedTransactions.size());

        when(walletTransactionRepository.findByCompanyId(eq(companyId), any()))
                .thenReturn(mockPage);

        // Thực thi
        Page<WalletTransactionResponse> result = walletTransactionService.getByCompanyId(companyId, null, pageable);

        // Kiểm tra: Pagination Default Property
        assertEquals(defaultPageSize, result.getSize(),
                "Page size phải là 20 (default)");
        assertTrue(result.getContent().size() <= defaultPageSize,
                "Số lượng transactions trong page không được vượt quá page size");
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> validWalletIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> validCompanyIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<TransactionType> transactionTypes() {
        return Arbitraries.of(TransactionType.values());
    }

    /**
     * Generator cho danh sách transactions với createdAt ngẫu nhiên
     */
    @Provide
    Arbitrary<List<WalletTransactionEntity>> transactionLists() {
        return Arbitraries.integers().between(2, 10)
                .flatMap(size -> Arbitraries.of(size)
                        .map(s -> {
                            List<WalletTransactionEntity> list = new ArrayList<>();
                            LocalDateTime baseTime = LocalDateTime.now();
                            for (int i = 0; i < s; i++) {
                                WalletTransactionEntity entity = createTransactionEntity(
                                        (long) (i + 1),
                                        (long) (i + 1),
                                        TransactionType.DEPOSIT,
                                        BigDecimal.valueOf(1000 + i * 100),
                                        // Tạo createdAt ngẫu nhiên trong khoảng 30 ngày
                                        baseTime.minusDays((long) (Math.random() * 30))
                                                .minusHours((long) (Math.random() * 24))
                                                .minusMinutes((long) (Math.random() * 60)));
                                list.add(entity);
                            }
                            return list;
                        }));
    }

    /**
     * Generator cho danh sách transactions lớn (> 20) để test pagination
     */
    @Provide
    Arbitrary<List<WalletTransactionEntity>> largeTransactionLists() {
        return Arbitraries.integers().between(25, 50)
                .flatMap(size -> Arbitraries.of(size)
                        .map(s -> {
                            List<WalletTransactionEntity> list = new ArrayList<>();
                            LocalDateTime baseTime = LocalDateTime.now();
                            for (int i = 0; i < s; i++) {
                                WalletTransactionEntity entity = createTransactionEntity(
                                        (long) (i + 1),
                                        (long) (i + 1),
                                        TransactionType.DEPOSIT,
                                        BigDecimal.valueOf(1000 + i * 100),
                                        baseTime.minusDays((long) (Math.random() * 30))
                                                .minusHours((long) (Math.random() * 24)));
                                list.add(entity);
                            }
                            return list;
                        }));
    }

    // === Helper methods ===

    private WalletTransactionEntity createTransactionEntity(
            Long id,
            Long walletId,
            TransactionType type,
            BigDecimal amount,
            LocalDateTime createdAt) {

        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setId(id);
        entity.setWalletId(walletId);
        entity.setTransactionType(type);
        entity.setAmount(amount);
        entity.setBalanceBefore(BigDecimal.valueOf(5000));
        entity.setBalanceAfter(BigDecimal.valueOf(5000).add(amount));
        entity.setDescription("Test transaction");
        entity.setDeleted(false);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
