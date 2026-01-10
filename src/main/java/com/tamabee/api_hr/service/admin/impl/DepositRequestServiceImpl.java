package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.request.RejectRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.admin.DepositRequestMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.DepositRequestRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.admin.IDepositRequestService;
import com.tamabee.api_hr.service.admin.IWalletService;
import com.tamabee.api_hr.service.core.IEmailService;
import com.tamabee.api_hr.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private final UserRepository userRepository;
    private final DepositRequestMapper depositRequestMapper;
    private final IWalletService walletService;
    private final IEmailService emailService;
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
        UserEntity currentUser = getCurrentUser();
        Long companyId = securityUtil.getCurrentUserCompanyId();
        String requestedBy = currentUser.getEmployeeCode();

        // Tạo entity
        DepositRequestEntity entity = depositRequestMapper.toEntity(request, companyId, requestedBy);
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

        // Lấy thông tin người duyệt
        UserEntity currentUser = getCurrentUser();
        String approvedBy = currentUser.getEmployeeCode();

        // Cập nhật status
        entity.setStatus(DepositStatus.APPROVED);
        entity.setApprovedBy(approvedBy);
        entity.setProcessedAt(LocalDateTime.now());
        depositRequestRepository.save(entity);

        // Cộng tiền vào wallet
        String description = "Nạp tiền - Yêu cầu #" + id;
        walletService.addBalance(entity.getCompanyId(), entity.getAmount(), description,
                TransactionType.DEPOSIT, entity.getId());

        log.info("Duyệt yêu cầu nạp tiền: id={}, companyId={}, amount={}, approvedBy={}",
                id, entity.getCompanyId(), entity.getAmount(), approvedBy);

        // Gửi email thông báo
        sendDepositApprovedEmail(entity);

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

        // Lấy thông tin người từ chối
        UserEntity currentUser = getCurrentUser();
        String approvedBy = currentUser.getEmployeeCode();

        // Cập nhật status - KHÔNG thay đổi balance
        entity.setStatus(DepositStatus.REJECTED);
        entity.setApprovedBy(approvedBy);
        entity.setRejectionReason(request.getRejectionReason());
        entity.setProcessedAt(LocalDateTime.now());
        depositRequestRepository.save(entity);

        log.info("Từ chối yêu cầu nạp tiền: id={}, companyId={}, amount={}, rejectedBy={}, reason={}",
                id, entity.getCompanyId(), entity.getAmount(), approvedBy, request.getRejectionReason());

        return toResponseWithDetails(entity);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy user hiện tại từ JWT token
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
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

        // Lấy thông tin người tạo yêu cầu (name và email)
        String requesterName = getRequesterName(entity.getRequestedBy());
        String requesterEmail = getRequesterEmail(entity.getRequestedBy());

        // Lấy tên người duyệt/từ chối (nếu có)
        String approvedByName = entity.getApprovedBy() != null
                ? getRequesterName(entity.getApprovedBy())
                : null;

        return depositRequestMapper.toResponse(entity, companyName, requesterName, requesterEmail, approvedByName);
    }

    /**
     * Lấy tên user theo employeeCode
     * Fallback về employee code nếu không có name trong profile
     */
    private String getRequesterName(String employeeCode) {
        if (employeeCode == null) {
            return null;
        }
        return userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode)
                .map(user -> {
                    // Ưu tiên lấy name từ profile, nếu không có thì fallback về employee code
                    if (user.getProfile() != null && user.getProfile().getName() != null
                            && !user.getProfile().getName().trim().isEmpty()) {
                        return user.getProfile().getName();
                    }
                    return employeeCode;
                })
                .orElse(employeeCode);
    }

    /**
     * Lấy email user theo employeeCode
     */
    private String getRequesterEmail(String employeeCode) {
        if (employeeCode == null) {
            return null;
        }
        return userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode)
                .map(UserEntity::getEmail)
                .orElse(null);
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
