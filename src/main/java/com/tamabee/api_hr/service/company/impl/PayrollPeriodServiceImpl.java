package com.tamabee.api_hr.service.company.impl;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.payroll.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.request.wallet.PaymentRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.enums.PayrollItemStatus;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import com.tamabee.api_hr.enums.SalaryItemType;
import com.tamabee.api_hr.enums.SalaryType;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.PayrollPeriodMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.payroll.EmployeeSalaryItemRepository;
import com.tamabee.api_hr.repository.payroll.EmployeeSalaryRepository;
import com.tamabee.api_hr.repository.payroll.PayrollItemRepository;
import com.tamabee.api_hr.repository.payroll.PayrollPeriodRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.ICompanySettingsService;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;
import com.tamabee.api_hr.service.core.PayslipPdfGenerator;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho quản lý kỳ lương
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollPeriodServiceImpl implements IPayrollPeriodService {

    private final PayrollPeriodRepository periodRepository;
    private final PayrollItemRepository itemRepository;
    private final UserRepository userRepository;
    private final EmployeeSalaryRepository salaryRepository;
    private final EmployeeSalaryItemRepository salaryItemRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PayrollPeriodMapper mapper;
    private final ObjectMapper objectMapper;
    private final PayslipPdfGenerator pdfGenerator;
    private final CompanyRepository companyRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final ICompanySettingsService companySettingsService;
    private final INotificationService notificationService;
    private final IEmailService emailService;
    
    @Qualifier("masterJdbcTemplate")
    private final JdbcTemplate masterJdbcTemplate;

    // Giá trị mặc định nếu không có company settings
    private static final int DEFAULT_STANDARD_WORKING_DAYS = 22;
    private static final int DEFAULT_STANDARD_WORKING_HOURS = 8;

    @Override
    @Transactional
    public PayrollPeriodResponse createPayrollPeriod(PayrollPeriodRequest request, Long createdBy) {
        // Xác định year và month từ request hoặc từ periodStart
        Integer year = request.getYear();
        Integer month = request.getMonth();
        
        if ((year == null || month == null) && request.getPeriodStart() != null) {
            year = request.getPeriodStart().getYear();
            month = request.getPeriodStart().getMonthValue();
        }
        
        // Validate: phải có year và month
        if (year == null || month == null) {
            throw new BadRequestException("Phải cung cấp year/month hoặc periodStart", ErrorCode.VALIDATION_ERROR);
        }
        
        // Validate: không cho phép tạo kỳ lương cho tháng tương lai
        YearMonth requestedMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();
        if (requestedMonth.isAfter(currentMonth)) {
            throw new BadRequestException(
                "Không thể tạo kỳ lương cho tháng tương lai. Chỉ được tạo cho tháng hiện tại hoặc quá khứ.",
                ErrorCode.PAYROLL_FUTURE_PERIOD_NOT_ALLOWED);
        }
        
        // PayrollPeriod không có soft delete
        if (periodRepository.existsByYearAndMonth(year, month)) {
            throw new ConflictException("Kỳ lương đã tồn tại cho tháng " + month + "/" + year,
                    ErrorCode.PAYROLL_PERIOD_EXISTS);
        }

        // Tạo entity mới
        PayrollPeriodEntity entity = mapper.toEntity(request, createdBy);
        entity = periodRepository.save(entity);

        // Lấy thông tin user để trả về
        Map<Long, UserEntity> userMap = getUserMap(List.of(createdBy));

        return mapper.toResponse(entity, userMap);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse calculatePayroll(Long periodId) {
        // Lấy kỳ lương
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Kiểm tra trạng thái - chỉ cho phép tính lương khi DRAFT
        if (period.getStatus() != PayrollPeriodStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể tính lương khi kỳ lương ở trạng thái DRAFT",
                    ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        // Lấy company settings để tính lương
        PayrollConfig payrollConfig = companySettingsService.getPayrollConfig();
        OvertimeConfig overtimeConfig = companySettingsService.getOvertimeConfig();
        
        int standardWorkingDays = payrollConfig.getStandardWorkingDaysPerMonth() != null 
                ? payrollConfig.getStandardWorkingDaysPerMonth() 
                : DEFAULT_STANDARD_WORKING_DAYS;
        int standardWorkingHours = payrollConfig.getStandardWorkingHoursPerDay() != null 
                ? payrollConfig.getStandardWorkingHoursPerDay() 
                : DEFAULT_STANDARD_WORKING_HOURS;

        // Xóa các payroll items cũ (nếu có) để tính lại
        // PayrollItem không có soft delete - xóa thẳng
        List<PayrollItemEntity> existingItems = itemRepository.findByPayrollPeriodId(periodId);
        itemRepository.deleteAll(existingItems);

        // Lấy danh sách nhân viên active
        List<UserEntity> employees = userRepository.findByDeletedFalse();
        List<Long> employeeIds = employees.stream().map(UserEntity::getId).collect(Collectors.toList());

        // Lấy salary items (allowances và deductions) cho tất cả nhân viên
        List<EmployeeSalaryItemEntity> allSalaryItems = salaryItemRepository
                .findByEmployeeIdInAndDeletedFalse(employeeIds);

        // Group theo employeeId và type
        Map<Long, List<EmployeeSalaryItemEntity>> allowancesByEmployee = allSalaryItems.stream()
                .filter(item -> item.getTemplate() != null && item.getTemplate().getType() == SalaryItemType.ALLOWANCE)
                .collect(Collectors.groupingBy(EmployeeSalaryItemEntity::getEmployeeId));
        Map<Long, List<EmployeeSalaryItemEntity>> deductionsByEmployee = allSalaryItems.stream()
                .filter(item -> item.getTemplate() != null && item.getTemplate().getType() == SalaryItemType.DEDUCTION)
                .collect(Collectors.groupingBy(EmployeeSalaryItemEntity::getEmployeeId));

        // Tính lương cho từng nhân viên
        List<PayrollItemEntity> payrollItems = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (UserEntity employee : employees) {
            PayrollItemEntity item = calculateEmployeePayroll(
                    period, employee,
                    allowancesByEmployee.getOrDefault(employee.getId(), Collections.emptyList()),
                    deductionsByEmployee.getOrDefault(employee.getId(), Collections.emptyList()),
                    standardWorkingDays, standardWorkingHours, overtimeConfig);

            if (item != null) {
                payrollItems.add(item);
                if (item.getGrossSalary() != null) {
                    totalGross = totalGross.add(item.getGrossSalary());
                }
                if (item.getNetSalary() != null) {
                    totalNet = totalNet.add(item.getNetSalary());
                }
            }
        }

        // Lưu payroll items
        itemRepository.saveAll(payrollItems);

        // Cập nhật thông tin tổng hợp của period
        period.setTotalGrossSalary(totalGross);
        period.setTotalNetSalary(totalNet);
        period.setTotalEmployees(payrollItems.size());
        period = periodRepository.save(period);

        // Lấy thông tin user để trả về
        Map<Long, UserEntity> userMap = getUserMap(List.of(period.getCreatedBy()));

        return mapper.toResponse(period, userMap);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodDetailResponse getPayrollPeriodDetail(Long periodId) {
        // Lấy kỳ lương
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Lấy danh sách payroll items
        // PayrollItem không có soft delete
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);

        // Lấy danh sách user IDs cần thiết
        Set<Long> userIds = new HashSet<>();
        userIds.add(period.getCreatedBy());
        if (period.getApprovedBy() != null) {
            userIds.add(period.getApprovedBy());
        }
        for (PayrollItemEntity item : items) {
            userIds.add(item.getEmployeeId());
            if (item.getAdjustedBy() != null) {
                userIds.add(item.getAdjustedBy());
            }
        }

        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        // Chuyển đổi items sang response
        List<PayrollItemResponse> itemResponses = items.stream()
                .map(item -> mapper.toItemResponse(item, userMap, period.getYear(), period.getMonth(), period.getPaidAt()))
                .collect(Collectors.toList());

        return mapper.toDetailResponse(period, itemResponses, userMap);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollPeriodResponse> getPayrollPeriods(Integer year, String status, Pageable pageable) {
        Page<PayrollPeriodEntity> periods;

        // Filter theo year và status
        if (year != null && status != null && !status.isEmpty()) {
            PayrollPeriodStatus periodStatus = PayrollPeriodStatus.valueOf(status);
            periods = periodRepository.findByYearAndStatus(year, periodStatus, pageable);
        } else if (year != null) {
            periods = periodRepository.findByYear(year, pageable);
        } else if (status != null && !status.isEmpty()) {
            PayrollPeriodStatus periodStatus = PayrollPeriodStatus.valueOf(status);
            periods = periodRepository.findByStatus(periodStatus, pageable);
        } else {
            periods = periodRepository.findAllPaged(pageable);
        }

        // Lấy danh sách user IDs
        Set<Long> userIds = new HashSet<>();
        for (PayrollPeriodEntity period : periods) {
            userIds.add(period.getCreatedBy());
            if (period.getApprovedBy() != null) {
                userIds.add(period.getApprovedBy());
            }
        }

        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        return periods.map(period -> mapper.toResponse(period, userMap));
    }

    @Override
    @Transactional
    public PayrollItemResponse adjustPayrollItem(Long itemId, PayrollAdjustmentRequest request, Long adjustedBy) {
        // Lấy payroll item
        // PayrollItem không có soft delete
        PayrollItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(
                        () -> new NotFoundException("Không tìm thấy chi tiết lương", ErrorCode.PAYROLL_ITEM_NOT_FOUND));

        // Lấy period để kiểm tra trạng thái
        PayrollPeriodEntity period = getPeriodOrThrow(item.getPayrollPeriodId());

        // Kiểm tra trạng thái - chỉ cho phép điều chỉnh khi DRAFT hoặc REVIEWING
        if (period.getStatus() == PayrollPeriodStatus.APPROVED || period.getStatus() == PayrollPeriodStatus.PAID) {
            throw new BadRequestException("Không thể điều chỉnh lương khi kỳ lương đã được duyệt hoặc thanh toán",
                    ErrorCode.PAYROLL_ALREADY_APPROVED);
        }

        // Cập nhật điều chỉnh
        item.setAdjustmentAmount(request.getAdjustmentAmount());
        item.setAdjustmentReason(request.getAdjustmentReason());
        item.setAdjustedBy(adjustedBy);
        item.setAdjustedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        item.setStatus(PayrollItemStatus.ADJUSTED);

        // Tính lại net salary với adjustment
        BigDecimal netSalary = item.getGrossSalary();
        if (request.getAdjustmentAmount() != null) {
            netSalary = netSalary.add(request.getAdjustmentAmount());
        }
        item.setNetSalary(netSalary);

        item = itemRepository.save(item);

        // Cập nhật tổng của period
        updatePeriodTotals(period.getId());

        // Lấy thông tin user để trả về
        Set<Long> userIds = new HashSet<>();
        userIds.add(item.getEmployeeId());
        userIds.add(adjustedBy);
        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        return mapper.toItemResponse(item, userMap, period.getYear(), period.getMonth(), period.getPaidAt());
    }

    @Override
    @Transactional
    public PayrollPeriodResponse submitForReview(Long periodId, Long submittedBy) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Kiểm tra trạng thái - chỉ cho phép submit khi DRAFT
        if (period.getStatus() != PayrollPeriodStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể submit khi kỳ lương ở trạng thái DRAFT",
                    ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        // Kiểm tra đã có payroll items chưa
        // PayrollItem không có soft delete
        long itemCount = itemRepository.countByPayrollPeriodId(periodId);
        if (itemCount == 0) {
            throw new BadRequestException("Cần tính lương trước khi submit", ErrorCode.PAYROLL_CALCULATION_FAILED);
        }

        period.setStatus(PayrollPeriodStatus.REVIEWING);
        period.setSubmittedBy(submittedBy);
        period.setRejectionReason(null);
        period = periodRepository.save(period);

        // Gửi thông báo cho ADMIN_COMPANY
        notifyAdminOnPayrollSubmit(period);

        Map<Long, UserEntity> userMap = getUserMap(List.of(period.getCreatedBy()));
        return mapper.toResponse(period, userMap);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse approvePayroll(Long periodId, Long approverId) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Kiểm tra trạng thái - chỉ cho phép approve khi REVIEWING
        if (period.getStatus() != PayrollPeriodStatus.REVIEWING) {
            throw new BadRequestException("Chỉ có thể duyệt khi kỳ lương ở trạng thái REVIEWING",
                    ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(approverId);
        period.setApprovedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        period = periodRepository.save(period);

        // Cập nhật status của tất cả items thành CONFIRMED
        // PayrollItem không có soft delete
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);
        for (PayrollItemEntity item : items) {
            item.setStatus(PayrollItemStatus.CONFIRMED);
        }
        itemRepository.saveAll(items);

        // Gửi thông báo cho người gửi duyệt (admin) — KHÔNG gửi cho nhân viên vì chưa đến ngày trả lương
        notifySubmitterOnPayrollApproved(period);

        Set<Long> userIds = new HashSet<>();
        userIds.add(period.getCreatedBy());
        userIds.add(approverId);
        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        return mapper.toResponse(period, userMap);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse markAsPaid(Long periodId, PaymentRequest request) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Kiểm tra trạng thái - chỉ cho phép mark as paid khi APPROVED
        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể đánh dấu thanh toán khi kỳ lương đã được duyệt",
                    ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        period.setStatus(PayrollPeriodStatus.PAID);
        period.setPaidAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        if (request != null && request.getPaymentReference() != null) {
            period.setPaymentReference(request.getPaymentReference());
        }
        period = periodRepository.save(period);

        // Gửi thông báo cho tất cả nhân viên có trong kỳ lương
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);
        List<Long> employeeIds = items.stream()
                .map(PayrollItemEntity::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        
        if (!employeeIds.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("month", String.valueOf(period.getMonth()));
            params.put("year", String.valueOf(period.getYear()));
            
            notificationService.createBulkNotifications(
                    employeeIds,
                    NotificationCode.PAYROLL_PAID,
                    params,
                    "/me/payroll",
                    NotificationType.PAYROLL
            );
            log.info("Đã gửi thông báo thanh toán lương cho {} nhân viên", employeeIds.size());
        }

        Set<Long> userIds = new HashSet<>();
        userIds.add(period.getCreatedBy());
        if (period.getApprovedBy() != null) {
            userIds.add(period.getApprovedBy());
        }
        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        return mapper.toResponse(period, userMap);
    }

    // === Private helper methods ===

    /**
     * Lấy PayrollPeriodEntity hoặc throw NotFoundException
     * PayrollPeriod không có soft delete
     */
    private PayrollPeriodEntity getPeriodOrThrow(Long periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(
                        () -> new NotFoundException("Không tìm thấy kỳ lương", ErrorCode.PAYROLL_PERIOD_NOT_FOUND));
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Gửi thông báo và email cho ADMIN_COMPANY khi có kỳ lương được gửi duyệt
     */
    private void notifyAdminOnPayrollSubmit(PayrollPeriodEntity period) {
        try {
            // Tìm admin của company (ADMIN_COMPANY) hoặc admin Tamabee (ADMIN_TAMABEE) nếu đang ở tenant tamabee
            List<UserEntity> admins = userRepository.findByRoleInAndDeletedFalse(
                    List.of(UserRole.ADMIN_COMPANY, UserRole.ADMIN_TAMABEE));
            if (admins.isEmpty()) {
                log.warn("Không tìm thấy admin để gửi thông báo gửi duyệt lương");
                return;
            }

            List<Long> adminIds = admins.stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toList());

            Map<String, Object> params = new HashMap<>();
            params.put("month", String.valueOf(period.getMonth()));
            params.put("year", String.valueOf(period.getYear()));

            notificationService.createBulkNotifications(
                    adminIds,
                    NotificationCode.PAYROLL_SUBMITTED,
                    params,
                    "/dashboard/payroll/" + period.getId(),
                    NotificationType.PAYROLL);

            // Gửi email cho từng admin
            String periodStr = formatPeriodString(period);
            String totalEmployees = String.valueOf(period.getTotalEmployees());

            // Lấy tên người gửi duyệt
            String submitterName = getUserDisplayName(period.getSubmittedBy());

            for (UserEntity admin : admins) {
                try {
                    String recipientName = getProfileName(admin);
                    String totalNetSalary = formatCurrencyForEmail(period.getTotalNetSalary(), admin.getLanguage());
                    emailService.sendPayrollSubmittedNotification(
                            admin.getEmail(), recipientName, submitterName,
                            periodStr, totalEmployees, totalNetSalary,
                            admin.getLanguage());
                } catch (Exception e) {
                    log.error("Lỗi gửi email gửi duyệt lương cho admin {}: {}", admin.getId(), e.getMessage());
                }
            }

            log.info("Đã gửi thông báo gửi duyệt lương cho {} admin", adminIds.size());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo gửi duyệt lương: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo và email cho người gửi duyệt khi kỳ lương được duyệt
     */
    private void notifySubmitterOnPayrollApproved(PayrollPeriodEntity period) {
        try {
            if (period.getSubmittedBy() == null) {
                log.warn("Không có thông tin người gửi duyệt cho period {}", period.getId());
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("month", String.valueOf(period.getMonth()));
            params.put("year", String.valueOf(period.getYear()));

            notificationService.createNotification(
                    period.getSubmittedBy(),
                    NotificationCode.PAYROLL_CONFIRMED,
                    params,
                    "/dashboard/payroll/" + period.getId(),
                    NotificationType.PAYROLL);

            // Gửi email cho người gửi duyệt
            UserEntity submitter = userRepository.findByIdAndDeletedFalse(period.getSubmittedBy()).orElse(null);
            if (submitter != null) {
                String recipientName = getProfileName(submitter);
                String periodStr = formatPeriodString(period);
                String totalNetSalary = formatCurrencyForEmail(period.getTotalNetSalary(), submitter.getLanguage());
                String totalEmployees = String.valueOf(period.getTotalEmployees());

                emailService.sendPayrollApprovedNotification(
                        submitter.getEmail(), recipientName,
                        periodStr, totalEmployees, totalNetSalary,
                        submitter.getLanguage());
            }

            log.info("Đã gửi thông báo duyệt lương cho người gửi duyệt userId={}", period.getSubmittedBy());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo duyệt lương cho người gửi: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo và email cho người gửi duyệt khi kỳ lương bị từ chối
     */
    private void notifySubmitterOnPayrollRejected(PayrollPeriodEntity period) {
        try {
            if (period.getSubmittedBy() == null) {
                log.warn("Không có thông tin người gửi duyệt cho period {}", period.getId());
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("month", String.valueOf(period.getMonth()));
            params.put("year", String.valueOf(period.getYear()));

            notificationService.createNotification(
                    period.getSubmittedBy(),
                    NotificationCode.PAYROLL_REJECTED,
                    params,
                    "/dashboard/payroll/" + period.getId(),
                    NotificationType.PAYROLL);

            // Gửi email cho người gửi duyệt
            UserEntity submitter = userRepository.findByIdAndDeletedFalse(period.getSubmittedBy()).orElse(null);
            if (submitter != null) {
                String recipientName = getProfileName(submitter);
                String periodStr = formatPeriodString(period);
                String totalNetSalary = formatCurrencyForEmail(period.getTotalNetSalary(), submitter.getLanguage());
                String totalEmployees = String.valueOf(period.getTotalEmployees());
                String rejectionReason = period.getRejectionReason() != null ? period.getRejectionReason() : "";

                emailService.sendPayrollRejectedNotification(
                        submitter.getEmail(), recipientName,
                        periodStr, totalEmployees, totalNetSalary,
                        rejectionReason, submitter.getLanguage());
            }

            log.info("Đã gửi thông báo từ chối lương cho người gửi duyệt userId={}", period.getSubmittedBy());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo từ chối lương: {}", e.getMessage());
        }
    }

    /**
     * Lấy map user theo IDs
     */
    private Map<Long, UserEntity> getUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    /**
     * Format kỳ lương thành chuỗi "MM/yyyy"
     */
    private String formatPeriodString(PayrollPeriodEntity period) {
        return String.format("%02d/%d", period.getMonth(), period.getYear());
    }

    /**
     * Format tiền tệ cho email dựa theo region của company
     * - ja → ¥1,234,567
     * - vi → 1.234.567 ₫
     * - en → ¥1,234,567 (mặc định JPY)
     */
    private String formatCurrencyForEmail(BigDecimal amount, String companyLocale) {
        if (amount == null) {
            return "0";
        }
        java.util.Locale region = "vi".equals(companyLocale)
                ? Locale.of("vi", "VN")
                : Locale.JAPAN;
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(region);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }

    /**
     * Lấy tên hiển thị từ profile của user
     */
    private String getProfileName(UserEntity user) {
        if (user.getProfile() != null && user.getProfile().getName() != null) {
            return user.getProfile().getName();
        }
        return user.getEmail();
    }

    /**
     * Lấy tên hiển thị của user theo userId
     */
    private String getUserDisplayName(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository.findByIdAndDeletedFalse(userId)
                .map(this::getProfileName)
                .orElse("");
    }

    /**
     * Tính lương cho một nhân viên
     * - MONTHLY: Tính full lương cơ bản, overtime chỉ tính khi vượt ngày công chuẩn
     * - DAILY/HOURLY/SHIFT_BASED: Chỉ tính nếu có dữ liệu chấm công
     */
    private PayrollItemEntity calculateEmployeePayroll(
            PayrollPeriodEntity period,
            UserEntity employee,
            List<EmployeeSalaryItemEntity> allowances,
            List<EmployeeSalaryItemEntity> deductions,
            int standardWorkingDays,
            int standardWorkingHours,
            OvertimeConfig overtimeConfig) {

        // Lấy cấu hình lương của nhân viên
        Optional<EmployeeSalaryEntity> salaryConfigOpt = salaryRepository.findEffectiveSalary(
                employee.getId(), period.getPeriodEnd());

        if (salaryConfigOpt.isEmpty()) {
            log.warn("Không tìm thấy cấu hình lương cho nhân viên {}", employee.getId());
            return null;
        }

        EmployeeSalaryEntity salaryConfig = salaryConfigOpt.get();

        // Lấy dữ liệu chấm công trong kỳ
        List<AttendanceRecordEntity> attendanceRecords = attendanceRepository.findByEmployeeIdAndWorkDateBetween(
                employee.getId(), period.getPeriodStart(), period.getPeriodEnd());

        // Với loại lương không phải MONTHLY, nếu không có dữ liệu chấm công thì không tính
        if (salaryConfig.getSalaryType() != SalaryType.MONTHLY
                && attendanceRecords.isEmpty()) {
            log.info("Bỏ qua nhân viên {} (loại lương {}) vì không có dữ liệu chấm công trong kỳ", 
                    employee.getId(), salaryConfig.getSalaryType());
            return null;
        }

        // Tính toán thời gian làm việc
        int workingDays = attendanceRecords.size();
        int workingMinutes = attendanceRecords.stream()
                .mapToInt(a -> a.getWorkingMinutes() != null ? a.getWorkingMinutes() : 0)
                .sum();
        int workingHours = workingMinutes / 60;

        // Tính overtime dựa trên loại lương
        int regularOvertimeMinutes = 0;
        int nightOvertimeMinutes = 0;
        
        if (salaryConfig.getSalaryType() == SalaryType.MONTHLY) {
            // Với lương tháng: overtime = tổng giờ làm - (ngày công chuẩn × giờ/ngày)
            int standardMinutesPerMonth = standardWorkingDays * standardWorkingHours * 60;
            int excessMinutes = workingMinutes - standardMinutesPerMonth;
            if (excessMinutes > 0) {
                regularOvertimeMinutes = excessMinutes;
                log.info("Nhân viên {} (MONTHLY): làm {} phút, chuẩn {} phút, overtime {} phút",
                        employee.getId(), workingMinutes, standardMinutesPerMonth, regularOvertimeMinutes);
            }
        } else {
            // Với các loại lương khác: lấy overtime từ attendance records
            regularOvertimeMinutes = attendanceRecords.stream()
                    .mapToInt(a -> a.getOvertimeMinutes() != null ? a.getOvertimeMinutes() : 0)
                    .sum();
        }

        // Tính break
        int totalBreakMinutes = attendanceRecords.stream()
                .mapToInt(a -> a.getTotalBreakMinutes() != null ? a.getTotalBreakMinutes() : 0)
                .sum();

        // Tính lương cơ bản theo loại (MONTHLY luôn trả full)
        BigDecimal baseSalary = getBaseSalary(salaryConfig);
        BigDecimal calculatedBaseSalary = calculateBaseSalary(salaryConfig, workingDays, workingMinutes,
                attendanceRecords.size(), standardWorkingDays, standardWorkingHours);

        // Tính điều chỉnh giờ làm cho MONTHLY (dương = thêm, âm = trừ)
        BigDecimal workingTimeAdjustment = calculateWorkingTimeAdjustment(salaryConfig, workingMinutes,
                standardWorkingDays, standardWorkingHours);

        // Tính overtime pay (chỉ khi làm vượt chuẩn)
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        if (workingTimeAdjustment.compareTo(BigDecimal.ZERO) > 0) {
            // Làm vượt chuẩn → tính overtime pay
            totalOvertimePay = calculateOvertimePay(salaryConfig, regularOvertimeMinutes, nightOvertimeMinutes,
                    standardWorkingDays, standardWorkingHours, overtimeConfig);
        }

        // Tính allowances từ salary items
        BigDecimal totalAllowances = BigDecimal.ZERO;
        List<PayrollItemResponse.AllowanceDetailResponse> allowanceDetails = new ArrayList<>();
        for (EmployeeSalaryItemEntity allowance : allowances) {
            totalAllowances = totalAllowances.add(allowance.getAmount());
            String templateName = allowance.getTemplate() != null ? allowance.getTemplate().getName() : "";
            allowanceDetails.add(PayrollItemResponse.AllowanceDetailResponse.builder()
                    .code(String.valueOf(allowance.getTemplateId()))
                    .name(templateName)
                    .amount(allowance.getAmount())
                    .taxable(false)
                    .build());
        }
        
        // Thêm overtime vào allowances nếu có
        if (totalOvertimePay.compareTo(BigDecimal.ZERO) > 0) {
            totalAllowances = totalAllowances.add(totalOvertimePay);
            allowanceDetails.add(PayrollItemResponse.AllowanceDetailResponse.builder()
                    .code("OVERTIME")
                    .name("Tăng ca")
                    .amount(totalOvertimePay)
                    .taxable(false)
                    .build());
        }

        // Tính deductions từ salary items
        BigDecimal totalDeductions = BigDecimal.ZERO;
        List<PayrollItemResponse.DeductionDetailResponse> deductionDetails = new ArrayList<>();
        for (EmployeeSalaryItemEntity deduction : deductions) {
            BigDecimal deductionAmount = deduction.getAmount();
            totalDeductions = totalDeductions.add(deductionAmount);
            String templateName = deduction.getTemplate() != null ? deduction.getTemplate().getName() : "";
            deductionDetails.add(PayrollItemResponse.DeductionDetailResponse.builder()
                    .code(String.valueOf(deduction.getTemplateId()))
                    .name(templateName)
                    .amount(deductionAmount)
                    .percentage(null)
                    .calculatedAmount(deductionAmount)
                    .build());
        }
        
        // Thêm khấu trừ thiếu giờ nếu có (workingTimeAdjustment < 0)
        if (workingTimeAdjustment.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal shortfallDeduction = workingTimeAdjustment.abs();
            totalDeductions = totalDeductions.add(shortfallDeduction);
            deductionDetails.add(PayrollItemResponse.DeductionDetailResponse.builder()
                    .code("SHORTFALL")
                    .name("Thiếu giờ công")
                    .amount(shortfallDeduction)
                    .percentage(null)
                    .calculatedAmount(shortfallDeduction)
                    .build());
        }

        // Tính gross và net salary
        // Gross = Lương cơ bản + Phụ cấp (bao gồm tăng ca)
        // Net = Gross - Khấu trừ
        BigDecimal grossSalary = calculatedBaseSalary.add(totalAllowances);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        // Tạo PayrollItemEntity
        PayrollItemEntity item = new PayrollItemEntity();
        item.setPayrollPeriodId(period.getId());
        item.setEmployeeId(employee.getId());
        item.setSalaryType(salaryConfig.getSalaryType());
        item.setBaseSalary(baseSalary);
        item.setCalculatedBaseSalary(calculatedBaseSalary);
        item.setWorkingDays(workingDays);
        item.setWorkingHours(workingHours);
        item.setWorkingMinutes(workingMinutes);
        item.setRegularOvertimeMinutes(regularOvertimeMinutes);
        item.setNightOvertimeMinutes(nightOvertimeMinutes);
        item.setHolidayOvertimeMinutes(0);
        item.setWeekendOvertimeMinutes(0);
        item.setTotalOvertimePay(totalOvertimePay);
        item.setTotalBreakMinutes(totalBreakMinutes);
        item.setBreakDeductionAmount(BigDecimal.ZERO);
        item.setTotalAllowances(totalAllowances);
        item.setTotalDeductions(totalDeductions);
        item.setGrossSalary(grossSalary);
        item.setNetSalary(netSalary);
        item.setStatus(PayrollItemStatus.CALCULATED);

        // Serialize allowance và deduction details thành JSON
        try {
            item.setAllowanceDetails(objectMapper.writeValueAsString(allowanceDetails));
            item.setDeductionDetails(objectMapper.writeValueAsString(deductionDetails));
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize allowance/deduction details", e);
        }

        return item;
    }

    /**
     * Lấy lương cơ bản từ config
     */
    private BigDecimal getBaseSalary(EmployeeSalaryEntity config) {
        return switch (config.getSalaryType()) {
            case MONTHLY -> config.getMonthlySalary();
            case DAILY -> config.getDailyRate();
            case HOURLY -> config.getHourlyRate();
            case SHIFT_BASED -> config.getShiftRate();
        };
    }

    /**
     * Tính lương cơ bản theo loại
     * - MONTHLY: Tính full lương cơ bản (không trừ ở đây, trừ ở deductions)
     * - DAILY/HOURLY/SHIFT_BASED: Tính theo số ngày/giờ/ca thực tế
     */
    private BigDecimal calculateBaseSalary(EmployeeSalaryEntity config, int workingDays, int workingMinutes, int shifts,
            int standardWorkingDays, int standardWorkingHours) {
        return switch (config.getSalaryType()) {
            case MONTHLY -> {
                // Lương tháng: luôn trả full, việc trừ/cộng sẽ xử lý ở allowances/deductions
                if (config.getMonthlySalary() == null) {
                    yield BigDecimal.ZERO;
                }
                yield config.getMonthlySalary();
            }
            case DAILY -> {
                if (config.getDailyRate() == null) {
                    yield BigDecimal.ZERO;
                }
                yield config.getDailyRate().multiply(BigDecimal.valueOf(workingDays));
            }
            case HOURLY -> {
                if (config.getHourlyRate() == null) {
                    yield BigDecimal.ZERO;
                }
                int workingHours = workingMinutes / 60;
                yield config.getHourlyRate().multiply(BigDecimal.valueOf(workingHours));
            }
            case SHIFT_BASED -> {
                if (config.getShiftRate() == null) {
                    yield BigDecimal.ZERO;
                }
                yield config.getShiftRate().multiply(BigDecimal.valueOf(shifts));
            }
        };
    }
    
    /**
     * Tính điều chỉnh giờ làm cho nhân viên MONTHLY
     * - Nếu làm vượt chuẩn → trả về số dương (thêm vào allowances)
     * - Nếu làm thiếu chuẩn → trả về số âm (thêm vào deductions)
     * - Nếu đúng chuẩn → trả về 0
     */
    private BigDecimal calculateWorkingTimeAdjustment(EmployeeSalaryEntity config, int workingMinutes,
            int standardWorkingDays, int standardWorkingHours) {
        if (config.getSalaryType() != SalaryType.MONTHLY || config.getMonthlySalary() == null) {
            return BigDecimal.ZERO;
        }
        
        int standardMinutesPerMonth = standardWorkingDays * standardWorkingHours * 60;
        int diffMinutes = workingMinutes - standardMinutesPerMonth;
        
        if (diffMinutes == 0) {
            return BigDecimal.ZERO;
        }
        
        // Tính lương theo phút
        BigDecimal salaryPerMinute = config.getMonthlySalary().divide(
                BigDecimal.valueOf(standardMinutesPerMonth), 4, RoundingMode.HALF_UP);
        
        // Trả về số tiền điều chỉnh (dương = thêm, âm = trừ)
        return salaryPerMinute.multiply(BigDecimal.valueOf(diffMinutes)).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền tăng ca
     * @param config Cấu hình lương nhân viên
     * @param regularOT Số phút tăng ca thường
     * @param nightOT Số phút tăng ca đêm
     * @param standardWorkingDays Số ngày công chuẩn/tháng từ company settings
     * @param standardWorkingHours Số giờ làm việc chuẩn/ngày từ company settings
     * @param overtimeConfig Cấu hình tăng ca từ company settings
     */
    private BigDecimal calculateOvertimePay(EmployeeSalaryEntity config, int regularOT, int nightOT,
            int standardWorkingDays, int standardWorkingHours, OvertimeConfig overtimeConfig) {
        
        // Nếu overtime bị tắt hoặc không có hệ số, trả về 0
        if (overtimeConfig == null || !Boolean.TRUE.equals(overtimeConfig.getOvertimeEnabled())) {
            return BigDecimal.ZERO;
        }
        
        // Lấy hệ số tăng ca từ config, nếu null thì = 0 (không tính overtime)
        BigDecimal regularOTRate = overtimeConfig.getRegularOvertimeRate();
        BigDecimal nightOTRate = overtimeConfig.getNightOvertimeRate();
        
        if (regularOTRate == null) {
            regularOTRate = BigDecimal.ZERO;
        }
        if (nightOTRate == null) {
            nightOTRate = BigDecimal.ZERO;
        }
        
        // Nếu cả 2 hệ số đều = 0, không cần tính
        if (regularOTRate.compareTo(BigDecimal.ZERO) == 0 && nightOTRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Tính hourly rate từ config
        BigDecimal hourlyRate = switch (config.getSalaryType()) {
            case MONTHLY -> config.getMonthlySalary() != null
                    ? config.getMonthlySalary().divide(
                            BigDecimal.valueOf(standardWorkingDays * standardWorkingHours), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            case DAILY -> config.getDailyRate() != null
                    ? config.getDailyRate().divide(BigDecimal.valueOf(standardWorkingHours), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            case HOURLY -> config.getHourlyRate() != null ? config.getHourlyRate() : BigDecimal.ZERO;
            case SHIFT_BASED -> config.getShiftRate() != null
                    ? config.getShiftRate().divide(BigDecimal.valueOf(standardWorkingHours), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        };

        BigDecimal regularOTPay = hourlyRate.multiply(regularOTRate)
                .multiply(BigDecimal.valueOf(regularOT))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal nightOTPay = hourlyRate.multiply(nightOTRate)
                .multiply(BigDecimal.valueOf(nightOT))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return regularOTPay.add(nightOTPay);
    }

    /**
     * Cập nhật tổng của period sau khi điều chỉnh item
     */
    private void updatePeriodTotals(Long periodId) {
        BigDecimal totalGross = itemRepository.sumGrossSalaryByPeriodId(periodId);
        BigDecimal totalNet = itemRepository.sumNetSalaryByPeriodId(periodId);
        // PayrollItem không có soft delete
        long itemCount = itemRepository.countByPayrollPeriodId(periodId);

        PayrollPeriodEntity period = getPeriodOrThrow(periodId);
        period.setTotalGrossSalary(totalGross);
        period.setTotalNetSalary(totalNet);
        period.setTotalEmployees((int) itemCount);
        periodRepository.save(period);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse rejectPayroll(Long periodId, String reason) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Kiểm tra trạng thái - chỉ cho phép reject khi REVIEWING
        if (period.getStatus() != PayrollPeriodStatus.REVIEWING) {
            throw new BadRequestException("Chỉ có thể từ chối khi kỳ lương đang ở trạng thái REVIEWING",
                    ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        // Gửi thông báo cho người gửi duyệt (trước khi xóa submittedBy)
        notifySubmitterOnPayrollRejected(period);

        // Chuyển về DRAFT để có thể chỉnh sửa lại
        period.setStatus(PayrollPeriodStatus.DRAFT);
        period.setRejectionReason(reason);
        period.setSubmittedBy(null);
        period.setApprovedBy(null);
        period.setApprovedAt(null);
        period = periodRepository.save(period);

        Set<Long> userIds = new HashSet<>();
        userIds.add(period.getCreatedBy());
        Map<Long, UserEntity> userMap = getUserMap(new ArrayList<>(userIds));

        log.info("Từ chối kỳ lương {} với lý do: {}", periodId, reason);
        return mapper.toResponse(period, userMap);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollItemResponse> getPayrollItems(Long periodId, Long employeeId, String status, Pageable pageable) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        Page<PayrollItemEntity> itemPage;
        if (employeeId != null && status != null) {
            itemPage = itemRepository.findByPayrollPeriodIdAndEmployeeIdAndStatus(
                    periodId, employeeId, PayrollItemStatus.valueOf(status), pageable);
        } else if (employeeId != null) {
            itemPage = itemRepository.findByPayrollPeriodIdAndEmployeeId(periodId, employeeId, pageable);
        } else if (status != null) {
            itemPage = itemRepository.findByPayrollPeriodIdAndStatus(
                    periodId, PayrollItemStatus.valueOf(status), pageable);
        } else {
            itemPage = itemRepository.findByPayrollPeriodId(periodId, pageable);
        }

        // Lấy thông tin employees
        List<Long> employeeIds = itemPage.getContent().stream()
                .map(PayrollItemEntity::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserEntity> userMap = getUserMap(employeeIds);

        return itemPage.map(item -> mapper.toItemResponse(item, userMap, period.getYear(), period.getMonth(), period.getPaidAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollItemResponse getPayrollItemById(Long itemId) {
        PayrollItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy payroll item", ErrorCode.PAYROLL_ITEM_NOT_FOUND));

        PayrollPeriodEntity period = periodRepository.findById(item.getPayrollPeriodId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ lương", ErrorCode.PAYROLL_PERIOD_NOT_FOUND));

        UserEntity employee = userRepository.findById(item.getEmployeeId())
                .orElseThrow(() -> NotFoundException.user(item.getEmployeeId()));

        // Tạo map với 1 employee
        Map<Long, UserEntity> userMap = new HashMap<>();
        userMap.put(employee.getId(), employee);

        return mapper.toItemResponse(item, userMap, period.getYear(), period.getMonth(), period.getPaidAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollItemResponse> getEmployeePayslips(Long employeeId, String status, Pageable pageable) {
        // Nếu filter theo status, cần join với payroll_periods
        Page<PayrollItemEntity> itemPage;
        
        if (status != null && !status.isEmpty()) {
            // Filter theo status của PayrollPeriod (DRAFT, FINALIZED, PAID)
            try {
                PayrollPeriodStatus periodStatus = PayrollPeriodStatus.valueOf(status);
                
                // Lấy tất cả periods có status này
                List<PayrollPeriodEntity> periods = periodRepository.findByStatus(periodStatus);
                Set<Long> periodIds = periods.stream()
                        .map(PayrollPeriodEntity::getId)
                        .collect(Collectors.toSet());
                
                if (periodIds.isEmpty()) {
                    // Không có period nào với status này
                    return Page.empty(pageable);
                }
                
                // Lấy items của employee trong các periods này
                itemPage = itemRepository.findByEmployeeIdAndPayrollPeriodIdIn(employeeId, periodIds, pageable);
            } catch (IllegalArgumentException e) {
                // Status không hợp lệ, trả về empty
                log.warn("Invalid payroll period status: {}", status);
                return Page.empty(pageable);
            }
        } else {
            // Không filter, lấy tất cả
            itemPage = itemRepository.findByEmployeeId(employeeId, pageable);
        }

        // Lấy thông tin employee
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        Map<Long, UserEntity> userMap = new HashMap<>();
        userMap.put(employee.getId(), employee);

        Set<Long> periodIds = itemPage.getContent().stream()
                .map(PayrollItemEntity::getPayrollPeriodId)
                .collect(Collectors.toSet());
        Map<Long, PayrollPeriodEntity> periodMap = periodRepository.findAllById(periodIds).stream()
                .collect(Collectors.toMap(PayrollPeriodEntity::getId, Function.identity()));

        return itemPage.map(item -> {
            PayrollPeriodEntity p = periodMap.get(item.getPayrollPeriodId());
            return mapper.toItemResponse(
                item, 
                userMap, 
                p != null ? p.getYear() : null, 
                p != null ? p.getMonth() : null,
                p != null ? p.getPaidAt() : null
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollItemResponse> getAllCompanyPayslips(Long employeeId, String status, Pageable pageable) {
        // Lấy tất cả periods có status = PAID
        List<PayrollPeriodEntity> paidPeriods = periodRepository.findByStatus(PayrollPeriodStatus.PAID);
        if (paidPeriods.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> paidPeriodIds = paidPeriods.stream()
                .map(PayrollPeriodEntity::getId)
                .collect(Collectors.toSet());

        // Lấy items từ paid periods, filter theo employeeId nếu có
        Page<PayrollItemEntity> itemPage;
        if (employeeId != null) {
            itemPage = itemRepository.findByEmployeeIdAndPayrollPeriodIdIn(employeeId, paidPeriodIds, pageable);
        } else {
            // Lấy tất cả items từ paid periods
            itemPage = itemRepository.findAll(pageable);
            // Filter chỉ lấy items thuộc paid periods
            List<PayrollItemEntity> filteredItems = itemPage.getContent().stream()
                    .filter(item -> paidPeriodIds.contains(item.getPayrollPeriodId()))
                    .collect(Collectors.toList());
            itemPage = new PageImpl<>(filteredItems, pageable, filteredItems.size());
        }

        // Lấy tất cả employee IDs
        Set<Long> employeeIds = itemPage.getContent().stream()
                .map(PayrollItemEntity::getEmployeeId)
                .collect(Collectors.toSet());

        // Lấy thông tin employees
        Map<Long, UserEntity> userMap = userRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        Set<Long> periodIds = itemPage.getContent().stream()
                .map(PayrollItemEntity::getPayrollPeriodId)
                .collect(Collectors.toSet());
        Map<Long, PayrollPeriodEntity> periodMap = periodRepository.findAllById(periodIds).stream()
                .collect(Collectors.toMap(PayrollPeriodEntity::getId, Function.identity()));

        List<PayrollItemResponse> responses = itemPage.getContent().stream()
                .map(item -> {
                    PayrollPeriodEntity p = periodMap.get(item.getPayrollPeriodId());
                    return mapper.toItemResponse(
                            item, 
                            userMap, 
                            p != null ? p.getYear() : null, 
                            p != null ? p.getMonth() : null,
                            p != null ? p.getPaidAt() : null
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, responses.size());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePayslipPdf(Long itemId) {
        // Lấy thông tin payroll item
        PayrollItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chi tiết lương", ErrorCode.PAYROLL_ITEM_NOT_FOUND));

        // Lấy thông tin period
        PayrollPeriodEntity period = periodRepository.findById(item.getPayrollPeriodId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ lương", ErrorCode.PAYROLL_PERIOD_NOT_FOUND));

        // Lấy thông tin employee
        UserEntity employee = userRepository.findById(item.getEmployeeId())
                .orElseThrow(() -> NotFoundException.user(item.getEmployeeId()));

        // Lấy thông tin company từ master DB bằng JDBC
        String tenantDomain = TenantContext.getCurrentTenant();
        CompanyEntity company = null;
        if (tenantDomain != null) {
            company = getCompanyFromMasterDb(tenantDomain);
        }

        // Convert PayrollItemEntity sang PayrollRecordResponse để dùng với PayslipPdfGenerator
        com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse recordResponse = 
            convertItemToRecordResponse(item, period);

        // Sử dụng PayslipPdfGenerator để tạo PDF
        return pdfGenerator.generate(recordResponse, employee, company);
    }

    /**
     * Lấy thông tin company từ master DB bằng JDBC
     */
    private CompanyEntity getCompanyFromMasterDb(String tenantDomain) {
        String sql = "SELECT id, name, email, region, logo FROM companies WHERE tenant_domain = ? AND deleted = false";
        
        try {
            return masterJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                CompanyEntity company = new CompanyEntity();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setEmail(rs.getString("email"));
                company.setRegion(rs.getString("region"));
                company.setLogo(rs.getString("logo"));
                return company;
            }, tenantDomain);
        } catch (Exception e) {
            log.warn("Không tìm thấy company với tenant: {}", tenantDomain);
            return null;
        }
    }

    /**
     * Convert PayrollItemEntity sang PayrollRecordResponse để tương thích với PayslipPdfGenerator
     */
    private com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse convertItemToRecordResponse(
            PayrollItemEntity item, PayrollPeriodEntity period) {
        
        com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse response = 
            new com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse();
        
        response.setYear(period.getYear());
        response.setMonth(period.getMonth());
        response.setEmployeeId(item.getEmployeeId());
        response.setSalaryType(item.getSalaryType());
        response.setSalaryRate(item.getBaseSalary());
        response.setBaseSalary(item.getCalculatedBaseSalary());
        response.setWorkingDays(item.getWorkingDays());
        response.setWorkingHours(item.getWorkingMinutes());
        response.setRegularOvertimeHours(item.getRegularOvertimeMinutes());
        response.setNightOvertimeHours(item.getNightOvertimeMinutes());
        response.setHolidayOvertimeHours(item.getHolidayOvertimeMinutes());
        response.setTotalOvertimePay(item.getTotalOvertimePay());
        response.setGrossSalary(item.getGrossSalary());
        response.setTotalDeductions(item.getTotalDeductions());
        response.setNetSalary(item.getNetSalary());

        // Parse allowance details từ JSON
        if (item.getAllowanceDetails() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<PayrollItemResponse.AllowanceDetailResponse> allowanceDetails = 
                    objectMapper.readValue(item.getAllowanceDetails(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            PayrollItemResponse.AllowanceDetailResponse.class));
                
                List<com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.AllowanceItemResponse> allowances = 
                    allowanceDetails.stream()
                        .map(a -> {
                            com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.AllowanceItemResponse item2 = 
                                new com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.AllowanceItemResponse();
                            item2.setCode(a.getCode());
                            item2.setName(a.getName());
                            item2.setAmount(a.getAmount());
                            return item2;
                        })
                        .collect(Collectors.toList());
                response.setAllowanceDetails(allowances);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse allowance details", e);
            }
        }

        // Parse deduction details từ JSON
        if (item.getDeductionDetails() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<PayrollItemResponse.DeductionDetailResponse> deductionDetails = 
                    objectMapper.readValue(item.getDeductionDetails(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            PayrollItemResponse.DeductionDetailResponse.class));
                
                List<com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.DeductionItemResponse> deductions = 
                    deductionDetails.stream()
                        .map(d -> {
                            com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.DeductionItemResponse item2 = 
                                new com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse.DeductionItemResponse();
                            item2.setCode(d.getCode());
                            item2.setName(d.getName());
                            item2.setAmount(d.getCalculatedAmount() != null ? d.getCalculatedAmount() : d.getAmount());
                            return item2;
                        })
                        .collect(Collectors.toList());
                response.setDeductionDetails(deductions);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse deduction details", e);
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateAllPayslipsZip(Long periodId) {
        // Lấy thông tin period
        PayrollPeriodEntity period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ lương", ErrorCode.PAYROLL_PERIOD_NOT_FOUND));

        // Lấy tất cả payroll items của period
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);
        
        if (items.isEmpty()) {
            throw new BadRequestException("Kỳ lương chưa có dữ liệu", ErrorCode.PAYROLL_CALCULATION_FAILED);
        }

        // Lấy thông tin company từ master DB
        String tenantDomain = TenantContext.getCurrentTenant();
        CompanyEntity company = null;
        if (tenantDomain != null) {
            company = getCompanyFromMasterDb(tenantDomain);
        }

        // Lấy region của company để format filename
        String region = company != null && company.getRegion() != null ? company.getRegion() : "vi";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Generate PDF cho từng item và thêm vào ZIP
            for (PayrollItemEntity item : items) {
                try {
                    // Lấy thông tin employee
                    UserEntity employee = userRepository.findById(item.getEmployeeId())
                            .orElseThrow(() -> NotFoundException.user(item.getEmployeeId()));

                    // Convert item sang response để generate PDF
                    com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse recordResponse = 
                        convertItemToRecordResponse(item, period);

                    // Generate PDF
                    byte[] pdfData = pdfGenerator.generate(recordResponse, employee, company);

                    // Tạo filename sử dụng method chung
                    String filename = formatPayslipFilename(employee.getEmployeeCode(), period.getYear(), period.getMonth(), region);

                    // Thêm vào ZIP
                    ZipEntry zipEntry = new ZipEntry(filename);
                    zos.putNextEntry(zipEntry);
                    zos.write(pdfData);
                    zos.closeEntry();

                    log.debug("Added payslip to ZIP: {}", filename);

                } catch (Exception e) {
                    log.error("Lỗi khi generate PDF cho employee {}: {}", item.getEmployeeId(), e.getMessage(), e);
                    // Continue với items khác
                }
            }

            zos.finish();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Lỗi khi tạo ZIP file: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tạo file ZIP", ErrorCode.PAYROLL_CALCULATION_FAILED);
        }
    }

    @Override
    public String formatPayslipFilename(String employeeCode, Integer year, Integer month, String region) {
        String label = getPayslipLabel(region);
        return String.format("%s_%s_%d-%02d.pdf", label, employeeCode, year, month);
    }

    /**
     * Lấy label "payslip" theo ngôn ngữ (method private để dùng chung)
     */
    private String getPayslipLabel(String region) {
        if (region == null) {
            return "payslip";
        }
        return switch (region.toLowerCase()) {
            case "vi" -> "phieu_luong";
            case "ja" -> "給与明細";
            default -> "payslip";
        };
    }

    @Override
    @Transactional
    public int rollbackPayrollPeriodsToDraft(Long companyId, Integer year, Integer month) {
        log.info("Rollback payroll periods - companyId: {}, year: {}, month: {}", companyId, year, month);

        if (companyId != null) {
            // Rollback cho 1 công ty cụ thể
            return rollbackForSingleCompany(companyId, year, month);
        } else {
            // Rollback cho tất cả công ty
            return rollbackForAllCompanies(year, month);
        }
    }

    private int rollbackForSingleCompany(Long companyId, Integer year, Integer month) {
        // Lấy tenant domain của company
        CompanyEntity company = companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        String tenantDomain = company.getTenantDomain();
        DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);

        if (tenantDs == null) {
            log.warn("Tenant DataSource not found for company {}: {}", companyId, tenantDomain);
            return 0;
        }

        return rollbackWithJdbc(tenantDs, year, month);
    }

    private int rollbackForAllCompanies(Integer year, Integer month) {
        List<CompanyEntity> companies = companyRepository.findAllByDeletedFalse();
        int totalRollback = 0;

        for (CompanyEntity company : companies) {
            try {
                String tenantDomain = company.getTenantDomain();
                DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);

                if (tenantDs == null) {
                    log.warn("Tenant DataSource not found for company {}: {}", company.getId(), tenantDomain);
                    continue;
                }

                int count = rollbackWithJdbc(tenantDs, year, month);
                totalRollback += count;
                log.info("Rollback {} periods cho company {}", count, company.getId());

            } catch (Exception e) {
                log.error("Lỗi khi rollback cho company {}: {}", company.getId(), e.getMessage(), e);
            }
        }

        return totalRollback;
    }

    private int rollbackWithJdbc(DataSource tenantDs, Integer year, Integer month) {
        try (Connection conn = tenantDs.getConnection()) {
            // Build query với điều kiện động
            // Rollback tất cả status không phải DRAFT về DRAFT
            StringBuilder periodSql = new StringBuilder(
                    "UPDATE payroll_periods SET status = 'DRAFT', " +
                            "submitted_by = NULL, rejection_reason = NULL, " +
                            "approved_by = NULL, approved_at = NULL, " +
                            "paid_at = NULL, payment_reference = NULL, " +
                            "updated_at = NOW() " +
                            "WHERE status IN ('REVIEWING', 'APPROVED', 'PAID')");

            if (year != null) {
                periodSql.append(" AND year = ?");
            }
            if (month != null) {
                periodSql.append(" AND month = ?");
            }

            int periodCount;
            try (PreparedStatement ps = conn.prepareStatement(periodSql.toString())) {
                int paramIndex = 1;
                if (year != null) {
                    ps.setInt(paramIndex++, year);
                }
                if (month != null) {
                    ps.setInt(paramIndex, month);
                }
                periodCount = ps.executeUpdate();
            }

            // Rollback payroll_items.is_confirmed về false cho các period đã rollback
            StringBuilder itemSql = new StringBuilder(
                    "UPDATE payroll_items SET is_confirmed = false, updated_at = NOW() " +
                            "WHERE payroll_period_id IN (" +
                            "SELECT id FROM payroll_periods WHERE status = 'DRAFT'");

            if (year != null) {
                itemSql.append(" AND year = ?");
            }
            if (month != null) {
                itemSql.append(" AND month = ?");
            }
            itemSql.append(")");

            try (PreparedStatement ps = conn.prepareStatement(itemSql.toString())) {
                int paramIndex = 1;
                if (year != null) {
                    ps.setInt(paramIndex++, year);
                }
                if (month != null) {
                    ps.setInt(paramIndex, month);
                }
                int itemCount = ps.executeUpdate();
                log.info("Rollback {} payroll items về is_confirmed = false", itemCount);
            }

            log.info("Rollback {} payroll periods về DRAFT", periodCount);
            return periodCount;

        } catch (Exception e) {
            log.error("Lỗi khi rollback payroll periods: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public void deletePayrollPeriod(Long periodId) {
        PayrollPeriodEntity period = getPeriodOrThrow(periodId);

        // Chỉ cho phép xóa khi status = DRAFT
        if (period.getStatus() != PayrollPeriodStatus.DRAFT) {
            throw new BadRequestException(
                "Chỉ có thể xóa kỳ lương ở trạng thái DRAFT",
                ErrorCode.PAYROLL_INVALID_STATUS_TRANSITION);
        }

        // Xóa tất cả payroll items của period này trước
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);
        if (!items.isEmpty()) {
            itemRepository.deleteAll(items);
            log.info("Đã xóa {} payroll items của period {}", items.size(), periodId);
        }

        // Xóa period
        periodRepository.delete(period);
        log.info("Đã xóa payroll period {}", periodId);
    }
}
