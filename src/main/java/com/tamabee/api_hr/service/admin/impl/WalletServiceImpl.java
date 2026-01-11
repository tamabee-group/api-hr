package com.tamabee.api_hr.service.admin.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.wallet.RefundRequest;
import com.tamabee.api_hr.dto.request.wallet.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.WalletOverviewResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletStatisticsResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletTransactionResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.WalletMapper;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.PlanRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.repository.wallet.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.interfaces.IWalletService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý ví tiền của công ty
 * Hỗ trợ xem thông tin ví, thêm/trừ số dư, hoàn tiền và thống kê
 */
@Slf4j
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
    private final SecurityUtil securityUtil;
    private final IEmailService emailService;

    // ==================== View Operations ====================

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getByCompanyId(Long companyId) {
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        PlanEntity plan = getPlanFromCompanyId(companyId);
        String planNameVi = plan != null ? plan.getNameVi() : null;
        String planNameEn = plan != null ? plan.getNameEn() : null;
        String planNameJa = plan != null ? plan.getNameJa() : null;
        return walletMapper.toResponse(wallet, planNameVi, planNameEn, planNameJa);
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

        // Auto-reactivate company nếu đang INACTIVE và balance đủ
        tryAutoReactivateCompany(companyId, balanceAfter);

        return walletTransactionMapper.toResponse(savedTransaction);
    }

    /**
     * Tự động reactivate company nếu đang INACTIVE và balance đủ để thanh toán plan
     * Trừ tiền subscription và ghi lịch sử transaction
     */
    private void tryAutoReactivateCompany(Long companyId, BigDecimal newBalance) {
        try {
            CompanyEntity company = companyRepository.findById(companyId).orElse(null);
            if (company == null || company.getStatus() != CompanyStatus.INACTIVE) {
                return;
            }

            // Lấy giá plan
            PlanEntity plan = getPlanFromCompany(company);
            if (plan == null) {
                return;
            }

            BigDecimal monthlyPrice = plan.getMonthlyPrice();
            
            // Nếu balance đủ để thanh toán ít nhất 1 tháng, tự động reactivate
            if (newBalance.compareTo(monthlyPrice) >= 0) {
                // Lấy wallet để trừ tiền
                WalletEntity wallet = walletRepository.findByCompanyId(companyId).orElse(null);
                if (wallet == null) {
                    return;
                }

                // Trừ tiền subscription
                BigDecimal balanceBefore = wallet.getBalance();
                BigDecimal balanceAfter = balanceBefore.subtract(monthlyPrice);
                wallet.setBalance(balanceAfter);
                walletRepository.save(wallet);

                // Ghi lịch sử transaction với description theo language của company
                String language = company.getLanguage() != null ? company.getLanguage() : "vi";
                String planName = getPlanNameByLanguage(plan, language);
                String description = getReactivationBillingDescription(planName, language);
                WalletTransactionEntity transaction = walletTransactionMapper.createEntity(
                        wallet.getId(),
                        TransactionType.BILLING,
                        monthlyPrice,
                        balanceBefore,
                        balanceAfter,
                        description,
                        null);
                walletTransactionRepository.save(transaction);

                // Cập nhật nextBillingDate
                wallet.setLastBillingDate(java.time.LocalDateTime.now());
                wallet.setNextBillingDate(java.time.LocalDateTime.now().plusMonths(1));
                walletRepository.save(wallet);

                // Reactivate company
                company.setStatus(CompanyStatus.ACTIVE);
                company.setDeactivatedAt(null);
                companyRepository.save(company);
                
                log.info("Auto-reactivated company {} after deposit. Charged: {}, Balance after: {}", 
                        companyId, monthlyPrice, balanceAfter);
                
                // Gửi email thông báo reactivation
                String companyEmail = company.getEmail();
                String companyName = company.getName();
                emailService.sendReactivationNotification(companyEmail, companyName, balanceAfter, language);
            }
        } catch (Exception e) {
            log.error("Error auto-reactivating company {}: {}", companyId, e.getMessage());
        }
    }

    /**
     * Lấy tên plan theo ngôn ngữ
     */
    private String getPlanNameByLanguage(PlanEntity plan, String language) {
        if (plan == null) return "N/A";
        return switch (language) {
            case "vi" -> plan.getNameVi();
            case "ja" -> plan.getNameJa();
            default -> plan.getNameEn();
        };
    }

    /**
     * Lấy description cho transaction billing khi reactivate theo ngôn ngữ
     */
    private String getReactivationBillingDescription(String planName, String language) {
        return switch (language) {
            case "vi" -> "Thanh toán subscription khi kích hoạt lại: " + planName;
            case "ja" -> "再有効化時のサブスクリプション支払い: " + planName;
            default -> "Subscription payment on reactivation: " + planName;
        };
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

            // Lấy plan names theo các locale
            PlanEntity plan = getPlanFromCompany(company);
            String planNameVi = plan != null ? plan.getNameVi() : null;
            String planNameEn = plan != null ? plan.getNameEn() : null;
            String planNameJa = plan != null ? plan.getNameJa() : null;

            // Lấy tổng deposits và billings
            BigDecimal totalDeposits = walletTransactionRepository.sumDepositsByWalletId(wallet.getId());
            BigDecimal totalBillings = walletTransactionRepository.sumBillingsByWalletId(wallet.getId());

            return walletMapper.toOverviewResponse(wallet, companyName, planNameVi, planNameEn, planNameJa, totalDeposits, totalBillings);
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
        return securityUtil.getCurrentUserCompanyId();
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
     * Lấy plan entity từ company entity
     */
    private PlanEntity getPlanFromCompany(CompanyEntity company) {
        if (company == null || company.getPlanId() == null) {
            return null;
        }
        return planRepository.findByIdAndDeletedFalse(company.getPlanId()).orElse(null);
    }

    /**
     * Lấy plan entity từ companyId
     */
    private PlanEntity getPlanFromCompanyId(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        return getPlanFromCompany(company);
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
            return walletTransactionRepository.findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
                    walletId, filter.getTransactionType(), pageable);
        } else if (hasDateRange) {
            return walletTransactionRepository.findByWalletIdAndDateRange(
                    walletId, filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
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
