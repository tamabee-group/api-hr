package com.tamabee.api_hr.service.company.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.wallet.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.wallet.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.wallet.DepositRequestResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.company.interfaces.ICompanyDepositService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;
import com.tamabee.api_hr.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho company deposit requests
 * Sử dụng masterJdbcTemplate vì deposit_requests nằm trong master DB
 */
@Slf4j
@Service
public class CompanyDepositServiceImpl implements ICompanyDepositService {

    private final JdbcTemplate masterJdbcTemplate;
    private final SecurityUtil securityUtil;
    private final IUploadService uploadService;
    private final IEmailService emailService;
    private final ISettingService settingService;

    public CompanyDepositServiceImpl(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            SecurityUtil securityUtil,
            IUploadService uploadService,
            IEmailService emailService,
            ISettingService settingService) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.securityUtil = securityUtil;
        this.uploadService = uploadService;
        this.emailService = emailService;
        this.settingService = settingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepositRequestResponse> getMyRequests(DepositFilterRequest filter, Pageable pageable) {
        // Lấy companyId từ tenantDomain
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);
        
        // Build query với filter
        StringBuilder sql = new StringBuilder("""
            SELECT dr.id, dr.company_id, dr.amount, dr.transfer_proof_url,
                   dr.status, dr.requested_by, dr.requester_name, dr.requester_role, dr.requester_email, dr.requester_language,
                   dr.approved_by, dr.approver_name, dr.approver_role, dr.approver_email, dr.rejection_reason,
                   dr.processed_at, dr.created_at, dr.updated_at,
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
     * Lấy tên công ty từ companyId
     */
    private String getCompanyNameById(Long companyId) {
        String sql = "SELECT name FROM companies WHERE id = ? AND deleted = false";
        try {
            return masterJdbcTemplate.queryForObject(sql, String.class, companyId);
        } catch (Exception e) {
            return "Unknown";
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
        response.setRequesterName(rs.getString("requester_name"));
        response.setRequesterRole(rs.getString("requester_role"));
        response.setRequesterEmail(rs.getString("requester_email"));
        response.setRequesterLanguage(rs.getString("requester_language"));
        response.setApprovedBy(rs.getString("approved_by"));
        response.setApproverName(rs.getString("approver_name"));
        response.setApproverRole(rs.getString("approver_role"));
        response.setApproverEmail(rs.getString("approver_email"));
        response.setRejectionReason(rs.getString("rejection_reason"));
        response.setProcessedAt(toLocalDateTime(rs.getTimestamp("processed_at")));
        response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return response;
    }

    /**
     * Convert Timestamp sang LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    @Override
    @Transactional
    public DepositRequestResponse create(DepositRequestCreateRequest request) {
        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }
        
        // Validate min deposit amount từ settings
        int minDepositAmount = settingService.getMinDepositAmount();
        if (request.getAmount().compareTo(BigDecimal.valueOf(minDepositAmount)) < 0) {
            throw BadRequestException.minDepositAmount(minDepositAmount);
        }
        
        if (request.getTransferProofUrl() == null || request.getTransferProofUrl().trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_TRANSFER_PROOF);
        }

        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);
        String requestedBy = securityUtil.getCurrentUserEmployeeCode();
        String requesterName = securityUtil.getCurrentUserName();
        String requesterRole = securityUtil.getCurrentUserRole();
        String requesterEmail = securityUtil.getCurrentUserEmail();
        LocalDateTime now = LocalDateTime.now();

        String sql = """
            INSERT INTO deposit_requests 
            (company_id, amount, transfer_proof_url, status, requested_by, requester_name, requester_role, requester_email, requester_language, deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        masterJdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, companyId);
            ps.setBigDecimal(2, request.getAmount());
            ps.setString(3, request.getTransferProofUrl());
            ps.setString(4, DepositStatus.PENDING.name());
            ps.setString(5, requestedBy);
            ps.setString(6, requesterName);
            ps.setString(7, requesterRole);
            ps.setString(8, requesterEmail);
            ps.setString(9, securityUtil.getCurrentUserLanguage());
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setTimestamp(11, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);
        log.info("Tạo yêu cầu nạp tiền: id={}, companyId={}, amount={}", id, companyId, request.getAmount());

        // Lấy tên công ty để gửi email
        String companyName = getCompanyNameById(companyId);
        
        // Gửi email thông báo cho admin Tamabee
        emailService.sendNewDepositNotification(
            companyName,
            request.getAmount(),
            request.getTransferProofUrl(),
            requesterName,
            requesterEmail
        );

        return getById(id);
    }

    @Override
    @Transactional
    public DepositRequestResponse cancel(Long id) {
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Long companyId = getCompanyIdByTenantDomain(tenantDomain);

        // Kiểm tra deposit tồn tại và thuộc company
        DepositRequestResponse deposit = getById(id);
        if (!deposit.getCompanyId().equals(companyId)) {
            throw new BadRequestException(ErrorCode.FORBIDDEN);
        }
        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw BadRequestException.depositAlreadyProcessed();
        }

        // Xóa ảnh chứng từ
        if (deposit.getTransferProofUrl() != null) {
            uploadService.deleteFile(deposit.getTransferProofUrl());
        }

        String sql = "UPDATE deposit_requests SET deleted = true, updated_at = ? WHERE id = ?";
        masterJdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), id);

        log.info("Hủy yêu cầu nạp tiền: id={}, companyId={}", id, companyId);
        return deposit;
    }

    /**
     * Lấy deposit theo ID
     */
    private DepositRequestResponse getById(Long id) {
        String sql = """
            SELECT dr.id, dr.company_id, dr.amount, dr.transfer_proof_url,
                   dr.status, dr.requested_by, dr.requester_name, dr.requester_role, dr.requester_email, dr.requester_language,
                   dr.approved_by, dr.approver_name, dr.approver_role, dr.approver_email, dr.rejection_reason,
                   dr.processed_at, dr.created_at, dr.updated_at,
                   c.name as company_name
            FROM deposit_requests dr
            LEFT JOIN companies c ON c.id = dr.company_id AND c.deleted = false
            WHERE dr.id = ? AND dr.deleted = false
            """;
        List<DepositRequestResponse> results = masterJdbcTemplate.query(sql, (rs, rowNum) -> mapToResponse(rs), id);
        if (results.isEmpty()) {
            throw NotFoundException.deposit(id);
        }
        return results.get(0);
    }
}
