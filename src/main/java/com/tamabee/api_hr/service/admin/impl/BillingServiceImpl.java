package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.wallet.PlanRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.repository.wallet.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.interfaces.IBillingService;
import com.tamabee.api_hr.service.admin.interfaces.ICommissionService;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý billing tự động hàng tháng
 * Trừ tiền subscription từ wallet của company khi đến ngày billing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements IBillingService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final ISettingService settingService;
    private final IEmailService emailService;
    private final WalletTransactionMapper walletTransactionMapper;
    private final ICommissionService commissionService;

    @Override
    @Transactional
    public void processMonthlyBilling() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Bắt đầu xử lý billing hàng tháng tại: {}", now);

        // Lấy danh sách wallets cần billing
        List<WalletEntity> walletsDue = walletRepository.findWalletsDueForBilling(now);
        log.info("Tìm thấy {} wallets cần billing", walletsDue.size());

        int successCount = 0;
        int failCount = 0;

        for (WalletEntity wallet : walletsDue) {
            try {
                processSingleBilling(wallet, now);
                successCount++;
            } catch (Exception e) {
                log.error("Lỗi khi xử lý billing cho companyId {}: {}", wallet.getCompanyId(), e.getMessage());
                failCount++;
            }
        }

        log.info("Hoàn thành billing: {} thành công, {} thất bại", successCount, failCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInFreeTrial(Long companyId) {
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        if (wallet.getFreeTrialEndDate() == null) {
            return false;
        }

        return LocalDateTime.now().isBefore(wallet.getFreeTrialEndDate());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime calculateFreeTrialEndDate(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        boolean hasReferral = company.getReferredByEmployee() != null;
        return calculateFreeTrialEndDate(company.getCreatedAt(), hasReferral);
    }

    @Override
    public LocalDateTime calculateFreeTrialEndDate(LocalDateTime companyCreatedAt, boolean hasReferral) {
        int freeTrialMonths = settingService.getFreeTrialMonths();
        int referralBonusMonths = hasReferral ? settingService.getReferralBonusMonths() : 0;

        int totalFreeMonths = freeTrialMonths + referralBonusMonths;
        return companyCreatedAt.plusMonths(totalFreeMonths);
    }

    /**
     * Xử lý billing cho một wallet cụ thể
     */
    private void processSingleBilling(WalletEntity wallet, LocalDateTime now) {
        Long companyId = wallet.getCompanyId();

        // Lấy thông tin company
        CompanyEntity company = companyRepository.findById(companyId).orElse(null);
        if (company == null || company.getDeleted()) {
            log.warn("Company {} không tồn tại hoặc đã bị xóa, bỏ qua billing", companyId);
            return;
        }

        // Lấy thông tin plan
        PlanEntity plan = null;
        if (company.getPlanId() != null) {
            plan = planRepository.findByIdAndDeletedFalse(company.getPlanId()).orElse(null);
        }

        if (plan == null) {
            log.warn("Company {} không có plan, bỏ qua billing", companyId);
            return;
        }

        BigDecimal billingAmount = plan.getMonthlyPrice();
        BigDecimal currentBalance = wallet.getBalance();

        // Kiểm tra số dư
        if (currentBalance.compareTo(billingAmount) < 0) {
            // Số dư không đủ
            handleInsufficientBalance(wallet, company, plan, billingAmount, currentBalance);
            return;
        }

        // Trừ tiền và tạo transaction
        BigDecimal balanceAfter = currentBalance.subtract(billingAmount);
        wallet.setBalance(balanceAfter);
        wallet.setLastBillingDate(now);
        wallet.setNextBillingDate(now.plusMonths(1));
        // Cập nhật total billing cho commission eligibility
        wallet.setTotalBilling(wallet.getTotalBilling().add(billingAmount));
        walletRepository.save(wallet);

        // Tạo transaction record
        String description = "Thanh toán subscription: " + getPlanName(plan, company.getLanguage());
        WalletTransactionEntity transaction = walletTransactionMapper.createEntity(
                wallet.getId(),
                TransactionType.BILLING,
                billingAmount,
                currentBalance,
                balanceAfter,
                description,
                null);
        walletTransactionRepository.save(transaction);

        // Gửi email thông báo
        emailService.sendBillingNotification(
                company.getEmail(),
                company.getName(),
                getPlanName(plan, company.getLanguage()),
                billingAmount,
                balanceAfter,
                company.getLanguage());

        // Xử lý hoa hồng giới thiệu (nếu có)
        // Commission chỉ được tính cho lần thanh toán đầu tiên của company được giới
        // thiệu
        try {
            commissionService.processCommission(companyId);
        } catch (Exception e) {
            // Log lỗi nhưng không fail billing
            log.warn("Lỗi khi xử lý commission cho company {}: {}", companyId, e.getMessage());
        }

        // Recalculate commission eligibility sau mỗi billing
        try {
            commissionService.recalculateOnBilling(companyId);
        } catch (Exception e) {
            // Log lỗi nhưng không fail billing
            log.warn("Lỗi khi recalculate commission eligibility cho company {}: {}", companyId, e.getMessage());
        }

        log.info("Billing thành công cho company {}: {} -> {}", companyId, currentBalance, balanceAfter);
    }

    /**
     * Xử lý trường hợp số dư không đủ
     */
    private void handleInsufficientBalance(WalletEntity wallet, CompanyEntity company,
            PlanEntity plan, BigDecimal billingAmount, BigDecimal currentBalance) {

        // Đánh dấu company là INACTIVE
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);

        // Gửi email thông báo
        emailService.sendInsufficientBalance(
                company.getEmail(),
                company.getName(),
                getPlanName(plan, company.getLanguage()),
                billingAmount,
                currentBalance,
                company.getLanguage());

        log.warn("Company {} bị đánh dấu INACTIVE do số dư không đủ. Cần: {}, Có: {}",
                company.getId(), billingAmount, currentBalance);
    }

    /**
     * Lấy tên plan theo ngôn ngữ
     */
    private String getPlanName(PlanEntity plan, String language) {
        if (plan == null)
            return "N/A";

        return switch (language) {
            case "vi" -> plan.getNameVi();
            case "ja" -> plan.getNameJa();
            default -> plan.getNameEn();
        };
    }
}
