package com.tamabee.api_hr.service.company.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.response.PlanChangeHistoryResponse;
import com.tamabee.api_hr.dto.response.PlanEligibilityResponse;
import com.tamabee.api_hr.dto.response.SubscriptionStatusResponse;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.company.interfaces.ISubscriptionService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý subscription của company
 * Sử dụng masterJdbcTemplate vì companies, plans, wallets nằm trong master database
 */
@Service
@Slf4j
public class SubscriptionServiceImpl implements ISubscriptionService {

    // Grace period cho phép hủy upgrade (10 ngày hoặc đến cuối tháng nếu không đủ 10 ngày)
    private static final int UPGRADE_GRACE_PERIOD_DAYS = 10;
    // Giới hạn số lần đổi plan trong 1 ngày
    private static final int MAX_PLAN_CHANGES_PER_DAY = 3;

    private final JdbcTemplate masterJdbcTemplate;
    private final UserRepository userRepository;
    private final ISettingService settingService;

    public SubscriptionServiceImpl(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            UserRepository userRepository,
            ISettingService settingService) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.userRepository = userRepository;
        this.settingService = settingService;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy thông tin company, wallet và scheduled plan
        String sql = """
            SELECT c.id, c.name, c.status, c.plan_id, c.deactivated_at,
                   c.scheduled_plan_id, c.scheduled_plan_effective_date,
                   p.id as plan_id, p.name_vi, p.name_en, p.name_ja, 
                   p.description_vi, p.description_en, p.description_ja,
                   p.monthly_price, p.max_employees, p.is_active,
                   sp.name_vi as sp_name_vi, sp.name_en as sp_name_en, sp.name_ja as sp_name_ja,
                   sp.monthly_price as sp_monthly_price,
                   w.balance, w.free_trial_end_date, w.next_billing_date
            FROM companies c
            LEFT JOIN plans p ON c.plan_id = p.id AND p.deleted = false
            LEFT JOIN plans sp ON c.scheduled_plan_id = sp.id AND sp.deleted = false
            LEFT JOIN wallets w ON w.company_id = c.id AND w.deleted = false
            WHERE c.tenant_domain = ? AND c.deleted = false
            """;

        return masterJdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                throw NotFoundException.company(tenantDomain);
            }
            return buildSubscriptionStatus(rs, language);
        }, tenantDomain);
    }

    @Override
    @Transactional
    public SubscriptionStatusResponse changePlan(Long newPlanId, String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy thông tin company hiện tại
        String companySql = """
            SELECT c.id, c.plan_id, c.status,
                   p.monthly_price as current_price,
                   w.free_trial_end_date
            FROM companies c
            LEFT JOIN plans p ON c.plan_id = p.id AND p.deleted = false
            LEFT JOIN wallets w ON w.company_id = c.id AND w.deleted = false
            WHERE c.tenant_domain = ? AND c.deleted = false
            """;
        
        Object[] companyInfo = masterJdbcTemplate.query(companySql, rs -> {
            if (!rs.next()) {
                throw NotFoundException.company(tenantDomain);
            }
            return new Object[]{
                rs.getLong("id"),
                rs.getObject("plan_id", Long.class),
                rs.getString("status"),
                rs.getBigDecimal("current_price"),
                toLocalDateTime(rs.getTimestamp("free_trial_end_date"))
            };
        }, tenantDomain);
        
        Long companyId = (Long) companyInfo[0];
        Long currentPlanId = (Long) companyInfo[1];
        BigDecimal currentPrice = (BigDecimal) companyInfo[3];
        LocalDateTime freeTrialEndDate = (LocalDateTime) companyInfo[4];
        
        // Kiểm tra spam: không quá 3 lần đổi plan trong 1 ngày
        checkPlanChangeSpam(companyId);
        
        // Kiểm tra plan mới có tồn tại và active không
        String checkPlanSql = """
            SELECT id, max_employees, is_active, monthly_price, name_vi, name_en, name_ja
            FROM plans WHERE id = ? AND deleted = false
            """;
        
        List<Object[]> planResult = masterJdbcTemplate.query(checkPlanSql, (rs, rowNum) -> new Object[]{
            rs.getLong("id"),
            rs.getObject("max_employees", Integer.class),
            rs.getBoolean("is_active"),
            rs.getBigDecimal("monthly_price"),
            rs.getString("name_" + language)
        }, newPlanId);
        
        if (planResult.isEmpty()) {
            throw NotFoundException.plan(newPlanId);
        }
        
        Object[] newPlan = planResult.get(0);
        Boolean isActive = (Boolean) newPlan[2];
        Integer maxEmployees = (Integer) newPlan[1];
        BigDecimal newPrice = (BigDecimal) newPlan[3];
        String planName = (String) newPlan[4];
        
        if (!isActive) {
            throw BadRequestException.planNotActive();
        }
        
        // Kiểm tra số nhân viên
        int employeeCount = countActiveEmployees();
        if (maxEmployees != null && maxEmployees > 0 && employeeCount > maxEmployees) {
            throw BadRequestException.planExceedsEmployeeLimit(employeeCount, maxEmployees);
        }
        
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        boolean isInTrial = freeTrialEndDate != null
                && LocalDateTime.now(RegionUtil.getTimezone(
                        RegionContext.getCurrentRegion()))
                        .isBefore(freeTrialEndDate);
        
        // Xác định loại thay đổi
        if (isInTrial) {
            // Trong trial period - thay đổi ngay, không tính tiền
            updateCompanyPlan(companyId, newPlanId, tenantDomain);
            logPlanChange(companyId, currentPlanId, newPlanId, currentPrice, newPrice, "TRIAL_CHANGE", today);
            log.info("Company {} đổi plan trong trial: {} -> {} (ID: {})", tenantDomain, currentPlanId, planName, newPlanId);
        } else {
            // So sánh giá plan
            int priceCompare = newPrice.compareTo(currentPrice != null ? currentPrice : BigDecimal.ZERO);
            
            if (priceCompare > 0) {
                // UPGRADE - có hiệu lực ngay
                updateCompanyPlan(companyId, newPlanId, tenantDomain);
                logPlanChange(companyId, currentPlanId, newPlanId, currentPrice, newPrice, "UPGRADE", today);
                log.info("Company {} UPGRADE plan: {} -> {} (ID: {})", tenantDomain, currentPlanId, planName, newPlanId);
            } else if (priceCompare < 0) {
                // DOWNGRADE - có hiệu lực từ ngày 1 tháng sau
                LocalDate nextBillingDate = today.plusMonths(1).withDayOfMonth(1);
                scheduleDowngrade(companyId, newPlanId, nextBillingDate, tenantDomain);
                logPlanChange(companyId, currentPlanId, newPlanId, currentPrice, newPrice, "DOWNGRADE", nextBillingDate);
                log.info("Company {} DOWNGRADE scheduled: {} -> {} (ID: {}), effective: {}", 
                        tenantDomain, currentPlanId, planName, newPlanId, nextBillingDate);
            } else {
                // Cùng giá - thay đổi ngay
                updateCompanyPlan(companyId, newPlanId, tenantDomain);
                log.info("Company {} đổi plan cùng giá: {} -> {} (ID: {})", tenantDomain, currentPlanId, planName, newPlanId);
            }
        }
        
        return getSubscriptionStatus(language);
    }

    /**
     * Cập nhật plan của company ngay lập tức
     */
    private void updateCompanyPlan(Long companyId, Long newPlanId, String tenantDomain) {
        String updateSql = """
            UPDATE companies SET plan_id = ?, scheduled_plan_id = NULL, 
                   scheduled_plan_effective_date = NULL, updated_at = NOW()
            WHERE id = ? AND deleted = false
            """;
        masterJdbcTemplate.update(updateSql, newPlanId, companyId);
    }

    /**
     * Kiểm tra spam: không quá 3 lần đổi plan trong 1 ngày
     */
    private void checkPlanChangeSpam(Long companyId) {
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        String sql = """
            SELECT COUNT(*) FROM plan_change_history
            WHERE company_id = ? AND created_at BETWEEN ? AND ?
            """;
        
        Integer count = masterJdbcTemplate.queryForObject(sql, Integer.class, companyId, startOfDay, endOfDay);
        
        if (count != null && count >= MAX_PLAN_CHANGES_PER_DAY) {
            throw BadRequestException.planChangeSpamDetected();
        }
    }

    /**
     * Lên lịch downgrade cho kỳ billing tiếp theo
     */
    private void scheduleDowngrade(Long companyId, Long newPlanId, LocalDate effectiveDate, String tenantDomain) {
        String updateSql = """
            UPDATE companies SET scheduled_plan_id = ?, scheduled_plan_effective_date = ?, updated_at = NOW()
            WHERE id = ? AND deleted = false
            """;
        masterJdbcTemplate.update(updateSql, newPlanId, effectiveDate, companyId);
    }

    /**
     * Ghi log thay đổi plan vào master database
     */
    private void logPlanChange(Long companyId, Long fromPlanId, Long toPlanId, 
                               BigDecimal fromPrice, BigDecimal toPrice, 
                               String changeType, LocalDate effectiveDate) {
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        LocalDate billingPeriodStart = today.withDayOfMonth(1);
        LocalDate billingPeriodEnd = today.withDayOfMonth(today.lengthOfMonth());
        
        String sql = """
            INSERT INTO plan_change_history 
            (company_id, from_plan_id, to_plan_id, from_plan_price, to_plan_price, 
             change_type, effective_date, billing_period_start, billing_period_end, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        
        masterJdbcTemplate.update(sql, companyId, fromPlanId, toPlanId, fromPrice, toPrice,
                changeType, effectiveDate, billingPeriodStart, billingPeriodEnd);
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveEmployees() {
        // Trong multi-tenant, userRepository đã được filter theo tenant
        return (int) userRepository.countByDeletedFalse();
    }

    /**
     * Build SubscriptionStatusResponse từ ResultSet
     */
    private SubscriptionStatusResponse buildSubscriptionStatus(ResultSet rs, String language) throws SQLException {
        Long companyId = rs.getLong("id");
        String companyName = rs.getString("name");
        CompanyStatus companyStatus = CompanyStatus.valueOf(rs.getString("status"));
        Long planId = rs.getObject("plan_id", Long.class);
        LocalDateTime deactivatedAt = toLocalDateTime(rs.getTimestamp("deactivated_at"));
        
        // Plan info
        String planName = getPlanName(rs, language);
        BigDecimal planPrice = rs.getBigDecimal("monthly_price");
        Integer maxEmployees = rs.getObject("max_employees", Integer.class);
        
        // Scheduled plan info (downgrade)
        Long scheduledPlanId = rs.getObject("scheduled_plan_id", Long.class);
        String scheduledPlanName = null;
        BigDecimal scheduledPlanPrice = null;
        String scheduledPlanEffectiveDate = null;
        
        if (scheduledPlanId != null) {
            scheduledPlanName = getScheduledPlanName(rs, language);
            scheduledPlanPrice = rs.getBigDecimal("sp_monthly_price");
            java.sql.Date effectiveDate = rs.getDate("scheduled_plan_effective_date");
            if (effectiveDate != null) {
                scheduledPlanEffectiveDate = effectiveDate.toLocalDate().toString();
            }
        }
        
        // Wallet info
        BigDecimal walletBalance = rs.getBigDecimal("balance");
        LocalDateTime freeTrialEndDate = toLocalDateTime(rs.getTimestamp("free_trial_end_date"));
        LocalDateTime nextBillingDate = toLocalDateTime(rs.getTimestamp("next_billing_date"));
        
        int employeeCount = countActiveEmployees();
        boolean isInFreeTrial = freeTrialEndDate != null
                && LocalDateTime.now(RegionUtil.getTimezone(
                        RegionContext.getCurrentRegion()))
                        .isBefore(freeTrialEndDate);
        
        // Tính số ngày còn lại trước khi bị xóa (nếu INACTIVE)
        Integer daysUntilDeletion = null;
        if (companyStatus == CompanyStatus.INACTIVE && deactivatedAt != null) {
            int retentionDays = settingService.getInactiveRetentionDays();
            LocalDateTime deletionDate = deactivatedAt.plusDays(retentionDays);
            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDateTime.now(RegionUtil.getTimezone(
                            RegionContext.getCurrentRegion())),
                    deletionDate);
            daysUntilDeletion = Math.max(0, (int) daysLeft);
        }
        
        // Kiểm tra grace period cho upgrade cancellation
        UpgradeGraceInfo graceInfo = checkUpgradeGracePeriod(companyId, language);
        
        // Lấy danh sách plans
        List<PlanEligibilityResponse> availablePlans = getAvailablePlans(employeeCount, language);
        
        return SubscriptionStatusResponse.builder()
                .companyId(companyId)
                .companyName(companyName)
                .companyStatus(companyStatus.name())
                .currentEmployeeCount(employeeCount)
                .currentPlanId(planId)
                .currentPlanName(planName)
                .currentPlanPrice(planPrice)
                .currentPlanMaxEmployees(maxEmployees)
                .walletBalance(walletBalance != null ? walletBalance : BigDecimal.ZERO)
                .freeTrialEndDate(freeTrialEndDate)
                .nextBillingDate(nextBillingDate)
                .isInFreeTrial(isInFreeTrial)
                .daysUntilDeletion(daysUntilDeletion)
                .scheduledPlanId(scheduledPlanId)
                .scheduledPlanName(scheduledPlanName)
                .scheduledPlanPrice(scheduledPlanPrice)
                .scheduledPlanEffectiveDate(scheduledPlanEffectiveDate)
                .canCancelUpgrade(graceInfo.canCancel)
                .cancelUpgradeDeadline(graceInfo.deadline)
                .previousPlanId(graceInfo.previousPlanId)
                .previousPlanName(graceInfo.previousPlanName)
                .availablePlans(availablePlans)
                .build();
    }

    /**
     * Thông tin grace period cho upgrade
     */
    private record UpgradeGraceInfo(
            boolean canCancel,
            LocalDateTime deadline,
            Long previousPlanId,
            String previousPlanName
    ) {}

    /**
     * Tính deadline cho grace period: 10 ngày hoặc đến cuối tháng nếu không đủ 10 ngày
     */
    private LocalDateTime calculateGraceDeadline(LocalDateTime upgradeTime) {
        LocalDate upgradeDate = upgradeTime.toLocalDate();
        LocalDate endOfMonth = upgradeDate.withDayOfMonth(upgradeDate.lengthOfMonth());
        LocalDate gracePeriodEnd = upgradeDate.plusDays(UPGRADE_GRACE_PERIOD_DAYS);
        
        // Nếu 10 ngày vượt quá cuối tháng, lấy cuối tháng
        LocalDate deadline = gracePeriodEnd.isAfter(endOfMonth) ? endOfMonth : gracePeriodEnd;
        return deadline.atTime(23, 59, 59);
    }

    /**
     * Kiểm tra xem có upgrade gần đây trong grace period không
     * Bao gồm cả UPGRADE và TRIAL_CHANGE (khi giá mới > giá cũ)
     */
    private UpgradeGraceInfo checkUpgradeGracePeriod(Long companyId, String language) {
        // Lấy upgrade gần nhất trong tháng hiện tại
        LocalDate startOfMonth = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).withDayOfMonth(1);
        
        String sql = """
            SELECT h.id, h.from_plan_id, h.from_plan_price, h.to_plan_price, h.change_type, h.created_at,
                   p.name_vi, p.name_en, p.name_ja
            FROM plan_change_history h
            LEFT JOIN plans p ON h.from_plan_id = p.id
            WHERE h.company_id = ? 
              AND (h.change_type = 'UPGRADE' OR (h.change_type = 'TRIAL_CHANGE' AND h.to_plan_price > h.from_plan_price))
              AND h.created_at >= ?
            ORDER BY h.created_at DESC
            LIMIT 1
            """;
        
        List<UpgradeGraceInfo> result = masterJdbcTemplate.query(sql, (rs, rowNum) -> {
            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            LocalDateTime deadline = calculateGraceDeadline(createdAt);
            
            // Kiểm tra còn trong grace period không
            if (LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).isAfter(deadline)) {
                return new UpgradeGraceInfo(false, null, null, null);
            }
            
            Long previousPlanId = rs.getObject("from_plan_id", Long.class);
            String previousPlanName = switch (language) {
                case "vi" -> rs.getString("name_vi");
                case "ja" -> rs.getString("name_ja");
                default -> rs.getString("name_en");
            };
            
            return new UpgradeGraceInfo(true, deadline, previousPlanId, previousPlanName);
        }, companyId, startOfMonth.atStartOfDay());
        
        if (result.isEmpty()) {
            return new UpgradeGraceInfo(false, null, null, null);
        }
        
        return result.get(0);
    }

    /**
     * Lấy tên scheduled plan theo ngôn ngữ
     */
    private String getScheduledPlanName(ResultSet rs, String language) throws SQLException {
        return switch (language) {
            case "vi" -> rs.getString("sp_name_vi");
            case "ja" -> rs.getString("sp_name_ja");
            default -> rs.getString("sp_name_en");
        };
    }

    /**
     * Lấy danh sách plans với thông tin eligibility
     */
    private List<PlanEligibilityResponse> getAvailablePlans(int currentEmployeeCount, String language) {
        String sql = """
            SELECT id, name_vi, name_en, name_ja, description_vi, description_en, description_ja,
                   monthly_price, max_employees, is_active
            FROM plans
            WHERE deleted = false AND is_active = true
            ORDER BY monthly_price ASC
            """;
        
        return masterJdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer maxEmployees = rs.getObject("max_employees", Integer.class);
            boolean eligible = true;
            String ineligibleReason = null;
            
            if (maxEmployees != null && maxEmployees > 0 && currentEmployeeCount > maxEmployees) {
                eligible = false;
                ineligibleReason = "EXCEEDS_EMPLOYEE_LIMIT";
            }
            
            return PlanEligibilityResponse.builder()
                    .id(rs.getLong("id"))
                    .name(getPlanName(rs, language))
                    .description(getPlanDescription(rs, language))
                    .monthlyPrice(rs.getBigDecimal("monthly_price"))
                    .maxEmployees(maxEmployees)
                    .isActive(rs.getBoolean("is_active"))
                    .eligible(eligible)
                    .ineligibleReason(ineligibleReason)
                    .build();
        });
    }

    /**
     * Lấy tên plan theo ngôn ngữ
     */
    private String getPlanName(ResultSet rs, String language) throws SQLException {
        return switch (language) {
            case "vi" -> rs.getString("name_vi");
            case "ja" -> rs.getString("name_ja");
            default -> rs.getString("name_en");
        };
    }

    /**
     * Lấy mô tả plan theo ngôn ngữ
     */
    private String getPlanDescription(ResultSet rs, String language) throws SQLException {
        return switch (language) {
            case "vi" -> rs.getString("description_vi");
            case "ja" -> rs.getString("description_ja");
            default -> rs.getString("description_en");
        };
    }

    /**
     * Convert Timestamp sang LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    @Override
    @Transactional
    public SubscriptionStatusResponse reactivate(String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy thông tin company, plan và wallet
        String sql = """
            SELECT c.id, c.status, c.plan_id,
                   p.monthly_price,
                   w.balance
            FROM companies c
            LEFT JOIN plans p ON c.plan_id = p.id AND p.deleted = false
            LEFT JOIN wallets w ON w.company_id = c.id AND w.deleted = false
            WHERE c.tenant_domain = ? AND c.deleted = false
            """;
        
        Object[] result = masterJdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                throw NotFoundException.company(tenantDomain);
            }
            return new Object[]{
                rs.getLong("id"),
                rs.getString("status"),
                rs.getBigDecimal("monthly_price"),
                rs.getBigDecimal("balance")
            };
        }, tenantDomain);
        
        Long companyId = (Long) result[0];
        String status = (String) result[1];
        BigDecimal planPrice = (BigDecimal) result[2];
        BigDecimal balance = (BigDecimal) result[3];
        
        // Kiểm tra company đang INACTIVE
        if (!"INACTIVE".equals(status)) {
            throw BadRequestException.companyAlreadyActive();
        }
        
        // Kiểm tra balance đủ để thanh toán
        if (balance == null || planPrice == null || balance.compareTo(planPrice) < 0) {
            throw BadRequestException.insufficientBalance();
        }
        
        // Reactivate company
        String updateSql = """
            UPDATE companies SET status = 'ACTIVE', deactivated_at = NULL, updated_at = NOW()
            WHERE id = ? AND deleted = false
            """;
        
        masterJdbcTemplate.update(updateSql, companyId);
        log.info("Company {} đã được reactivate thủ công. Balance: {}, Plan price: {}", 
                tenantDomain, balance, planPrice);
        
        return getSubscriptionStatus(language);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanChangeHistoryResponse> getPlanChangeHistory(String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy company ID từ tenant domain
        String companyIdSql = "SELECT id FROM companies WHERE tenant_domain = ? AND deleted = false";
        Long companyId = masterJdbcTemplate.queryForObject(companyIdSql, Long.class, tenantDomain);
        
        if (companyId == null) {
            throw NotFoundException.company(tenantDomain);
        }
        
        // Lấy lịch sử thay đổi plan với tên plan
        String sql = """
            SELECT h.id, h.from_plan_id, h.to_plan_id, h.from_plan_price, h.to_plan_price,
                   h.change_type, h.effective_date, h.created_at,
                   fp.name_vi as from_name_vi, fp.name_en as from_name_en, fp.name_ja as from_name_ja,
                   tp.name_vi as to_name_vi, tp.name_en as to_name_en, tp.name_ja as to_name_ja
            FROM plan_change_history h
            LEFT JOIN plans fp ON h.from_plan_id = fp.id
            LEFT JOIN plans tp ON h.to_plan_id = tp.id
            WHERE h.company_id = ?
            ORDER BY h.created_at DESC
            """;
        
        return masterJdbcTemplate.query(sql, (rs, rowNum) -> {
            String fromPlanName = null;
            String toPlanName = null;
            
            // Lấy tên plan theo ngôn ngữ
            if (rs.getObject("from_plan_id") != null) {
                fromPlanName = switch (language) {
                    case "vi" -> rs.getString("from_name_vi");
                    case "ja" -> rs.getString("from_name_ja");
                    default -> rs.getString("from_name_en");
                };
            }
            
            toPlanName = switch (language) {
                case "vi" -> rs.getString("to_name_vi");
                case "ja" -> rs.getString("to_name_ja");
                default -> rs.getString("to_name_en");
            };
            
            return PlanChangeHistoryResponse.builder()
                    .id(rs.getLong("id"))
                    .fromPlanName(fromPlanName)
                    .toPlanName(toPlanName)
                    .fromPlanPrice(rs.getBigDecimal("from_plan_price"))
                    .toPlanPrice(rs.getBigDecimal("to_plan_price"))
                    .changeType(rs.getString("change_type"))
                    .effectiveDate(rs.getDate("effective_date").toLocalDate())
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        }, companyId);
    }

    @Override
    @Transactional
    public SubscriptionStatusResponse cancelUpgrade(String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy company ID
        String companyIdSql = "SELECT id FROM companies WHERE tenant_domain = ? AND deleted = false";
        Long companyId = masterJdbcTemplate.queryForObject(companyIdSql, Long.class, tenantDomain);
        
        if (companyId == null) {
            throw NotFoundException.company(tenantDomain);
        }
        
        // Lấy upgrade gần nhất trong tháng hiện tại (bao gồm cả TRIAL_CHANGE khi giá tăng)
        LocalDate startOfMonth = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).withDayOfMonth(1);
        
        String checkSql = """
            SELECT id, from_plan_id, from_plan_price, to_plan_price, created_at
            FROM plan_change_history
            WHERE company_id = ? 
              AND (change_type = 'UPGRADE' OR (change_type = 'TRIAL_CHANGE' AND to_plan_price > from_plan_price))
              AND created_at >= ?
            ORDER BY created_at DESC
            LIMIT 1
            """;
        
        List<Object[]> upgradeResult = masterJdbcTemplate.query(checkSql, (rs, rowNum) -> new Object[]{
            rs.getLong("id"),
            rs.getObject("from_plan_id", Long.class),
            rs.getTimestamp("created_at").toLocalDateTime()
        }, companyId, startOfMonth.atStartOfDay());
        
        if (upgradeResult.isEmpty()) {
            throw BadRequestException.upgradeGracePeriodExpired();
        }
        
        Long historyId = (Long) upgradeResult.get(0)[0];
        Long previousPlanId = (Long) upgradeResult.get(0)[1];
        LocalDateTime createdAt = (LocalDateTime) upgradeResult.get(0)[2];
        
        // Kiểm tra còn trong grace period không
        LocalDateTime deadline = calculateGraceDeadline(createdAt);
        if (LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).isAfter(deadline)) {
            throw BadRequestException.upgradeGracePeriodExpired();
        }
        
        if (previousPlanId == null) {
            throw BadRequestException.cannotCancelUpgrade();
        }
        
        // Revert về plan trước đó
        String updateCompanySql = """
            UPDATE companies SET plan_id = ?, updated_at = NOW()
            WHERE id = ? AND deleted = false
            """;
        masterJdbcTemplate.update(updateCompanySql, previousPlanId, companyId);
        
        // Xóa record upgrade khỏi history
        String deleteHistorySql = "DELETE FROM plan_change_history WHERE id = ?";
        masterJdbcTemplate.update(deleteHistorySql, historyId);
        
        log.info("Company {} đã hủy upgrade, revert về plan ID: {}", tenantDomain, previousPlanId);
        
        return getSubscriptionStatus(language);
    }
}
