package com.tamabee.api_hr.service.admin.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.request.wallet.CommissionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.CommissionOverallSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.admin.EmployeeCommissionMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.wallet.EmployeeCommissionRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.service.admin.interfaces.ICommissionService;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý hoa hồng giới thiệu cho nhân viên Tamabee
 * Status flow: PENDING → ELIGIBLE → PAID
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements ICommissionService {

    private final EmployeeCommissionRepository commissionRepository;
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;
    private final ISettingService settingService;
    private final EmployeeCommissionMapper commissionMapper;
    private final TenantDataSourceManager tenantDataSourceManager;


    // ==================== Commission Processing ====================

    @Override
    @Transactional
    public void processCommission(Long companyId) {
        if (commissionRepository.existsByCompanyId(companyId)) {
            log.debug("Company {} đã có commission, bỏ qua", companyId);
            return;
        }

        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        Long referrerId = company.getReferredByEmployeeId();
        if (referrerId == null) {
            log.debug("Company {} không có người giới thiệu, bỏ qua", companyId);
            return;
        }

        ReferrerInfo referrer = getReferrerInfo(referrerId);
        if (referrer == null) {
            log.debug("Không tìm thấy thông tin người giới thiệu {}, bỏ qua", referrerId);
            return;
        }

        if (!isTamabeeEmployee(referrer.role)) {
            log.debug("Người giới thiệu {} không phải nhân viên Tamabee, bỏ qua", referrer.employeeCode);
            return;
        }

        BigDecimal commissionAmount = BigDecimal.valueOf(settingService.getCommissionAmount());
        EmployeeCommissionEntity commission = commissionMapper.createEntity(
                referrer.employeeCode, companyId, commissionAmount);
        commissionRepository.save(commission);

        log.info("Đã tạo commission {} JPY cho nhân viên {} từ company {}",
                commissionAmount, referrer.employeeCode, companyId);
    }

    private record ReferrerInfo(String employeeCode, String role) {}

    private ReferrerInfo getReferrerInfo(Long userId) {
        try {
            DataSource tamabeeDs = tenantDataSourceManager.getDataSource("tamabee");
            if (tamabeeDs == null) return null;

            try (Connection conn = tamabeeDs.getConnection()) {
                String sql = "SELECT employee_code, role FROM users WHERE id = ? AND deleted = false";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new ReferrerInfo(rs.getString("employee_code"), rs.getString("role"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting referrer info: {}", e.getMessage());
        }
        return null;
    }

    private boolean isTamabeeEmployee(String role) {
        return role != null && (
            role.equals(UserRole.ADMIN_TAMABEE.name()) ||
            role.equals(UserRole.MANAGER_TAMABEE.name()) ||
            role.equals(UserRole.EMPLOYEE_TAMABEE.name())
        );
    }


    // ==================== View Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getAll(CommissionFilterRequest filter, Pageable pageable) {
        Page<EmployeeCommissionEntity> commissions = queryCommissions(filter, pageable);
        return commissions.map(this::toResponseWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getMyCommissions(CommissionFilterRequest filter, Pageable pageable) {
        String employeeCode = getCurrentUserEmployeeCode();

        CommissionFilterRequest myFilter = new CommissionFilterRequest();
        myFilter.setEmployeeCode(employeeCode);
        myFilter.setStatus(filter != null ? filter.getStatus() : null);
        myFilter.setFromDate(filter != null ? filter.getFromDate() : null);
        myFilter.setToDate(filter != null ? filter.getToDate() : null);

        Page<EmployeeCommissionEntity> commissions = queryCommissions(myFilter, pageable);
        return commissions.map(this::toResponseWithDetails);
    }

    // ==================== Summary Operations ====================

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponse getSummary(String employeeCode) {
        String employeeName = getEmployeeNameByCode(employeeCode);
        return buildSummaryResponse(employeeCode, employeeName);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponse getMySummary() {
        String employeeCode = getCurrentUserEmployeeCode();
        String employeeName = getEmployeeNameByCode(employeeCode);
        return buildSummaryResponse(employeeCode, employeeName);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionOverallSummaryResponse getOverallSummary() {
        CommissionOverallSummaryResponse response = new CommissionOverallSummaryResponse();

        response.setTotalPending(commissionRepository.sumAmountByStatus(CommissionStatus.PENDING));
        response.setTotalEligible(commissionRepository.sumAmountByStatus(CommissionStatus.ELIGIBLE));
        response.setTotalPaid(commissionRepository.sumAmountByStatus(CommissionStatus.PAID));
        response.setTotalAmount(commissionRepository.sumTotalAmount());

        List<String> employeeCodes = commissionRepository.findDistinctEmployeeCodes();
        List<CommissionOverallSummaryResponse.EmployeeSummary> byEmployee = new ArrayList<>();

        for (String empCode : employeeCodes) {
            CommissionOverallSummaryResponse.EmployeeSummary empSummary = new CommissionOverallSummaryResponse.EmployeeSummary();
            empSummary.setEmployeeCode(empCode);
            empSummary.setEmployeeName(getEmployeeNameByCode(empCode));
            empSummary.setCount(commissionRepository.countByEmployeeCode(empCode));
            empSummary.setTotalPending(commissionRepository.sumAmountByEmployeeCodeAndStatus(empCode, CommissionStatus.PENDING));
            empSummary.setTotalEligible(commissionRepository.sumAmountByEmployeeCodeAndStatus(empCode, CommissionStatus.ELIGIBLE));
            empSummary.setTotalPaid(commissionRepository.sumAmountByEmployeeCodeAndStatus(empCode, CommissionStatus.PAID));
            empSummary.setTotalAmount(commissionRepository.sumAmountByEmployeeCode(empCode));
            byEmployee.add(empSummary);
        }
        response.setByEmployee(byEmployee);

        List<java.sql.Timestamp> months = commissionRepository.findDistinctMonthsNative();
        List<CommissionOverallSummaryResponse.MonthSummary> byMonth = new ArrayList<>();

        for (java.sql.Timestamp monthTimestamp : months) {
            LocalDateTime month = monthTimestamp.toLocalDateTime();
            CommissionOverallSummaryResponse.MonthSummary monthSummary = new CommissionOverallSummaryResponse.MonthSummary();
            monthSummary.setMonth(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));

            LocalDateTime startOfMonth = month.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

            BigDecimal pendingAmount = commissionRepository.sumAmountByMonthRangeAndStatus(startOfMonth, endOfMonth, CommissionStatus.PENDING);
            BigDecimal paidAmount = commissionRepository.sumAmountByMonthRangeAndStatus(startOfMonth, endOfMonth, CommissionStatus.PAID);
            
            monthSummary.setTotalPending(pendingAmount);
            monthSummary.setTotalPaid(paidAmount);
            monthSummary.setTotalAmount(pendingAmount.add(paidAmount));
            monthSummary.setCount(commissionRepository.countByMonthRange(startOfMonth, endOfMonth));
            byMonth.add(monthSummary);
        }
        response.setByMonth(byMonth);

        return response;
    }


    // ==================== Admin Operations ====================

    @Override
    @Transactional
    public CommissionResponse markAsPaid(Long id) {
        EmployeeCommissionEntity commission = commissionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.commission(id));

        if (commission.getStatus() != CommissionStatus.ELIGIBLE) {
            throw BadRequestException.commissionNotEligible();
        }

        commission.setStatus(CommissionStatus.PAID);
        commission.setPaidAt(LocalDateTime.now());
        commission.setPaidBy(getCurrentUserEmployeeCode());

        EmployeeCommissionEntity savedCommission = commissionRepository.save(commission);
        log.info("Đã đánh dấu commission {} là PAID bởi {}", id, commission.getPaidBy());

        return toResponseWithDetails(savedCommission);
    }

    // ==================== Eligibility Operations ====================

    @Override
    @Transactional
    public boolean calculateEligibility(Long commissionId) {
        EmployeeCommissionEntity commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> NotFoundException.commission(commissionId));

        if (commission.getStatus() == CommissionStatus.PAID) {
            return true;
        }

        WalletEntity wallet = walletRepository.findByCompanyId(commission.getCompanyId())
                .orElseThrow(() -> NotFoundException.wallet(commission.getCompanyId()));

        BigDecimal totalBilling = wallet.getTotalBilling();
        BigDecimal commissionAmount = commission.getAmount();
        boolean isEligible = totalBilling.compareTo(commissionAmount) > 0;

        if (isEligible && commission.getStatus() == CommissionStatus.PENDING) {
            commission.setStatus(CommissionStatus.ELIGIBLE);
            commissionRepository.save(commission);
            log.info("Commission {} đã đủ điều kiện (billing {} > commission {})",
                    commissionId, totalBilling, commissionAmount);
        }

        return isEligible;
    }

    @Override
    @Transactional
    public void recalculateOnBilling(Long companyId) {
        List<EmployeeCommissionEntity> pendingCommissions = commissionRepository.findPendingByCompanyId(companyId);

        if (pendingCommissions.isEmpty()) {
            log.debug("Company {} không có pending commissions", companyId);
            return;
        }

        WalletEntity wallet = walletRepository.findByCompanyId(companyId).orElse(null);
        if (wallet == null) {
            log.warn("Company {} không có wallet", companyId);
            return;
        }

        BigDecimal totalBilling = wallet.getTotalBilling();

        for (EmployeeCommissionEntity commission : pendingCommissions) {
            if (totalBilling.compareTo(commission.getAmount()) > 0) {
                commission.setStatus(CommissionStatus.ELIGIBLE);
                commissionRepository.save(commission);
                log.info("Commission {} của company {} đã đủ điều kiện", commission.getId(), companyId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getCommissionsWithEligibility(Pageable pageable) {
        Page<EmployeeCommissionEntity> commissions = commissionRepository.findAllWithEligibility(pageable);
        return commissions.map(this::toResponseWithDetails);
    }


    // ==================== Private Helper Methods ====================

    private String getCurrentUserEmployeeCode() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }
        String email = authentication.getName();
        return getEmployeeCodeByEmail(email);
    }

    private String getEmployeeCodeByEmail(String email) {
        try {
            DataSource tamabeeDs = tenantDataSourceManager.getDataSource("tamabee");
            if (tamabeeDs == null) return null;

            try (Connection conn = tamabeeDs.getConnection()) {
                String sql = "SELECT employee_code FROM users WHERE email = ? AND deleted = false";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("employee_code");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting employee code by email: {}", e.getMessage());
        }
        throw NotFoundException.user(email);
    }

    private String getEmployeeNameByCode(String employeeCode) {
        try {
            DataSource tamabeeDs = tenantDataSourceManager.getDataSource("tamabee");
            if (tamabeeDs == null) return employeeCode;

            try (Connection conn = tamabeeDs.getConnection()) {
                String sql = """
                    SELECT u.email, up.name FROM users u 
                    LEFT JOIN user_profiles up ON up.user_id = u.id 
                    WHERE u.employee_code = ? AND u.deleted = false
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, employeeCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            return name != null ? name : rs.getString("email");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting employee name: {}", e.getMessage());
        }
        return employeeCode;
    }

    private Page<EmployeeCommissionEntity> queryCommissions(CommissionFilterRequest filter, Pageable pageable) {
        if (filter == null) {
            return commissionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        boolean hasEmployeeCode = filter.getEmployeeCode() != null;
        boolean hasStatus = filter.getStatus() != null;
        boolean hasDateRange = filter.getFromDate() != null && filter.getToDate() != null;

        if (hasEmployeeCode && hasStatus && hasDateRange) {
            return commissionRepository.findByEmployeeCodeAndStatusAndDateRange(
                    filter.getEmployeeCode(), filter.getStatus(),
                    filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasEmployeeCode && hasStatus) {
            return commissionRepository.findByEmployeeCodeAndStatusOrderByCreatedAtDesc(
                    filter.getEmployeeCode(), filter.getStatus(), pageable);
        } else if (hasEmployeeCode && hasDateRange) {
            return commissionRepository.findByEmployeeCodeAndDateRange(
                    filter.getEmployeeCode(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasStatus && hasDateRange) {
            return commissionRepository.findByStatusAndDateRange(
                    filter.getStatus(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasEmployeeCode) {
            return commissionRepository.findByEmployeeCodeOrderByCreatedAtDesc(
                    filter.getEmployeeCode(), pageable);
        } else if (hasStatus) {
            return commissionRepository.findByStatusOrderByCreatedAtDesc(filter.getStatus(), pageable);
        } else if (hasDateRange) {
            return commissionRepository.findByDateRange(filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return commissionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    private CommissionResponse toResponseWithDetails(EmployeeCommissionEntity entity) {
        String employeeName = getEmployeeNameByCode(entity.getEmployeeCode());
        String companyName = companyRepository.findById(entity.getCompanyId())
                .map(CompanyEntity::getName).orElse(null);
        String paidByName = entity.getPaidBy() != null ? getEmployeeNameByCode(entity.getPaidBy()) : null;

        return commissionMapper.toResponse(entity, employeeName, companyName, paidByName);
    }

    private CommissionSummaryResponse buildSummaryResponse(String employeeCode, String employeeName) {
        CommissionSummaryResponse response = new CommissionSummaryResponse();
        response.setEmployeeCode(employeeCode);
        response.setEmployeeName(employeeName);
        response.setTotalCommissions(commissionRepository.countByEmployeeCode(employeeCode));
        response.setTotalAmount(commissionRepository.sumAmountByEmployeeCode(employeeCode));
        response.setPendingCommissions(commissionRepository.countByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PENDING));
        response.setPendingAmount(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PENDING));
        response.setPaidCommissions(commissionRepository.countByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PAID));
        response.setPaidAmount(commissionRepository.sumAmountByEmployeeCodeAndStatus(employeeCode, CommissionStatus.PAID));
        return response;
    }
}
