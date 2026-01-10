package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.wallet.CommissionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.CommissionOverallSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.admin.EmployeeCommissionMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.wallet.EmployeeCommissionRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.service.admin.interfaces.ICommissionService;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ISettingService settingService;
    private final EmployeeCommissionMapper commissionMapper;

    // ==================== Commission Processing ====================

    @Override
    @Transactional
    public void processCommission(Long companyId) {
        // Kiểm tra company đã có commission chưa (chỉ tính lần đầu)
        if (commissionRepository.existsByCompanyId(companyId)) {
            log.debug("Company {} đã có commission, bỏ qua", companyId);
            return;
        }

        // Lấy thông tin company
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        // Kiểm tra company có được giới thiệu không
        UserEntity referrer = company.getReferredByEmployee();
        if (referrer == null) {
            log.debug("Company {} không có người giới thiệu, bỏ qua", companyId);
            return;
        }

        // Kiểm tra người giới thiệu có phải nhân viên Tamabee không
        if (!isTamabeeEmployee(referrer)) {
            log.debug("Người giới thiệu {} không phải nhân viên Tamabee, bỏ qua", referrer.getEmployeeCode());
            return;
        }

        // Lấy số tiền hoa hồng cố định từ setting
        BigDecimal commissionAmount = settingService.getCommissionRate();

        // Tạo commission record
        EmployeeCommissionEntity commission = commissionMapper.createEntity(
                referrer.getEmployeeCode(),
                companyId,
                commissionAmount);

        commissionRepository.save(commission);

        log.info("Đã tạo commission {} JPY cho nhân viên {} từ company {}",
                commissionAmount, referrer.getEmployeeCode(), companyId);
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

        // Override filter với employeeCode của user hiện tại
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
        // Lấy thông tin nhân viên
        UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                .orElseThrow(() -> NotFoundException.user(employeeCode));

        return buildSummaryResponse(employeeCode, getEmployeeName(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponse getMySummary() {
        String employeeCode = getCurrentUserEmployeeCode();
        UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                .orElseThrow(() -> NotFoundException.user(employeeCode));

        return buildSummaryResponse(employeeCode, getEmployeeName(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionOverallSummaryResponse getOverallSummary() {
        CommissionOverallSummaryResponse response = new CommissionOverallSummaryResponse();

        // Tổng số tiền pending (chờ đủ điều kiện)
        response.setTotalPending(commissionRepository.sumAmountByStatus(CommissionStatus.PENDING));

        // Tổng số tiền eligible (đủ điều kiện, chờ thanh toán)
        response.setTotalEligible(commissionRepository.sumAmountByStatus(CommissionStatus.ELIGIBLE));

        // Tổng số tiền paid
        response.setTotalPaid(commissionRepository.sumAmountByStatus(CommissionStatus.PAID));

        // Tổng số tiền
        response.setTotalAmount(commissionRepository.sumTotalAmount());

        // Thống kê theo nhân viên
        List<String> employeeCodes = commissionRepository.findDistinctEmployeeCodes();
        List<CommissionOverallSummaryResponse.EmployeeSummary> byEmployee = new ArrayList<>();

        for (String employeeCode : employeeCodes) {
            CommissionOverallSummaryResponse.EmployeeSummary empSummary = new CommissionOverallSummaryResponse.EmployeeSummary();
            empSummary.setEmployeeCode(employeeCode);

            // Lấy tên nhân viên
            String employeeName = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                    .map(this::getEmployeeName)
                    .orElse(employeeCode);
            empSummary.setEmployeeName(employeeName);

            // Số lượng commission
            empSummary.setCount(commissionRepository.countByEmployeeCode(employeeCode));

            // Tổng pending
            empSummary.setTotalPending(commissionRepository.sumAmountByEmployeeCodeAndStatus(
                    employeeCode, CommissionStatus.PENDING));

            // Tổng eligible
            empSummary.setTotalEligible(commissionRepository.sumAmountByEmployeeCodeAndStatus(
                    employeeCode, CommissionStatus.ELIGIBLE));

            // Tổng paid
            empSummary.setTotalPaid(commissionRepository.sumAmountByEmployeeCodeAndStatus(
                    employeeCode, CommissionStatus.PAID));

            // Tổng số tiền
            empSummary.setTotalAmount(commissionRepository.sumAmountByEmployeeCode(employeeCode));

            byEmployee.add(empSummary);
        }

        response.setByEmployee(byEmployee);

        // Thống kê theo tháng
        List<java.sql.Timestamp> months = commissionRepository.findDistinctMonthsNative();
        List<CommissionOverallSummaryResponse.MonthSummary> byMonth = new ArrayList<>();

        for (java.sql.Timestamp monthTimestamp : months) {
            LocalDateTime month = monthTimestamp.toLocalDateTime();
            CommissionOverallSummaryResponse.MonthSummary monthSummary = new CommissionOverallSummaryResponse.MonthSummary();

            // Format tháng: YYYY-MM
            monthSummary.setMonth(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));

            // Tính khoảng thời gian của tháng
            LocalDateTime startOfMonth = month.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

            // Tổng số tiền pending trong tháng
            BigDecimal pendingAmount = commissionRepository.sumAmountByMonthRangeAndStatus(startOfMonth, endOfMonth,
                    CommissionStatus.PENDING);
            monthSummary.setTotalPending(pendingAmount);

            // Tổng số tiền paid trong tháng
            BigDecimal paidAmount = commissionRepository.sumAmountByMonthRangeAndStatus(startOfMonth, endOfMonth,
                    CommissionStatus.PAID);
            monthSummary.setTotalPaid(paidAmount);

            // Tổng số tiền trong tháng
            monthSummary.setTotalAmount(pendingAmount.add(paidAmount));

            // Số lượng commission trong tháng
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

        // Chỉ cho phép pay khi status đã ELIGIBLE
        if (commission.getStatus() != CommissionStatus.ELIGIBLE) {
            throw com.tamabee.api_hr.exception.BadRequestException.commissionNotEligible();
        }

        // Cập nhật status
        commission.setStatus(CommissionStatus.PAID);
        commission.setPaidAt(LocalDateTime.now());
        commission.setPaidBy(getCurrentUserEmployeeCode());

        EmployeeCommissionEntity savedCommission = commissionRepository.save(commission);

        log.info("Đã đánh dấu commission {} là PAID bởi {}", id, commission.getPaidBy());

        return toResponseWithDetails(savedCommission);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Kiểm tra user có phải nhân viên Tamabee không
     */
    private boolean isTamabeeEmployee(UserEntity user) {
        UserRole role = user.getRole();
        return role == UserRole.ADMIN_TAMABEE ||
                role == UserRole.MANAGER_TAMABEE ||
                role == UserRole.EMPLOYEE_TAMABEE;
    }

    /**
     * Lấy employeeCode của user hiện tại từ JWT token
     */
    private String getCurrentUserEmployeeCode() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        return user.getEmployeeCode();
    }

    /**
     * Query commissions với filter
     */
    private Page<EmployeeCommissionEntity> queryCommissions(CommissionFilterRequest filter, Pageable pageable) {
        boolean hasEmployeeCode = filter != null && filter.getEmployeeCode() != null;
        boolean hasStatus = filter != null && filter.getStatus() != null;
        boolean hasDateRange = filter != null && filter.getFromDate() != null && filter.getToDate() != null;

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
            return commissionRepository.findByStatusOrderByCreatedAtDesc(
                    filter.getStatus(), pageable);
        } else if (hasDateRange) {
            return commissionRepository.findByDateRange(
                    filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return commissionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    /**
     * Chuyển đổi entity sang response với thông tin chi tiết
     */
    private CommissionResponse toResponseWithDetails(EmployeeCommissionEntity entity) {
        // Lấy tên nhân viên
        String employeeName = userRepository.findByEmployeeCodeAndDeletedFalse(entity.getEmployeeCode())
                .map(this::getEmployeeName)
                .orElse(null);

        // Lấy tên company
        String companyName = companyRepository.findById(entity.getCompanyId())
                .map(CompanyEntity::getName)
                .orElse(null);

        // Lấy tên người thanh toán
        String paidByName = null;
        if (entity.getPaidBy() != null) {
            paidByName = userRepository.findByEmployeeCodeAndDeletedFalse(entity.getPaidBy())
                    .map(this::getEmployeeName)
                    .orElse(null);
        }

        return commissionMapper.toResponse(entity, employeeName, companyName, paidByName);
    }

    /**
     * Lấy tên nhân viên từ profile
     */
    private String getEmployeeName(UserEntity user) {
        if (user.getProfile() != null && user.getProfile().getName() != null) {
            return user.getProfile().getName();
        }
        return user.getEmail();
    }

    /**
     * Build summary response cho một nhân viên
     */
    private CommissionSummaryResponse buildSummaryResponse(String employeeCode, String employeeName) {
        CommissionSummaryResponse response = new CommissionSummaryResponse();
        response.setEmployeeCode(employeeCode);
        response.setEmployeeName(employeeName);

        // Tổng số commission
        response.setTotalCommissions(commissionRepository.countByEmployeeCode(employeeCode));

        // Tổng số tiền
        response.setTotalAmount(commissionRepository.sumAmountByEmployeeCode(employeeCode));

        // Số commission pending
        response.setPendingCommissions(commissionRepository.countByEmployeeCodeAndStatus(
                employeeCode, CommissionStatus.PENDING));

        // Số tiền pending
        response.setPendingAmount(commissionRepository.sumAmountByEmployeeCodeAndStatus(
                employeeCode, CommissionStatus.PENDING));

        // Số commission paid
        response.setPaidCommissions(commissionRepository.countByEmployeeCodeAndStatus(
                employeeCode, CommissionStatus.PAID));

        // Số tiền paid
        response.setPaidAmount(commissionRepository.sumAmountByEmployeeCodeAndStatus(
                employeeCode, CommissionStatus.PAID));

        return response;
    }

    // ==================== Eligibility Operations ====================

    @Override
    @Transactional
    public boolean calculateEligibility(Long commissionId) {
        EmployeeCommissionEntity commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> NotFoundException.commission(commissionId));

        // Nếu đã PAID thì không cần tính lại
        if (commission.getStatus() == CommissionStatus.PAID) {
            return true;
        }

        // Lấy total billing của company
        WalletEntity wallet = walletRepository.findByCompanyId(commission.getCompanyId())
                .orElseThrow(() -> NotFoundException.wallet(commission.getCompanyId()));

        BigDecimal totalBilling = wallet.getTotalBilling();
        BigDecimal commissionAmount = commission.getAmount();

        // Commission eligible khi: total_billing > commission amount
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
        // Lấy tất cả commissions PENDING của company
        List<EmployeeCommissionEntity> pendingCommissions = commissionRepository.findPendingByCompanyId(companyId);

        if (pendingCommissions.isEmpty()) {
            log.debug("Company {} không có pending commissions", companyId);
            return;
        }

        // Lấy total billing của company
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElse(null);

        if (wallet == null) {
            log.warn("Company {} không có wallet", companyId);
            return;
        }

        BigDecimal totalBilling = wallet.getTotalBilling();

        // Recalculate eligibility cho từng commission
        for (EmployeeCommissionEntity commission : pendingCommissions) {
            BigDecimal commissionAmount = commission.getAmount();

            // Commission eligible khi: total_billing > commission amount
            if (totalBilling.compareTo(commissionAmount) > 0) {
                commission.setStatus(CommissionStatus.ELIGIBLE);
                commissionRepository.save(commission);
                log.info("Commission {} của company {} đã đủ điều kiện (billing {} > commission {})",
                        commission.getId(), companyId, totalBilling, commissionAmount);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getCommissionsWithEligibility(Pageable pageable) {
        Page<EmployeeCommissionEntity> commissions = commissionRepository.findAllWithEligibility(pageable);
        return commissions.map(this::toResponseWithDetails);
    }
}
