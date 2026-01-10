package com.tamabee.api_hr.service.company.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.request.wallet.PaymentRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollAdjustmentRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollPeriodRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodDetailResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollPeriodResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.payroll.*;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.*;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.PayrollPeriodMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.payroll.*;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final EmployeeAllowanceRepository allowanceRepository;
    private final EmployeeDeductionRepository deductionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PayrollPeriodMapper mapper;
    private final ObjectMapper objectMapper;

    // Số ngày làm việc tiêu chuẩn trong tháng (dùng cho tính lương MONTHLY)
    private static final int STANDARD_WORKING_DAYS = 22;

    @Override
    @Transactional
    public PayrollPeriodResponse createPayrollPeriod(PayrollPeriodRequest request, Long createdBy) {
        // PayrollPeriod không có soft delete
        if (periodRepository.existsByYearAndMonth(request.getYear(), request.getMonth())) {
            throw new ConflictException("Kỳ lương đã tồn tại cho tháng " + request.getMonth() + "/" + request.getYear(),
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

        // Xóa các payroll items cũ (nếu có) để tính lại
        // PayrollItem không có soft delete - xóa thẳng
        List<PayrollItemEntity> existingItems = itemRepository.findByPayrollPeriodId(periodId);
        itemRepository.deleteAll(existingItems);

        // Lấy danh sách nhân viên active
        List<UserEntity> employees = userRepository.findByDeletedFalse();
        List<Long> employeeIds = employees.stream().map(UserEntity::getId).collect(Collectors.toList());

        // Lấy dữ liệu cần thiết cho tính lương
        LocalDate periodStart = period.getPeriodStart();
        LocalDate periodEnd = period.getPeriodEnd();

        // Lấy allowances và deductions cho tất cả nhân viên trong kỳ
        List<EmployeeAllowanceEntity> allAllowances = allowanceRepository
                .findByEmployeeIdsAndEffectiveDateRange(employeeIds, periodStart, periodEnd);
        List<EmployeeDeductionEntity> allDeductions = deductionRepository
                .findByEmployeeIdsAndEffectiveDateRange(employeeIds, periodStart, periodEnd);

        // Group theo employeeId
        Map<Long, List<EmployeeAllowanceEntity>> allowancesByEmployee = allAllowances.stream()
                .collect(Collectors.groupingBy(EmployeeAllowanceEntity::getEmployeeId));
        Map<Long, List<EmployeeDeductionEntity>> deductionsByEmployee = allDeductions.stream()
                .collect(Collectors.groupingBy(EmployeeDeductionEntity::getEmployeeId));

        // Tính lương cho từng nhân viên
        List<PayrollItemEntity> payrollItems = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (UserEntity employee : employees) {
            PayrollItemEntity item = calculateEmployeePayroll(
                    period, employee,
                    allowancesByEmployee.getOrDefault(employee.getId(), Collections.emptyList()),
                    deductionsByEmployee.getOrDefault(employee.getId(), Collections.emptyList()));

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
                .map(item -> mapper.toItemResponse(item, userMap))
                .collect(Collectors.toList());

        return mapper.toDetailResponse(period, itemResponses, userMap);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollPeriodResponse> getPayrollPeriods(Pageable pageable) {
        Page<PayrollPeriodEntity> periods = periodRepository.findAllPaged(pageable);

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
        item.setAdjustedAt(LocalDateTime.now());
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

        return mapper.toItemResponse(item, userMap);
    }

    @Override
    @Transactional
    public PayrollPeriodResponse submitForReview(Long periodId) {
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
        period = periodRepository.save(period);

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
        period.setApprovedAt(LocalDateTime.now());
        period = periodRepository.save(period);

        // Cập nhật status của tất cả items thành CONFIRMED
        // PayrollItem không có soft delete
        List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(periodId);
        for (PayrollItemEntity item : items) {
            item.setStatus(PayrollItemStatus.CONFIRMED);
        }
        itemRepository.saveAll(items);

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
        period.setPaidAt(LocalDateTime.now());
        if (request != null && request.getPaymentReference() != null) {
            period.setPaymentReference(request.getPaymentReference());
        }
        period = periodRepository.save(period);

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
     * Tính lương cho một nhân viên
     */
    private PayrollItemEntity calculateEmployeePayroll(
            PayrollPeriodEntity period,
            UserEntity employee,
            List<EmployeeAllowanceEntity> allowances,
            List<EmployeeDeductionEntity> deductions) {

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

        // Tính toán thời gian làm việc
        int workingDays = attendanceRecords.size();
        int workingMinutes = attendanceRecords.stream()
                .mapToInt(a -> a.getWorkingMinutes() != null ? a.getWorkingMinutes() : 0)
                .sum();
        int workingHours = workingMinutes / 60;

        // Tính overtime (chỉ có overtimeMinutes trong entity hiện tại)
        int regularOvertimeMinutes = attendanceRecords.stream()
                .mapToInt(a -> a.getOvertimeMinutes() != null ? a.getOvertimeMinutes() : 0)
                .sum();
        // Night overtime chưa được track trong AttendanceRecordEntity, set = 0
        int nightOvertimeMinutes = 0;

        // Tính break
        int totalBreakMinutes = attendanceRecords.stream()
                .mapToInt(a -> a.getTotalBreakMinutes() != null ? a.getTotalBreakMinutes() : 0)
                .sum();

        // Tính lương cơ bản theo loại
        BigDecimal baseSalary = getBaseSalary(salaryConfig);
        BigDecimal calculatedBaseSalary = calculateBaseSalary(salaryConfig, workingDays, workingHours,
                attendanceRecords.size());

        // Tính overtime pay (đơn giản hóa - có thể mở rộng sau)
        BigDecimal totalOvertimePay = calculateOvertimePay(salaryConfig, regularOvertimeMinutes, nightOvertimeMinutes);

        // Tính allowances
        BigDecimal totalAllowances = BigDecimal.ZERO;
        List<PayrollItemResponse.AllowanceDetailResponse> allowanceDetails = new ArrayList<>();
        for (EmployeeAllowanceEntity allowance : allowances) {
            totalAllowances = totalAllowances.add(allowance.getAmount());
            allowanceDetails.add(PayrollItemResponse.AllowanceDetailResponse.builder()
                    .code(allowance.getAllowanceCode())
                    .name(allowance.getAllowanceName())
                    .amount(allowance.getAmount())
                    .taxable(allowance.getTaxable())
                    .build());
        }

        // Tính deductions
        BigDecimal totalDeductions = BigDecimal.ZERO;
        List<PayrollItemResponse.DeductionDetailResponse> deductionDetails = new ArrayList<>();
        for (EmployeeDeductionEntity deduction : deductions) {
            BigDecimal deductionAmount = deduction.getAmount();
            if (deductionAmount == null && deduction.getPercentage() != null) {
                // Tính theo phần trăm của lương cơ bản
                deductionAmount = calculatedBaseSalary.multiply(deduction.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            if (deductionAmount != null) {
                totalDeductions = totalDeductions.add(deductionAmount);
                deductionDetails.add(PayrollItemResponse.DeductionDetailResponse.builder()
                        .code(deduction.getDeductionCode())
                        .name(deduction.getDeductionName())
                        .amount(deduction.getAmount())
                        .percentage(deduction.getPercentage())
                        .calculatedAmount(deductionAmount)
                        .build());
            }
        }

        // Tính gross và net salary
        BigDecimal grossSalary = calculatedBaseSalary
                .add(totalOvertimePay)
                .add(totalAllowances)
                .subtract(totalDeductions);
        BigDecimal netSalary = grossSalary; // Có thể trừ thêm thuế sau

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
     */
    private BigDecimal calculateBaseSalary(EmployeeSalaryEntity config, int workingDays, int workingHours, int shifts) {
        return switch (config.getSalaryType()) {
            case MONTHLY -> {
                // Prorate theo số ngày làm việc thực tế
                if (config.getMonthlySalary() == null)
                    yield BigDecimal.ZERO;
                yield config.getMonthlySalary()
                        .multiply(BigDecimal.valueOf(workingDays))
                        .divide(BigDecimal.valueOf(STANDARD_WORKING_DAYS), 2, RoundingMode.HALF_UP);
            }
            case DAILY -> {
                if (config.getDailyRate() == null)
                    yield BigDecimal.ZERO;
                yield config.getDailyRate().multiply(BigDecimal.valueOf(workingDays));
            }
            case HOURLY -> {
                if (config.getHourlyRate() == null)
                    yield BigDecimal.ZERO;
                yield config.getHourlyRate().multiply(BigDecimal.valueOf(workingHours));
            }
            case SHIFT_BASED -> {
                if (config.getShiftRate() == null)
                    yield BigDecimal.ZERO;
                yield config.getShiftRate().multiply(BigDecimal.valueOf(shifts));
            }
        };
    }

    /**
     * Tính tiền tăng ca (đơn giản hóa)
     */
    private BigDecimal calculateOvertimePay(EmployeeSalaryEntity config, int regularOT, int nightOT) {
        // Tính hourly rate từ config
        BigDecimal hourlyRate = switch (config.getSalaryType()) {
            case MONTHLY -> config.getMonthlySalary() != null
                    ? config.getMonthlySalary().divide(BigDecimal.valueOf(STANDARD_WORKING_DAYS * 8), 2,
                            RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            case DAILY -> config.getDailyRate() != null
                    ? config.getDailyRate().divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            case HOURLY -> config.getHourlyRate() != null ? config.getHourlyRate() : BigDecimal.ZERO;
            case SHIFT_BASED -> config.getShiftRate() != null
                    ? config.getShiftRate().divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        };

        // Hệ số tăng ca (có thể lấy từ company settings sau)
        BigDecimal regularOTRate = BigDecimal.valueOf(1.5);
        BigDecimal nightOTRate = BigDecimal.valueOf(2.0);

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
}
