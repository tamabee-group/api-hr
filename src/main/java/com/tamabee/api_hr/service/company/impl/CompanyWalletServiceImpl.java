package com.tamabee.api_hr.service.company.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.wallet.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.WalletResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletTransactionResponse;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.service.company.interfaces.ICompanyWalletService;
import com.tamabee.api_hr.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho company wallet
 * Sử dụng masterJdbcTemplate vì wallets và wallet_transactions nằm trong master DB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyWalletServiceImpl implements ICompanyWalletService {

    @Qualifier("masterJdbcTemplate")
    private final JdbcTemplate masterJdbcTemplate;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        // Lấy companyId từ tenantDomain thay vì từ JWT (vì Tamabee users có companyId = 0 trong JWT)
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);
        
        // Lấy language từ company
        String language = getCompanyLanguage(companyId);
        
        String sql = """
            SELECT w.id, w.company_id, w.balance, w.last_billing_date, 
                   w.next_billing_date, w.free_trial_end_date,
                   p.name_vi, p.name_en, p.name_ja
            FROM wallets w
            LEFT JOIN companies c ON c.id = w.company_id AND c.deleted = false
            LEFT JOIN plans p ON p.id = c.plan_id AND p.deleted = false
            WHERE w.company_id = ? AND w.deleted = false
            """;
        
        String finalLanguage = language;
        return masterJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToWalletResponse(rs, finalLanguage);
            }
            throw NotFoundException.wallet(companyId);
        }, companyId);
    }

    /**
     * Lấy companyId từ tenantDomain
     */
    private Long getCompanyIdByTenantDomain(String tenantDomain) {
        String sql = "SELECT id FROM companies WHERE tenant_domain = ? AND deleted = false";
        try {
            return masterJdbcTemplate.queryForObject(sql, Long.class, tenantDomain);
        } catch (Exception e) {
            throw NotFoundException.company(tenantDomain);
        }
    }

    /**
     * Lấy language của company từ master DB
     */
    private String getCompanyLanguage(Long companyId) {
        String sql = "SELECT language FROM companies WHERE id = ? AND deleted = false";
        try {
            return masterJdbcTemplate.queryForObject(sql, String.class, companyId);
        } catch (Exception e) {
            return "vi";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(TransactionFilterRequest filter, Pageable pageable) {
        // Lấy companyId từ tenantDomain
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);
        
        // Build query với filter
        StringBuilder sql = new StringBuilder("""
            SELECT wt.id, wt.wallet_id, wt.transaction_type, wt.amount,
                   wt.balance_before, wt.balance_after, wt.description,
                   wt.reference_id, wt.created_at
            FROM wallet_transactions wt
            JOIN wallets w ON w.id = wt.wallet_id AND w.deleted = false
            WHERE w.company_id = ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(companyId);
        
        // Filter theo transaction type
        if (filter != null && filter.getTransactionType() != null) {
            sql.append(" AND wt.transaction_type = ?");
            params.add(filter.getTransactionType().name());
        }
        
        // Filter theo date range
        if (filter != null && filter.getFromDate() != null) {
            sql.append(" AND wt.created_at >= ?");
            params.add(Timestamp.valueOf(filter.getFromDate()));
        }
        if (filter != null && filter.getToDate() != null) {
            sql.append(" AND wt.created_at <= ?");
            params.add(Timestamp.valueOf(filter.getToDate()));
        }
        
        // Count total
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        Long total = masterJdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        
        // Add order and pagination
        sql.append(" ORDER BY wt.created_at DESC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        // Query data
        List<WalletTransactionResponse> content = masterJdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> mapToTransactionResponse(rs),
            params.toArray()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * Map ResultSet sang WalletResponse
     */
    private WalletResponse mapToWalletResponse(ResultSet rs, String language) throws SQLException {
        WalletResponse response = new WalletResponse();
        response.setId(rs.getLong("id"));
        response.setCompanyId(rs.getLong("company_id"));
        response.setBalance(rs.getBigDecimal("balance"));
        response.setLastBillingDate(toLocalDateTime(rs.getTimestamp("last_billing_date")));
        response.setNextBillingDate(toLocalDateTime(rs.getTimestamp("next_billing_date")));
        response.setFreeTrialEndDate(toLocalDateTime(rs.getTimestamp("free_trial_end_date")));
        response.setPlanNameVi(rs.getString("name_vi"));
        response.setPlanNameEn(rs.getString("name_en"));
        response.setPlanNameJa(rs.getString("name_ja"));
        
        // Tính toán isFreeTrialActive
        LocalDateTime freeTrialEndDate = response.getFreeTrialEndDate();
        response.setIsFreeTrialActive(freeTrialEndDate != null && freeTrialEndDate.isAfter(LocalDateTime.now()));
        
        return response;
    }

    /**
     * Map ResultSet sang WalletTransactionResponse
     */
    private WalletTransactionResponse mapToTransactionResponse(ResultSet rs) throws SQLException {
        WalletTransactionResponse response = new WalletTransactionResponse();
        response.setId(rs.getLong("id"));
        response.setWalletId(rs.getLong("wallet_id"));
        response.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        response.setAmount(rs.getBigDecimal("amount"));
        response.setBalanceBefore(rs.getBigDecimal("balance_before"));
        response.setBalanceAfter(rs.getBigDecimal("balance_after"));
        response.setDescription(rs.getString("description"));
        response.setReferenceId(rs.getObject("reference_id", Long.class));
        response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return response;
    }

    /**
     * Convert Timestamp sang LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
