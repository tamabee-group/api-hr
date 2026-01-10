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

import com.tamabee.api_hr.dto.request.DepositFilterRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.service.company.ICompanyDepositService;
import com.tamabee.api_hr.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho company deposit requests
 * Sử dụng masterJdbcTemplate vì deposit_requests nằm trong master DB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyDepositServiceImpl implements ICompanyDepositService {

    @Qualifier("masterJdbcTemplate")
    private final JdbcTemplate masterJdbcTemplate;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public Page<DepositRequestResponse> getMyRequests(DepositFilterRequest filter, Pageable pageable) {
        // Lấy companyId từ tenantDomain
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);
        
        // Build query với filter
        StringBuilder sql = new StringBuilder("""
            SELECT dr.id, dr.company_id, dr.amount, dr.transfer_proof_url,
                   dr.status, dr.requested_by, dr.approved_by, dr.rejected_reason,
                   dr.created_at, dr.updated_at,
                   c.name as company_name
            FROM deposit_requests dr
            LEFT JOIN companies c ON c.id = dr.company_id AND c.deleted = false
            WHERE dr.deleted = false AND dr.company_id = ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(companyId);
        
        // Filter theo status
        if (filter != null && filter.getStatus() != null) {
            sql.append(" AND dr.status = ?");
            params.add(filter.getStatus().name());
        }
        
        // Count total
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        Long total = masterJdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        
        // Add order and pagination
        sql.append(" ORDER BY dr.created_at DESC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        // Query data
        List<DepositRequestResponse> content = masterJdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> mapToResponse(rs),
            params.toArray()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
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
     * Map ResultSet sang DepositRequestResponse
     */
    private DepositRequestResponse mapToResponse(ResultSet rs) throws SQLException {
        DepositRequestResponse response = new DepositRequestResponse();
        response.setId(rs.getLong("id"));
        response.setCompanyId(rs.getLong("company_id"));
        response.setCompanyName(rs.getString("company_name"));
        response.setAmount(rs.getBigDecimal("amount"));
        response.setTransferProofUrl(rs.getString("transfer_proof_url"));
        response.setStatus(DepositStatus.valueOf(rs.getString("status")));
        response.setRequestedBy(rs.getString("requested_by"));
        response.setApprovedBy(rs.getString("approved_by"));
        response.setRejectionReason(rs.getString("rejected_reason"));
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
