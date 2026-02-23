package com.tamabee.api_hr.service.company.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.request.company.UpdateCompanyProfileRequest;
import com.tamabee.api_hr.dto.response.company.CompanyProfileResponse;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.ICompanyProfileService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho company profile.
 * Sử dụng masterJdbcTemplate vì table companies nằm trong master database.
 * Sử dụng userRepository để đếm employees từ tenant database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyProfileServiceImpl implements ICompanyProfileService {

    @Qualifier("masterJdbcTemplate")
    private final JdbcTemplate masterJdbcTemplate;
    
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getMyCompanyProfile() {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        String sql = """
            SELECT c.id, c.name, c.owner_name, c.email, c.phone, c.address, 
                   c.industry, c.zipcode, c.region, c.language, c.logo, 
                   c.tenant_domain, c.status, c.plan_id, c.created_at, c.updated_at,
                   p.name_vi, p.name_en, p.name_ja, p.monthly_price, p.max_employees,
                   w.balance, w.last_billing_date, w.next_billing_date, w.free_trial_end_date
            FROM companies c
            LEFT JOIN plans p ON c.plan_id = p.id AND p.deleted = false
            LEFT JOIN wallets w ON w.company_id = c.id AND w.deleted = false
            WHERE c.tenant_domain = ? AND c.deleted = false
            """;
        
        CompanyProfileResponse response = masterJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToResponse(rs);
            }
            throw NotFoundException.company(tenantDomain);
        }, tenantDomain);
        
        // Đếm số employees từ tenant database
        int employeeCount = (int) userRepository.countByDeletedFalse();
        response.setEmployeeCount(employeeCount);
        
        return response;
    }

    @Override
    @Transactional
    public CompanyProfileResponse updateCompanyProfile(UpdateCompanyProfileRequest request) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        // Kiểm tra email không trùng với company khác
        String checkEmailSql = """
            SELECT COUNT(*) FROM companies 
            WHERE email = ? AND tenant_domain != ? AND deleted = false
            """;
        Integer count = masterJdbcTemplate.queryForObject(checkEmailSql, Integer.class, 
                request.getEmail(), tenantDomain);
        if (count != null && count > 0) {
            throw ConflictException.emailExists(request.getEmail());
        }
        
        // Cập nhật thông tin
        String updateSql = """
            UPDATE companies SET 
                name = ?, owner_name = ?, email = ?, phone = ?, 
                address = ?, industry = ?, zipcode = ?, updated_at = NOW()
            WHERE tenant_domain = ? AND deleted = false
            """;
        
        int updated = masterJdbcTemplate.update(updateSql,
                request.getName(),
                request.getOwnerName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress(),
                request.getIndustry(),
                request.getZipcode(),
                tenantDomain);
        
        if (updated == 0) {
            throw NotFoundException.company(tenantDomain);
        }
        
        // Chỉ ADMIN_TAMABEE mới được thay đổi region/language
        if (request.getRegion() != null || request.getLanguage() != null) {
            boolean isTamabeeAdmin = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN_TAMABEE"));
            
            if (isTamabeeAdmin) {
                // Validate region nếu được cung cấp
                if (request.getRegion() != null && !RegionUtil.isValidRegion(request.getRegion())) {
                    throw BadRequestException.invalidRegion(request.getRegion());
                }

                StringBuilder regionSql = new StringBuilder("UPDATE companies SET ");
                java.util.List<Object> params = new java.util.ArrayList<>();
                boolean hasField = false;
                
                if (request.getRegion() != null) {
                    regionSql.append("region = ?");
                    params.add(request.getRegion());
                    hasField = true;
                }
                if (request.getLanguage() != null) {
                    if (hasField) {
                        regionSql.append(", ");
                    }
                    regionSql.append("language = ?");
                    params.add(request.getLanguage());
                }
                regionSql.append(", updated_at = NOW() WHERE tenant_domain = ? AND deleted = false");
                params.add(tenantDomain);
                
                masterJdbcTemplate.update(regionSql.toString(), params.toArray());
                log.info("ADMIN_TAMABEE updated region/language for company: {}", tenantDomain);
            } else {
                log.warn("Non-ADMIN_TAMABEE user attempted to change region/language for company: {}", tenantDomain);
            }
        }
        
        return getMyCompanyProfile();
    }

    @Override
    @Transactional
    public CompanyProfileResponse updateLogo(String logoUrl) {
        String tenantDomain = TenantContext.getCurrentTenant();
        
        String updateSql = """
            UPDATE companies SET logo = ?, updated_at = NOW()
            WHERE tenant_domain = ? AND deleted = false
            """;
        
        int updated = masterJdbcTemplate.update(updateSql, logoUrl, tenantDomain);
        
        if (updated == 0) {
            throw NotFoundException.company(tenantDomain);
        }
        
        return getMyCompanyProfile();
    }

    /**
     * Map ResultSet sang CompanyProfileResponse
     */
    private CompanyProfileResponse mapToResponse(ResultSet rs) throws SQLException {
        LocalDateTime freeTrialEndDate = toLocalDateTime(rs.getTimestamp("free_trial_end_date"));
        
        // Tính toán isFreeTrialActive dựa trên free_trial_end_date
        Boolean isFreeTrialActive = freeTrialEndDate != null
                && freeTrialEndDate.isAfter(LocalDateTime.now(
                        RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        
        return CompanyProfileResponse.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .ownerName(rs.getString("owner_name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .address(rs.getString("address"))
                .industry(rs.getString("industry"))
                .zipcode(rs.getString("zipcode"))
                .region(rs.getString("region"))
                .language(rs.getString("language"))
                .logo(rs.getString("logo"))
                .tenantDomain(rs.getString("tenant_domain"))
                .status(CompanyStatus.valueOf(rs.getString("status")))
                .planId(rs.getObject("plan_id", Long.class))
                .planNameVi(rs.getString("name_vi"))
                .planNameEn(rs.getString("name_en"))
                .planNameJa(rs.getString("name_ja"))
                .planMonthlyPrice(rs.getBigDecimal("monthly_price"))
                .planMaxEmployees(rs.getObject("max_employees", Integer.class))
                .walletBalance(rs.getBigDecimal("balance"))
                .lastBillingDate(toLocalDateTime(rs.getTimestamp("last_billing_date")))
                .nextBillingDate(toLocalDateTime(rs.getTimestamp("next_billing_date")))
                .freeTrialEndDate(freeTrialEndDate)
                .isFreeTrialActive(isFreeTrialActive)
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                .build();
    }

    /**
     * Convert Timestamp sang LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
