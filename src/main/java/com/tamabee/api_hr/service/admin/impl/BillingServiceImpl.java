package com.tamabee.api_hr.service.admin.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
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
import com.tamabee.api_hr.util.RegionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý billing tự động hàng tháng
 * Trừ tiền subscription từ wallet của company khi đến ngày billing
 * Sử dụng masterJdbcTemplate cho plan_change_history vì nằm trong master database
 */
@Service
@Slf4j
public class BillingServiceImpl implements IBillingService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final JdbcTemplate masterJdbcTemplate;
    private final ISettingService settingService;
    private final IEmailService emailService;
    private final WalletTransactionMapper walletTransactionMapper;
    private final ICommissionService commissionService;

    public BillingServiceImpl(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            CompanyRepository companyRepository,
            PlanRepository planRepository,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            ISettingService settingService,
            IEmailService emailService,
            WalletTransactionMapper walletTransactionMapper,
            ICommissionService commissionService) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.companyRepository = companyRepository;
        this.planRepository = planRepository;
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.settingService = settingService;
        this.emailService = emailService;
        this.walletTransactionMapper = walletTransactionMapper;
        this.commissionService = commissionService;
    }

    @Override
    @Transactional
    public void processMonthlyBilling() {
        LocalDateTime now = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
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

        return LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).isBefore(wallet.getFreeTrialEndDate());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime calculateFreeTrialEndDate(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        boolean hasReferral = company.getReferredByEmployeeId() != null;
        return calculateFreeTrialEndDate(company.getCreatedAt(), hasReferral);
    }

    @Override
    public LocalDateTime calculateFreeTrialEndDate(LocalDateTime companyCreatedAt, boolean hasReferral) {
        int freeTrialMonths = settingService.getFreeTrialMonths();
        int referralBonusMonths = hasReferral ? settingService.getReferralBonusMonths() : 0;
        int totalFreeMonths = freeTrialMonths + referralBonusMonths;

        // Tính theo billing cycles (tháng đầy đủ)
        // Nếu đăng ký giữa tháng, tháng đầu tiên không tính
        // First billing sẽ là ngày 1 của tháng sau khi hết trial
        LocalDate createdDate = companyCreatedAt.toLocalDate();
        
        // Tháng đầu tiên bắt đầu từ ngày 1 tháng sau
        LocalDate firstFullMonth = createdDate.plusMonths(1).withDayOfMonth(1);
        
        // Trial kết thúc sau totalFreeMonths tháng đầy đủ
        // Ví dụ: đăng ký 15/01, free 3 tháng
        // First full month: 01/02
        // Trial end: 01/05 (sau 3 tháng đầy đủ: 02, 03, 04)
        LocalDate trialEndDate = firstFullMonth.plusMonths(totalFreeMonths);
        
        // Trả về 00:00:00 của ngày billing đầu tiên
        return trialEndDate.atStartOfDay();
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

        // Apply scheduled plan nếu có (downgrade)
        applyScheduledPlanIfDue(company, now);

        // Lấy thông tin plan hiện tại
        PlanEntity plan = null;
        if (company.getPlanId() != null) {
            plan = planRepository.findByIdAndDeletedFalse(company.getPlanId()).orElse(null);
        }

        if (plan == null) {
            log.warn("Company {} không có plan, bỏ qua billing", companyId);
            return;
        }

        // Tính billing amount theo plan cao nhất trong kỳ (chống gian lận)
        LocalDate periodStart = now.toLocalDate().withDayOfMonth(1).minusMonths(1);
        LocalDate periodEnd = now.toLocalDate().withDayOfMonth(1).minusDays(1);
        BigDecimal billingAmount = calculateBillingAmount(companyId, plan, periodStart, periodEnd);
        
        BigDecimal currentBalance = wallet.getBalance();
        String language = company.getLanguage() != null ? company.getLanguage() : "vi";

        // Kiểm tra số dư
        if (currentBalance.compareTo(billingAmount) < 0) {
            // Số dư không đủ
            handleInsufficientBalance(wallet, company, plan, billingAmount, currentBalance, language);
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

        // Tạo transaction record với description theo language
        String planName = getPlanName(plan, language);
        String description = getBillingDescription(planName, billingAmount, plan.getMonthlyPrice(), language);
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
                planName,
                billingAmount,
                balanceAfter,
                language);

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

        log.info("Billing thành công cho company {}: {} -> {} (amount: {})", 
                companyId, currentBalance, balanceAfter, billingAmount);
    }

    /**
     * Apply scheduled plan nếu đến ngày hiệu lực
     */
    private void applyScheduledPlanIfDue(CompanyEntity company, LocalDateTime now) {
        if (company.getScheduledPlanId() == null || company.getScheduledPlanEffectiveDate() == null) {
            return;
        }

        LocalDate today = now.toLocalDate();
        if (!today.isBefore(company.getScheduledPlanEffectiveDate())) {
            Long oldPlanId = company.getPlanId();
            Long newPlanId = company.getScheduledPlanId();
            
            // Lấy giá plan cũ và mới để log
            BigDecimal oldPrice = BigDecimal.ZERO;
            BigDecimal newPrice = BigDecimal.ZERO;
            
            if (oldPlanId != null) {
                PlanEntity oldPlan = planRepository.findByIdAndDeletedFalse(oldPlanId).orElse(null);
                if (oldPlan != null) {
                    oldPrice = oldPlan.getMonthlyPrice();
                }
            }
            
            PlanEntity newPlan = planRepository.findByIdAndDeletedFalse(newPlanId).orElse(null);
            if (newPlan != null) {
                newPrice = newPlan.getMonthlyPrice();
            }
            
            // Apply scheduled plan
            company.setPlanId(newPlanId);
            company.setScheduledPlanId(null);
            company.setScheduledPlanEffectiveDate(null);
            companyRepository.save(company);
            
            // Log plan change
            logPlanChange(company.getId(), oldPlanId, newPlanId, oldPrice, newPrice, "SCHEDULED_APPLY", today);
            
            log.info("Applied scheduled plan for company {}: {} -> {}", 
                    company.getId(), oldPlanId, newPlanId);
        }
    }

    /**
     * Tính billing amount theo plan cao nhất trong kỳ (chống gian lận)
     * Query trực tiếp master database vì plan_change_history nằm trong master
     */
    private BigDecimal calculateBillingAmount(Long companyId, PlanEntity currentPlan, 
                                              LocalDate periodStart, LocalDate periodEnd) {
        // Lấy giá plan cao nhất trong kỳ từ lịch sử thay đổi (master database)
        String sql = """
            SELECT MAX(GREATEST(COALESCE(from_plan_price, 0), COALESCE(to_plan_price, 0)))
            FROM plan_change_history
            WHERE company_id = ?
              AND billing_period_start <= ?
              AND billing_period_end >= ?
            """;
        
        BigDecimal maxPriceInPeriod = masterJdbcTemplate.queryForObject(
                sql, BigDecimal.class, companyId, periodEnd, periodStart);
        
        BigDecimal currentPrice = currentPlan.getMonthlyPrice();
        
        if (maxPriceInPeriod == null || maxPriceInPeriod.compareTo(BigDecimal.ZERO) == 0) {
            // Không có thay đổi plan trong kỳ, dùng giá hiện tại
            return currentPrice;
        }
        
        // Trả về giá cao nhất giữa plan cao nhất trong kỳ và plan hiện tại
        BigDecimal billingAmount = maxPriceInPeriod.max(currentPrice);
        
        if (billingAmount.compareTo(currentPrice) > 0) {
            log.info("Company {} bị tính theo plan cao nhất trong kỳ: {} (thay vì {})", 
                    companyId, billingAmount, currentPrice);
        }
        
        return billingAmount;
    }

    /**
     * Ghi log thay đổi plan vào master database
     */
    private void logPlanChange(Long companyId, Long fromPlanId, Long toPlanId,
                               BigDecimal fromPrice, BigDecimal toPrice,
                               String changeType, LocalDate effectiveDate) {
        LocalDate billingPeriodStart = effectiveDate.withDayOfMonth(1);
        LocalDate billingPeriodEnd = effectiveDate.withDayOfMonth(effectiveDate.lengthOfMonth());
        
        String sql = """
            INSERT INTO plan_change_history 
            (company_id, from_plan_id, to_plan_id, from_plan_price, to_plan_price, 
             change_type, effective_date, billing_period_start, billing_period_end, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        
        masterJdbcTemplate.update(sql, companyId, fromPlanId, toPlanId, fromPrice, toPrice,
                changeType, effectiveDate, billingPeriodStart, billingPeriodEnd);
    }

    /**
     * Xử lý trường hợp số dư không đủ
     */
    private void handleInsufficientBalance(WalletEntity wallet, CompanyEntity company,
            PlanEntity plan, BigDecimal billingAmount, BigDecimal currentBalance, String language) {

        // Ghi transaction thất bại với description theo language
        String planName = getPlanName(plan, language);
        String description = getBillingFailedDescription(planName, language);
        WalletTransactionEntity transaction = walletTransactionMapper.createEntity(
                wallet.getId(),
                TransactionType.BILLING_FAILED,
                billingAmount,
                currentBalance,
                currentBalance, // balance không đổi
                description,
                null);
        walletTransactionRepository.save(transaction);

        // Đánh dấu company là INACTIVE và set thời điểm deactivate
        company.setStatus(CompanyStatus.INACTIVE);
        company.setDeactivatedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        companyRepository.save(company);

        // Gửi email thông báo
        emailService.sendInsufficientBalance(
                company.getEmail(),
                company.getName(),
                planName,
                billingAmount,
                currentBalance,
                language);

        log.warn("Company {} bị đánh dấu INACTIVE do số dư không đủ. Cần: {}, Có: {}",
                company.getId(), billingAmount, currentBalance);
    }

    /**
     * Lấy tên plan theo ngôn ngữ
     */
    private String getPlanName(PlanEntity plan, String language) {
        if (plan == null) {
            return "N/A";
        }

        return switch (language) {
            case "vi" -> plan.getNameVi();
            case "ja" -> plan.getNameJa();
            default -> plan.getNameEn();
        };
    }

    /**
     * Lấy description cho transaction billing theo ngôn ngữ
     */
    private String getBillingDescription(String planName, BigDecimal billingAmount, 
                                         BigDecimal currentPlanPrice, String language) {
        // Nếu billing amount cao hơn giá plan hiện tại, ghi chú thêm
        boolean hasUpgradeCharge = billingAmount.compareTo(currentPlanPrice) > 0;
        
        if (hasUpgradeCharge) {
            return switch (language) {
                case "vi" -> "Thanh toán subscription: " + planName + " (tính theo plan cao nhất trong kỳ)";
                case "ja" -> "サブスクリプション支払い: " + planName + " (期間中の最高プランで計算)";
                default -> "Subscription payment: " + planName + " (charged at highest plan used in period)";
            };
        }
        
        return switch (language) {
            case "vi" -> "Thanh toán subscription: " + planName;
            case "ja" -> "サブスクリプション支払い: " + planName;
            default -> "Subscription payment: " + planName;
        };
    }

    /**
     * Lấy description cho transaction billing thất bại theo ngôn ngữ
     */
    private String getBillingFailedDescription(String planName, String language) {
        return switch (language) {
            case "vi" -> "Thanh toán thất bại - Số dư không đủ: " + planName;
            case "ja" -> "支払い失敗 - 残高不足: " + planName;
            default -> "Payment failed - Insufficient balance: " + planName;
        };
    }
}
