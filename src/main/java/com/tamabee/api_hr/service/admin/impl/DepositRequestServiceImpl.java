package com.tamabee.api_hr.service.admin.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.request.company.RejectRequest;
import com.tamabee.api_hr.dto.request.wallet.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.wallet.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.wallet.DepositRequestResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.DepositRequestMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.wallet.DepositRequestRepository;
import com.tamabee.api_hr.service.admin.interfaces.IDepositRequestService;
import com.tamabee.api_hr.service.admin.interfaces.IWalletService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.util.SecurityUtil;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý yêu cầu nạp tiền
 * Hỗ trợ tạo, xem, duyệt và từ chối yêu cầu nạp tiền
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepositRequestServiceImpl implements IDepositRequestService {

    private final DepositRequestRepository depositRequestRepository;
    private final CompanyRepository companyRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final DepositRequestMapper depositRequestMapper;
    private final IWalletService walletService;
    private final IEmailService emailService;
    private final INotificationService notificationService;
    private final SecurityUtil securityUtil;

    // ==================== Company Operations ====================

    @Override
    @Transactional
    public DepositRequestResponse create(DepositRequestCreateRequest request) {
        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Validate transferProofUrl
        if (request.getTransferProofUrl() == null || request.getTransferProofUrl().trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_TRANSFER_PROOF);
        }

        // Lấy thông tin user hiện tại từ JWT
        Long companyId = securityUtil.getCurrentUserCompanyId();
        String requestedBy = securityUtil.getCurrentUserEmployeeCode();
        String requesterName = securityUtil.getCurrentUserName();
        String requesterRole = securityUtil.getCurrentUserRole();
        String requesterEmail = securityUtil.getCurrentUserEmail();

        // Tạo entity
        DepositRequestEntity entity = depositRequestMapper.toEntity(request, companyId, 
                requestedBy, requesterName, requesterRole, requesterEmail);
        DepositRequestEntity savedEntity = depositRequestRepository.save(entity);

        log.info("Tạo yêu cầu nạp tiền mới: id={}, companyId={}, amount={}, requestedBy={}",
                savedEntity.getId(), companyId, request.getAmount(), requestedBy);

        return toResponseWithDetails(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepositRequestResponse> getMyRequests(DepositFilterRequest filter, Pageable pageable) {
        Long companyId = getCurrentUserCompanyId();

        Page<DepositRequestEntity> entities;
        if (filter != null && filter.getStatus() != null) {
            entities = depositRequestRepository.findByDeletedFalseAndCompanyIdAndStatusOrderByCreatedAtDesc(
                    companyId, filter.getStatus(), pageable);
        } else {
            entities = depositRequestRepository.findByDeletedFalseAndCompanyIdOrderByCreatedAtDesc(companyId, pageable);
        }

        return entities.map(this::toResponseWithDetails);
    }

    // ==================== Admin Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<DepositRequestResponse> getAll(DepositFilterRequest filter, Pageable pageable) {
        Page<DepositRequestEntity> entities;

        boolean hasStatus = filter != null && filter.getStatus() != null;
        boolean hasCompanyId = filter != null && filter.getCompanyId() != null;

        if (hasStatus && hasCompanyId) {
            entities = depositRequestRepository.findByDeletedFalseAndCompanyIdAndStatusOrderByCreatedAtDesc(
                    filter.getCompanyId(), filter.getStatus(), pageable);
        } else if (hasStatus) {
            entities = depositRequestRepository.findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                    filter.getStatus(), pageable);
        } else if (hasCompanyId) {
            entities = depositRequestRepository.findByDeletedFalseAndCompanyIdOrderByCreatedAtDesc(
                    filter.getCompanyId(), pageable);
        } else {
            entities = depositRequestRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable);
        }

        return entities.map(this::toResponseWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositRequestResponse getById(Long id) {
        DepositRequestEntity entity = depositRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.deposit(id));

        return toResponseWithDetails(entity);
    }

    @Override
    @Transactional
    public DepositRequestResponse approve(Long id) {
        // Lấy deposit request
        DepositRequestEntity entity = depositRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.deposit(id));

        // Kiểm tra status phải là PENDING
        if (entity.getStatus() != DepositStatus.PENDING) {
            throw BadRequestException.depositAlreadyProcessed();
        }

        // Lấy thông tin người duyệt từ JWT
        String approvedBy = securityUtil.getCurrentUserEmployeeCode();
        String approverName = securityUtil.getCurrentUserName();
        String approverRole = securityUtil.getCurrentUserRole();
        String approverEmail = securityUtil.getCurrentUserEmail();

        // Cập nhật status và thông tin người duyệt
        entity.setStatus(DepositStatus.APPROVED);
        entity.setApprovedBy(approvedBy);
        entity.setApproverName(approverName);
        entity.setApproverRole(approverRole);
        entity.setApproverEmail(approverEmail);
        entity.setProcessedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        depositRequestRepository.save(entity);

        // Cộng tiền vào wallet
        String description = "Nạp tiền - Yêu cầu #" + id;
        walletService.addBalance(entity.getCompanyId(), entity.getAmount(), description,
                TransactionType.DEPOSIT, entity.getId());

        log.info("Duyệt yêu cầu nạp tiền: id={}, companyId={}, amount={}, approvedBy={}",
                id, entity.getCompanyId(), entity.getAmount(), approvedBy);

        // Gửi email thông báo
        sendDepositApprovedEmail(entity);

        // Gửi notification cho người tạo yêu cầu
        notifyRequesterOnDepositApproved(entity);

        return toResponseWithDetails(entity);
    }

    @Override
    @Transactional
    public DepositRequestResponse reject(Long id, RejectRequest request) {
        // Validate rejection reason
        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw BadRequestException.invalidRejectionReason();
        }

        // Lấy deposit request
        DepositRequestEntity entity = depositRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.deposit(id));

        // Kiểm tra status phải là PENDING
        if (entity.getStatus() != DepositStatus.PENDING) {
            throw BadRequestException.depositAlreadyProcessed();
        }

        // Lấy thông tin người từ chối từ JWT
        String approvedBy = securityUtil.getCurrentUserEmployeeCode();
        String approverName = securityUtil.getCurrentUserName();
        String approverRole = securityUtil.getCurrentUserRole();
        String approverEmail = securityUtil.getCurrentUserEmail();

        // Cập nhật status và thông tin người từ chối
        entity.setStatus(DepositStatus.REJECTED);
        entity.setApprovedBy(approvedBy);
        entity.setApproverName(approverName);
        entity.setApproverRole(approverRole);
        entity.setApproverEmail(approverEmail);
        entity.setRejectionReason(request.getRejectionReason());
        entity.setProcessedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        depositRequestRepository.save(entity);

        log.info("Từ chối yêu cầu nạp tiền: id={}, companyId={}, amount={}, rejectedBy={}, reason={}",
                id, entity.getCompanyId(), entity.getAmount(), approvedBy, request.getRejectionReason());

        // Gửi notification cho người tạo yêu cầu
        notifyRequesterOnDepositRejected(entity, request.getRejectionReason());

        return toResponseWithDetails(entity);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy user hiện tại từ JWT token
     * Dùng employeeCode từ JWT thay vì query DB
     */
    private String getCurrentUserEmployeeCode() {
        return securityUtil.getCurrentUserEmployeeCode();
    }

    /**
     * Lấy companyId của user hiện tại từ JWT token
     */
    private Long getCurrentUserCompanyId() {
        return securityUtil.getCurrentUserCompanyId();
    }

    /**
     * Chuyển đổi entity sang response với thông tin bổ sung
     */
    private DepositRequestResponse toResponseWithDetails(DepositRequestEntity entity) {
        // Lấy tên company
        String companyName = companyRepository.findById(entity.getCompanyId())
                .map(CompanyEntity::getName)
                .orElse("Unknown");

        return depositRequestMapper.toResponse(entity, companyName);
    }

    /**
     * Gửi email thông báo nạp tiền thành công
     */
    private void sendDepositApprovedEmail(DepositRequestEntity entity) {
        try {
            CompanyEntity company = companyRepository.findById(entity.getCompanyId()).orElse(null);
            if (company != null) {
                // Lấy balance hiện tại của wallet sau khi nạp tiền
                BigDecimal currentBalance = walletService.getByCompanyId(entity.getCompanyId()).getBalance();

                emailService.sendDepositApproved(
                        company.getEmail(),
                        company.getName(),
                        entity.getAmount(),
                        currentBalance,
                        company.getLanguage());
                log.info("Email thông báo nạp tiền thành công đã được gửi đến: {}", company.getEmail());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Gửi notification cho người tạo yêu cầu khi deposit được duyệt
     */
    private void notifyRequesterOnDepositApproved(DepositRequestEntity entity) {
        try {
            // Lấy company để có tenantDomain
            CompanyEntity company = companyRepository.findById(entity.getCompanyId()).orElse(null);
            if (company == null || company.getTenantDomain() == null) {
                log.warn("Không tìm thấy company hoặc tenantDomain cho deposit: {}", entity.getId());
                return;
            }

            Long requesterId = getRequesterUserId(entity);
            if (requesterId == null) {
                log.warn("Không tìm thấy userId của người tạo yêu cầu nạp tiền: {}", entity.getRequestedBy());
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("amount", entity.getAmount().toString());

            // Dùng createNotificationForTenant để insert vào tenant DB
            notificationService.createNotificationForTenant(
                    company.getTenantDomain(),
                    requesterId,
                    NotificationCode.DEPOSIT_APPROVED,
                    params,
                    "/dashboard/wallet",
                    NotificationType.WALLET);

            log.info("Đã gửi notification duyệt nạp tiền cho user {} trong tenant {}", 
                    requesterId, company.getTenantDomain());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification duyệt nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Gửi notification cho người tạo yêu cầu khi deposit bị từ chối
     */
    private void notifyRequesterOnDepositRejected(DepositRequestEntity entity, String reason) {
        try {
            // Lấy company để có tenantDomain
            CompanyEntity company = companyRepository.findById(entity.getCompanyId()).orElse(null);
            if (company == null || company.getTenantDomain() == null) {
                log.warn("Không tìm thấy company hoặc tenantDomain cho deposit: {}", entity.getId());
                return;
            }

            Long requesterId = getRequesterUserId(entity);
            if (requesterId == null) {
                log.warn("Không tìm thấy userId của người tạo yêu cầu nạp tiền: {}", entity.getRequestedBy());
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("amount", entity.getAmount().toString());
            params.put("reason", reason != null ? reason : "");

            // Dùng createNotificationForTenant để insert vào tenant DB
            notificationService.createNotificationForTenant(
                    company.getTenantDomain(),
                    requesterId,
                    NotificationCode.DEPOSIT_REJECTED,
                    params,
                    "/dashboard/wallet",
                    NotificationType.WALLET);

            log.info("Đã gửi notification từ chối nạp tiền cho user {} trong tenant {}", 
                    requesterId, company.getTenantDomain());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification từ chối nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Lấy userId của người tạo yêu cầu nạp tiền.
     * Dùng JDBC để query trực tiếp từ tenant DB của company.
     */
    private Long getRequesterUserId(DepositRequestEntity entity) {
        // Lấy company để có tenantDomain
        CompanyEntity company = companyRepository.findById(entity.getCompanyId()).orElse(null);
        if (company == null || company.getTenantDomain() == null) {
            return null;
        }

        try {
            // Lấy DataSource của tenant
            DataSource tenantDataSource = tenantDataSourceManager.getDataSource(company.getTenantDomain());
            if (tenantDataSource == null) {
                log.error("Không tìm thấy DataSource cho tenant: {}", company.getTenantDomain());
                return null;
            }

            // Query user từ tenant DB bằng JDBC
            JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(tenantDataSource);
            String sql = "SELECT id FROM users WHERE employee_code = ? AND deleted = false";
            
            return tenantJdbcTemplate.queryForObject(sql, Long.class, entity.getRequestedBy());
        } catch (Exception e) {
            log.warn("Không tìm thấy user với employeeCode {} trong tenant {}: {}", 
                    entity.getRequestedBy(), company.getTenantDomain(), e.getMessage());
            return null;
        }
    }

    // ==================== Company Self-Service Operations ====================

    @Override
    @Transactional
    public DepositRequestResponse cancel(Long id) {
        // Lấy deposit request
        DepositRequestEntity entity = depositRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.deposit(id));

        // Kiểm tra quyền: chỉ company của mình mới được hủy
        Long currentCompanyId = getCurrentUserCompanyId();
        if (!entity.getCompanyId().equals(currentCompanyId)) {
            throw new BadRequestException(ErrorCode.FORBIDDEN);
        }

        // Kiểm tra status phải là PENDING
        if (entity.getStatus() != DepositStatus.PENDING) {
            throw BadRequestException.depositAlreadyProcessed();
        }

        // Soft delete
        entity.setDeleted(true);
        depositRequestRepository.save(entity);

        log.info("Hủy yêu cầu nạp tiền: id={}, companyId={}, amount={}",
                id, entity.getCompanyId(), entity.getAmount());

        return toResponseWithDetails(entity);
    }

    @Override
    @Transactional
    public DepositRequestResponse update(Long id, DepositRequestCreateRequest request) {
        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BadRequestException.invalidAmount();
        }

        // Validate transferProofUrl
        if (request.getTransferProofUrl() == null || request.getTransferProofUrl().trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_TRANSFER_PROOF);
        }

        // Lấy deposit request
        DepositRequestEntity entity = depositRequestRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.deposit(id));

        // Kiểm tra quyền: chỉ company của mình mới được cập nhật
        Long currentCompanyId = getCurrentUserCompanyId();
        if (!entity.getCompanyId().equals(currentCompanyId)) {
            throw new BadRequestException(ErrorCode.FORBIDDEN);
        }

        // Kiểm tra status phải là REJECTED
        if (entity.getStatus() != DepositStatus.REJECTED) {
            throw new BadRequestException(ErrorCode.DEPOSIT_NOT_REJECTED);
        }

        // Cập nhật thông tin
        entity.setAmount(request.getAmount());
        entity.setTransferProofUrl(request.getTransferProofUrl());
        entity.setStatus(DepositStatus.PENDING);
        entity.setRejectionReason(null);
        entity.setApprovedBy(null);
        entity.setProcessedAt(null);
        depositRequestRepository.save(entity);

        log.info("Cập nhật yêu cầu nạp tiền bị từ chối: id={}, companyId={}, newAmount={}",
                id, entity.getCompanyId(), request.getAmount());

        return toResponseWithDetails(entity);
    }
}
