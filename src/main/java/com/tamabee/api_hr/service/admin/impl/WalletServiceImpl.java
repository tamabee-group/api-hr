package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.RefundRequest;
import com.tamabee.api_hr.dto.request.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.WalletOverviewResponse;
import com.tamabee.api_hr.dto.response.WalletResponse;
import com.tamabee.api_hr.dto.response.WalletStatisticsResponse;
import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.admin.WalletMapper;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.repository.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.IWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service quản lý ví tiền của công ty
 * Hỗ trợ xem thông tin ví, thêm/trừ số dư, hoàn tiền và thống kê
 */
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements IWalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    // ==================== View Operations ====================

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getByCompanyId(Long companyId) {
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        String planName = getPlanName(companyId);
        return walletMapper.toResponse(wallet, planName);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        Long companyId = getCurrentUserCompanyId();
        return getByCompanyId(companyId);
    }

    // ==================== Balance Operations ====================

    @Override
    @Transactional
    public WalletTransactionResponse addBalance(Long companyId, BigDecimal amount, String description,
            TransactionType type, Long referenceId) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Lấy wallet
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        // Tính toán balance
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        // Cập nhật balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // Tạo transaction record
        WalletTransactionEntity transaction = walletTransactionMapper.createEntity(
                wallet.getId(),
                type,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId);
        WalletTransactionEntity savedTransaction = walletTransactionRepository.save(transaction);

        return walletTransactionMapper.toResponse(savedTransaction);
    }

    @Override
    @Transactional
    public WalletTransactionResponse deductBalance(Long companyId, BigDecimal amount, String description,
            TransactionType type, Long referenceId) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Lấy wallet
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        // Kiểm tra số dư
        BigDecimal balanceBefore = wallet.getBalance();
        if (balanceBefore.compareTo(amount) < 0) {
            throw BadRequestException.insufficientBalance();
        }

        // Tính toán balance
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // Cập nhật balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // Tạo transaction record
        WalletTransactionEntity transaction = walletTransactionMapper.createEntity(
                wallet.getId(),
                type,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId);
        WalletTransactionEntity savedTransaction = walletTransactionRepository.save(transaction);

        return walletTransactionMapper.toResponse(savedTransaction);
    }

    // ==================== Admin Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<WalletOverviewResponse> getOverview(BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable) {
        Page<WalletEntity> wallets;

        // Nếu có filter balance
        if (minBalance != null && maxBalance != null) {
            wallets = walletRepository.findByBalanceBetween(minBalance, maxBalance, pageable);
        } else {
            wallets = walletRepository.findAllWallets(pageable);
        }

        return wallets.map(wallet -> {
            // Lấy thông tin company
            CompanyEntity company = companyRepository.findById(wallet.getCompanyId()).orElse(null);
            String companyName = company != null ? company.getName() : "Unknown";

            // Lấy tên plan
            String planName = getPlanNameFromCompany(company);

            // Lấy tổng deposits và billings
            BigDecimal totalDeposits = walletTransactionRepository.sumDepositsByWalletId(wallet.getId());
            BigDecimal totalBillings = walletTransactionRepository.sumBillingsByWalletId(wallet.getId());

            return walletMapper.toOverviewResponse(wallet, companyName, planName, totalDeposits, totalBillings);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public WalletStatisticsResponse getStatistics() {
        WalletStatisticsResponse response = new WalletStatisticsResponse();

        // Tổng số công ty
        response.setTotalCompanies(walletRepository.countAllWallets());

        // Tổng số dư
        response.setTotalBalance(walletRepository.sumAllBalances());

        // Số công ty có số dư thấp
        response.setCompaniesWithLowBalance(walletRepository.countCompaniesWithLowBalance());

        // Số công ty đang trong thời gian miễn phí
        response.setCompaniesInFreeTrial(walletRepository.countCompaniesInFreeTrial(LocalDateTime.now()));

        // Tổng deposits và billings
        response.setTotalDeposits(walletTransactionRepository.sumAllDeposits());
        response.setTotalBillings(walletTransactionRepository.sumAllBillings());

        return response;
    }

    @Override
    @Transactional
    public WalletTransactionResponse createRefund(Long companyId, RefundRequest request) {
        // Validate request
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        String description = "Hoàn tiền: " + request.getReason();
        return addBalance(companyId, request.getAmount(), description, TransactionType.REFUND, null);
    }

    // ==================== Direct Wallet Operations (Admin Only)
    // ====================

    @Override
    @Transactional
    public WalletTransactionResponse addBalanceDirect(Long companyId, BigDecimal amount, String description) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Lấy thông tin operator từ SecurityContext
        String operatorInfo = getCurrentOperatorInfo();

        // Tạo description với thông tin operator
        String fullDescription = String.format("[ADMIN DIRECT] %s - Thực hiện bởi: %s", description, operatorInfo);

        // Sử dụng method addBalance có sẵn với TransactionType.DEPOSIT
        return addBalance(companyId, amount, fullDescription, TransactionType.DEPOSIT, null);
    }

    @Override
    @Transactional
    public WalletTransactionResponse deductBalanceDirect(Long companyId, BigDecimal amount, String description) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Lấy thông tin operator từ SecurityContext
        String operatorInfo = getCurrentOperatorInfo();

        // Tạo description với thông tin operator
        String fullDescription = String.format("[ADMIN DIRECT] %s - Thực hiện bởi: %s", description, operatorInfo);

        // Sử dụng method deductBalance có sẵn với TransactionType.BILLING
        return deductBalance(companyId, amount, fullDescription, TransactionType.BILLING, null);
    }

    // ==================== Transaction Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionsByWalletId(Long walletId, TransactionFilterRequest filter,
            Pageable pageable) {
        Page<WalletTransactionEntity> transactions = queryTransactionsByWalletId(walletId, filter, pageable);
        return transactions.map(walletTransactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionsByCompanyId(Long companyId, TransactionFilterRequest filter,
            Pageable pageable) {
        Page<WalletTransactionEntity> transactions = queryTransactionsByCompanyId(companyId, filter, pageable);
        return transactions.map(walletTransactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(TransactionFilterRequest filter, Pageable pageable) {
        Long companyId = getCurrentUserCompanyId();
        return getTransactionsByCompanyId(companyId, filter, pageable);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy companyId của user hiện tại từ JWT token
     */
    private Long getCurrentUserCompanyId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        return user.getCompanyId();
    }

    /**
     * Lấy thông tin operator hiện tại từ SecurityContext
     * Dùng để log vào transaction description
     */
    private String getCurrentOperatorInfo() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Unknown";
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email).orElse(null);

        if (user != null) {
            return String.format("%s (%s)", user.getEmployeeCode(), email);
        }
        return email;
    }

    /**
     * Lấy tên plan từ company entity
     */
    private String getPlanNameFromCompany(CompanyEntity company) {
        if (company == null || company.getPlanId() == null) {
            return null;
        }

        return planRepository.findByIdAndDeletedFalse(company.getPlanId())
                .map(PlanEntity::getNameVi)
                .orElse(null);
    }

    /**
     * Lấy tên plan của company
     */
    private String getPlanName(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        return getPlanNameFromCompany(company);
    }

    /**
     * Query transactions theo walletId với filter
     */
    private Page<WalletTransactionEntity> queryTransactionsByWalletId(Long walletId, TransactionFilterRequest filter,
            Pageable pageable) {
        boolean hasType = filter != null && filter.getTransactionType() != null;
        boolean hasDateRange = filter != null && filter.getFromDate() != null && filter.getToDate() != null;

        if (hasType && hasDateRange) {
            return walletTransactionRepository.findByWalletIdAndTypeAndDateRange(
                    walletId, filter.getTransactionType(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasType) {
            return walletTransactionRepository.findByWalletIdAndTransactionTypeAndDeletedFalseOrderByCreatedAtDesc(
                    walletId, filter.getTransactionType(), pageable);
        } else if (hasDateRange) {
            return walletTransactionRepository.findByWalletIdAndDateRange(
                    walletId, filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return walletTransactionRepository.findByWalletIdAndDeletedFalseOrderByCreatedAtDesc(walletId, pageable);
        }
    }

    /**
     * Query transactions theo companyId với filter
     */
    private Page<WalletTransactionEntity> queryTransactionsByCompanyId(Long companyId, TransactionFilterRequest filter,
            Pageable pageable) {
        boolean hasType = filter != null && filter.getTransactionType() != null;
        boolean hasDateRange = filter != null && filter.getFromDate() != null && filter.getToDate() != null;

        if (hasType && hasDateRange) {
            return walletTransactionRepository.findByCompanyIdAndTypeAndDateRange(
                    companyId, filter.getTransactionType(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasType) {
            return walletTransactionRepository.findByCompanyIdAndType(
                    companyId, filter.getTransactionType(), pageable);
        } else if (hasDateRange) {
            return walletTransactionRepository.findByCompanyIdAndDateRange(
                    companyId, filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return walletTransactionRepository.findByCompanyId(companyId, pageable);
        }
    }
}
