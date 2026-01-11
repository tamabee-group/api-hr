package com.tamabee.api_hr.service.company.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.response.PlanEligibilityResponse;
import com.tamabee.api_hr.dto.response.SubscriptionStatusResponse;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.company.interfaces.ISubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý subscription của company
 * Sử dụng masterJdbcTemplate vì companies, plans, wallets nằm trong master database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements ISubscriptionService {

    @Qualifier("masterJdbcTemplate")
    private final JdbcTemplate masterJdbcTemplate;
    private final UserRepository userRepository;
    private final ISettingService settingService;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(String language) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Lấy thông tin company và wallet
        String sql = """
            SELECT c.id, c.name, c.status, c.plan_id, c.deactivated_at,
                   p.id as plan_id, p.name_vi, p.name_en, p.name_ja, 
                   p.description_vi, p.description_en, p.description_ja,
                   p.monthly_price, p.max_employees, p.is_active,
                   w.balance, w.free_trial_end_date, w.next_billing_date
            FROM companies c
            LEFT JOIN plans p ON c.plan_id = p.id AND p.deleted = false
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
        
        // Kiểm tra plan mới có tồn tại và active không
        String checkPlanSql = """
            SELECT id, max_employees, is_active, name_vi, name_en, name_ja
            FROM plans WHERE id = ? AND deleted = false
            """;
        
        List<Object[]> planResult = masterJdbcTemplate.query(checkPlanSql, (rs, rowNum) -> new Object[]{
            rs.getLong("id"),
            rs.getObject("max_employees", Integer.class),
            rs.getBoolean("is_active"),
            rs.getString("name_" + language)
        }, newPlanId);
        
        if (planResult.isEmpty()) {
            throw NotFoundException.plan(newPlanId);
        }
        
        Object[] plan = planResult.get(0);
        Boolean isActive = (Boolean) plan[2];
        Integer maxEmployees = (Integer) plan[1];
        String planName = (String) plan[3];
        
        if (!isActive) {
            throw BadRequestException.planNotActive();
        }
        
        // Kiểm tra số nhân viên
        int employeeCount = countActiveEmployees();
        if (maxEmployees != null && maxEmployees > 0 && employeeCount > maxEmployees) {
            throw BadRequestException.planExceedsEmployeeLimit(employeeCount, maxEmployees);
        }
        
        // Cập nhật plan
        String updateSql = """
            UPDATE companies SET plan_id = ?, updated_at = NOW()
            WHERE tenant_domain = ? AND deleted = false
            """;
        
        int updated = masterJdbcTemplate.update(updateSql, newPlanId, tenantDomain);
        if (updated == 0) {
            throw NotFoundException.company(tenantDomain);
        }
        
        log.info("Company {} đã đổi plan sang {} (ID: {})", tenantDomain, planName, newPlanId);
        
        return getSubscriptionStatus(language);
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
        
        // Wallet info
        BigDecimal walletBalance = rs.getBigDecimal("balance");
        LocalDateTime freeTrialEndDate = toLocalDateTime(rs.getTimestamp("free_trial_end_date"));
        LocalDateTime nextBillingDate = toLocalDateTime(rs.getTimestamp("next_billing_date"));
        
        int employeeCount = countActiveEmployees();
        boolean isInFreeTrial = freeTrialEndDate != null && LocalDateTime.now().isBefore(freeTrialEndDate);
        
        // Tính số ngày còn lại trước khi bị xóa (nếu INACTIVE)
        Integer daysUntilDeletion = null;
        if (companyStatus == CompanyStatus.INACTIVE && deactivatedAt != null) {
            int retentionDays = settingService.getInactiveRetentionDays();
            LocalDateTime deletionDate = deactivatedAt.plusDays(retentionDays);
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), deletionDate);
            daysUntilDeletion = Math.max(0, (int) daysLeft);
        }
        
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
                .availablePlans(availablePlans)
                .build();
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
}
