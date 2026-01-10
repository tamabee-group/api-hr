package com.tamabee.api_hr.service.company.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.response.*;
import com.tamabee.api_hr.dto.result.*;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.*;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.PayrollMapper;
import com.tamabee.api_hr.repository.*;
import com.tamabee.api_hr.service.calculator.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.IOvertimeCalculator;
import com.tamabee.api_hr.service.calculator.IPayrollCalculator;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import com.tamabee.api_hr.service.company.IPayrollService;
import com.tamabee.api_hr.service.core.INotificationEmailService;
import com.tamabee.api_hr.service.core.PayslipPdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation quản lý tính lương và thanh toán.
 * Tích hợp các calculator để tính toán lương, phụ cấp, khấu trừ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements IPayrollService {

    private final PayrollRecordRepository payrollRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final UserRepository userRepository;
    private final HolidayRepository holidayRepository;
    private final CompanyRepository companyRepository;
    private final ICompanySettingsService companySettingsService;
    private final IPayrollCalculator payrollCalculator;
    private final IBreakCalculator breakCalculator;
    private final IOvertimeCalculator overtimeCalculator;
    private final PayrollMapper payrollMapper;
    private final ObjectMapper objectMapper;
    private final INotificationEmailService notificationEmailService;
    private final PayslipPdfGenerator payslipPdfGenerator;

    // ==================== Preview & Finalize ====================

    @Override
    @Transactional(readOnly = true)
    public PayrollPreviewResponse previewPayroll(YearMonth period) {
        log.info("Preview lương kỳ {}", period);

        // Lấy danh sách nhân viên
        List<UserEntity> employees = userRepository.findByDeletedFalse();

        // Lấy cấu hình
        PayrollConfig payrollConfig = companySettingsService.getPayrollConfig();
        OvertimeConfig overtimeConfig = companySettingsService.getOvertimeConfig();
        AllowanceConfig allowanceConfig = companySettingsService.getAllowanceConfig();
        DeductionConfig deductionConfig = companySettingsService.getDeductionConfig();
        BreakConfig breakConfig = companySettingsService.getBreakConfig();

        List<PayrollRecordResponse> records = new ArrayList<>();
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalGrossSalary = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;

        for (UserEntity employee : employees) {
            PayrollRecordResponse record = calculateEmployeePayroll(
                    employee, period,
                    payrollConfig, overtimeConfig, allowanceConfig, deductionConfig, breakConfig);

            if (record != null) {
                records.add(record);
                totalBaseSalary = totalBaseSalary.add(nullToZero(record.getBaseSalary()));
                totalOvertimePay = totalOvertimePay.add(nullToZero(record.getTotalOvertimePay()));
                totalAllowances = totalAllowances.add(nullToZero(record.getTotalAllowances()));
                totalDeductions = totalDeductions.add(nullToZero(record.getTotalDeductions()));
                totalGrossSalary = totalGrossSalary.add(nullToZero(record.getGrossSalary()));
                totalNetSalary = totalNetSalary.add(nullToZero(record.getNetSalary()));
            }
        }

        return PayrollPreviewResponse.builder()
                .year(period.getYear())
                .month(period.getMonthValue())
                .period(formatPeriod(period))
                .totalEmployees(records.size())
                .totalBaseSalary(totalBaseSalary)
                .totalOvertimePay(totalOvertimePay)
                .totalAllowances(totalAllowances)
                .totalDeductions(totalDeductions)
                .totalGrossSalary(totalGrossSalary)
                .totalNetSalary(totalNetSalary)
                .records(records)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRecordResponse previewEmployeePayroll(Long employeeId, YearMonth period) {
        log.info("Preview lương nhân viên {} kỳ {}", employeeId, period);

        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        PayrollConfig payrollConfig = companySettingsService.getPayrollConfig();
        OvertimeConfig overtimeConfig = companySettingsService.getOvertimeConfig();
        AllowanceConfig allowanceConfig = companySettingsService.getAllowanceConfig();
        DeductionConfig deductionConfig = companySettingsService.getDeductionConfig();
        BreakConfig breakConfig = companySettingsService.getBreakConfig();

        return calculateEmployeePayroll(
                employee, period,
                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig, breakConfig);
    }

    @Override
    @Transactional
    public PayrollPeriodSummaryResponse finalizePayroll(YearMonth period, Long finalizedBy) {
        log.info("Finalize lương kỳ {} bởi {}", period, finalizedBy);

        // Kiểm tra đã finalize chưa
        List<PayrollRecordEntity> existingRecords = payrollRecordRepository
                .findAllByYearAndMonth(period.getYear(), period.getMonthValue());

        boolean alreadyFinalized = existingRecords.stream()
                .anyMatch(r -> r.getStatus() == PayrollStatus.FINALIZED || r.getStatus() == PayrollStatus.PAID);

        if (alreadyFinalized) {
            throw new BadRequestException("Lương kỳ này đã được finalize", ErrorCode.PAYROLL_ALREADY_FINALIZED);
        }

        // Lấy danh sách nhân viên
        List<UserEntity> employees = userRepository.findByDeletedFalse();

        // Lấy cấu hình
        PayrollConfig payrollConfig = companySettingsService.getPayrollConfig();
        OvertimeConfig overtimeConfig = companySettingsService.getOvertimeConfig();
        AllowanceConfig allowanceConfig = companySettingsService.getAllowanceConfig();
        DeductionConfig deductionConfig = companySettingsService.getDeductionConfig();
        BreakConfig breakConfig = companySettingsService.getBreakConfig();

        LocalDateTime now = LocalDateTime.now();
        List<PayrollRecordEntity> savedRecords = new ArrayList<>();

        for (UserEntity employee : employees) {
            // Tính toán lương
            PayrollRecordEntity record = calculateAndCreatePayrollRecord(
                    employee, period,
                    payrollConfig, overtimeConfig, allowanceConfig, deductionConfig, breakConfig);

            if (record != null) {
                // Đánh dấu finalized
                record.setStatus(PayrollStatus.FINALIZED);
                record.setPaymentStatus(PaymentStatus.PENDING);
                record.setFinalizedAt(now);
                record.setFinalizedBy(finalizedBy);

                savedRecords.add(payrollRecordRepository.save(record));
            }
        }

        log.info("Đã finalize {} bản ghi lương kỳ {}", savedRecords.size(), period);

        return buildPeriodSummary(period, savedRecords);
    }

    // ==================== Payment Processing ====================

    @Override
    @Transactional
    public void markAsPaid(YearMonth period) {
        log.info("Đánh dấu đã thanh toán lương kỳ {}", period);

        List<PayrollRecordEntity> records = payrollRecordRepository
                .findByYearAndMonthAndStatus(
                        period.getYear(), period.getMonthValue(), PayrollStatus.FINALIZED);

        if (records.isEmpty()) {
            throw new BadRequestException("Không có bản ghi lương để thanh toán", ErrorCode.PAYROLL_NOT_FINALIZED);
        }

        LocalDateTime now = LocalDateTime.now();
        for (PayrollRecordEntity record : records) {
            record.setStatus(PayrollStatus.PAID);
            record.setPaymentStatus(PaymentStatus.PAID);
            record.setPaidAt(now);
            payrollRecordRepository.save(record);
        }

        log.info("Đã đánh dấu thanh toán {} bản ghi lương", records.size());
    }

    @Override
    @Transactional
    public void markEmployeeAsPaid(Long payrollRecordId, String paymentReference) {
        log.info("Đánh dấu đã thanh toán bản ghi lương {}", payrollRecordId);

        PayrollRecordEntity record = findRecordById(payrollRecordId);

        if (record.getStatus() != PayrollStatus.FINALIZED) {
            throw new BadRequestException("Bản ghi lương chưa được finalize", ErrorCode.PAYROLL_NOT_FINALIZED);
        }

        record.setStatus(PayrollStatus.PAID);
        record.setPaymentStatus(PaymentStatus.PAID);
        record.setPaidAt(LocalDateTime.now());
        record.setPaymentReference(paymentReference);

        payrollRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void retryPayment(Long payrollRecordId) {
        log.info("Retry thanh toán bản ghi lương {}", payrollRecordId);

        PayrollRecordEntity record = findRecordById(payrollRecordId);

        if (record.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new BadRequestException("Chỉ có thể retry thanh toán thất bại", ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // Reset payment status để retry
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setPaidAt(null);
        record.setPaymentReference(null);

        payrollRecordRepository.save(record);
    }

    // ==================== Salary Notification ====================

    @Override
    @Transactional
    public void sendSalaryNotifications(YearMonth period) {
        log.info("Gửi thông báo lương kỳ {}", period);

        List<PayrollRecordEntity> records = payrollRecordRepository
                .findPendingNotifications(period.getYear(), period.getMonthValue());

        for (PayrollRecordEntity record : records) {
            sendSalaryNotification(record.getId());
        }

        log.info("Đã gửi {} thông báo lương", records.size());
    }

    @Override
    @Transactional
    public void sendSalaryNotification(Long payrollRecordId) {
        PayrollRecordEntity record = findRecordById(payrollRecordId);

        if (record.getStatus() != PayrollStatus.FINALIZED && record.getStatus() != PayrollStatus.PAID) {
            throw new BadRequestException("Chỉ gửi thông báo cho lương đã finalize", ErrorCode.PAYROLL_NOT_FINALIZED);
        }

        // Gửi email thông báo lương
        notificationEmailService.sendSalaryNotification(record.getEmployeeId(), record);

        record.setNotificationSent(true);
        record.setNotificationSentAt(LocalDateTime.now());
        payrollRecordRepository.save(record);

        log.info("Đã gửi thông báo lương cho nhân viên {}", record.getEmployeeId());
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public PayrollRecordResponse getPayrollRecordById(Long recordId) {
        PayrollRecordEntity entity = findRecordById(recordId);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodSummaryResponse getPayrollPeriodSummary(YearMonth period) {
        List<PayrollRecordEntity> records = payrollRecordRepository
                .findAllByYearAndMonth(period.getYear(), period.getMonthValue());

        return buildPeriodSummary(period, records);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollRecordResponse> getPayrollRecords(YearMonth period, Pageable pageable) {
        return payrollRecordRepository
                .findByYearAndMonth(period.getYear(), period.getMonthValue(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRecordResponse getEmployeePayroll(Long employeeId, YearMonth period) {
        return payrollRecordRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, period.getYear(), period.getMonthValue())
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollRecordResponse> getEmployeePayrollHistory(Long employeeId, Pageable pageable) {
        return payrollRecordRepository
                .findByEmployeeIdOrderByYearDescMonthDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    // ==================== Export ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPayrollCsv(YearMonth period) {
        log.info("Export CSV lương kỳ {}", period);

        List<PayrollRecordEntity> records = payrollRecordRepository
                .findAllByYearAndMonth(period.getYear(), period.getMonthValue());

        StringBuilder csv = new StringBuilder();
        // Header
        csv.append(
                "Employee Code,Employee Name,Base Salary,Overtime Pay,Allowances,Deductions,Gross Salary,Net Salary,Status\n");

        for (PayrollRecordEntity record : records) {
            String employeeName = getEmployeeName(record.getEmployeeId());
            String employeeCode = getEmployeeCode(record.getEmployeeId());

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    employeeCode,
                    employeeName,
                    record.getBaseSalary(),
                    record.getTotalOvertimePay(),
                    record.getTotalAllowances(),
                    record.getTotalDeductions(),
                    record.getGrossSalary(),
                    record.getNetSalary(),
                    record.getStatus()));
        }

        return csv.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPayrollPdf(YearMonth period) {
        log.info("Export PDF lương kỳ {}", period);

        // PDF export cần thư viện như iText hoặc Apache PDFBox
        // Hiện tại trả về thông báo chưa hỗ trợ
        throw new BadRequestException("PDF export chưa được hỗ trợ. Vui lòng sử dụng CSV export.",
                ErrorCode.FEATURE_NOT_SUPPORTED);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePayslip(Long payrollRecordId) {
        log.info("Generate payslip cho bản ghi {}", payrollRecordId);

        // Lấy bản ghi lương
        PayrollRecordEntity record = payrollRecordRepository.findById(payrollRecordId)
                .orElseThrow(() -> NotFoundException.payrollRecord(payrollRecordId));

        // Lấy thông tin nhân viên
        UserEntity employee = userRepository.findByIdAndDeletedFalse(record.getEmployeeId())
                .orElseThrow(() -> NotFoundException.user(record.getEmployeeId()));

        // Lấy thông tin công ty từ tenant context
        CompanyEntity company = companyRepository.findAll()
                .stream()
                .filter(c -> !c.getDeleted())
                .findFirst()
                .orElse(null);

        // Convert to response
        String employeeName = employee.getProfile() != null ? employee.getProfile().getName() : "";
        PayrollRecordResponse response = payrollMapper.toResponse(record, employeeName, employee.getEmployeeCode());

        // Generate PDF
        return payslipPdfGenerator.generate(response, employee, company);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tính toán lương cho một nhân viên và trả về response (không lưu DB)
     * Tích hợp break deduction và overtime calculation
     */
    private PayrollRecordResponse calculateEmployeePayroll(
            UserEntity employee, YearMonth period,
            PayrollConfig payrollConfig, OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig, DeductionConfig deductionConfig,
            BreakConfig breakConfig) {

        Long employeeId = employee.getId();

        // Lấy thông tin lương của nhân viên
        EmployeeSalaryInfo salaryInfo = getEmployeeSalaryInfo(employeeId, period);
        if (salaryInfo == null) {
            log.warn("Nhân viên {} không có thông tin lương", employeeId);
            return null;
        }

        // Lấy attendance records trong kỳ
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();
        List<AttendanceRecordEntity> attendanceRecords = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);

        // Tổng hợp attendance với break info
        AttendanceSummary attendanceSummary = buildAttendanceSummaryWithBreak(attendanceRecords, breakConfig);

        // Tạo daily overtime details với night minutes
        List<DailyOvertimeDetail> dailyDetails = buildDailyOvertimeDetailsWithNight(
                attendanceRecords, overtimeConfig, employeeId);

        // Tính toán lương
        PayrollResult result = payrollCalculator.calculatePayroll(
                salaryInfo, attendanceSummary, dailyDetails,
                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig);

        // Tính break deduction nếu unpaid
        BigDecimal breakDeductionAmount = calculateBreakDeduction(
                attendanceSummary.getTotalBreakMinutes(), salaryInfo, payrollConfig, breakConfig);

        // Build response
        String employeeName = getEmployeeName(employeeId);
        String employeeCode = getEmployeeCode(employeeId);

        return buildPayrollResponseWithBreak(employeeId, period, result, attendanceSummary,
                employeeName, employeeCode, breakConfig, breakDeductionAmount);
    }

    /**
     * Tính toán và tạo PayrollRecordEntity (để lưu DB)
     * Tích hợp break deduction và overtime calculation
     */
    private PayrollRecordEntity calculateAndCreatePayrollRecord(
            UserEntity employee, YearMonth period,
            PayrollConfig payrollConfig, OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig, DeductionConfig deductionConfig,
            BreakConfig breakConfig) {

        Long employeeId = employee.getId();

        // Lấy thông tin lương
        EmployeeSalaryInfo salaryInfo = getEmployeeSalaryInfo(employeeId, period);
        if (salaryInfo == null) {
            return null;
        }

        // Lấy attendance records
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();
        List<AttendanceRecordEntity> attendanceRecords = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);

        // Tổng hợp attendance với break info
        AttendanceSummary attendanceSummary = buildAttendanceSummaryWithBreak(attendanceRecords, breakConfig);

        // Tạo daily overtime details với night minutes
        List<DailyOvertimeDetail> dailyDetails = buildDailyOvertimeDetailsWithNight(
                attendanceRecords, overtimeConfig, employeeId);

        // Tính toán lương
        PayrollResult result = payrollCalculator.calculatePayroll(
                salaryInfo, attendanceSummary, dailyDetails,
                payrollConfig, overtimeConfig, allowanceConfig, deductionConfig);

        // Tính break deduction nếu unpaid
        BigDecimal breakDeductionAmount = calculateBreakDeduction(
                attendanceSummary.getTotalBreakMinutes(), salaryInfo, payrollConfig, breakConfig);

        // Tạo entity với break info
        return buildPayrollEntityWithBreak(employeeId, period, result, attendanceSummary,
                breakConfig, breakDeductionAmount);
    }

    /**
     * Lấy thông tin lương của nhân viên
     */
    private EmployeeSalaryInfo getEmployeeSalaryInfo(Long employeeId, YearMonth period) {
        LocalDate effectiveDate = period.atEndOfMonth();

        return employeeSalaryRepository.findEffectiveSalary(employeeId, effectiveDate)
                .map(entity -> EmployeeSalaryInfo.builder()
                        .salaryType(entity.getSalaryType())
                        .monthlySalary(entity.getMonthlySalary())
                        .dailyRate(entity.getDailyRate())
                        .hourlyRate(entity.getHourlyRate())
                        .build())
                .orElse(null);
    }

    /**
     * Tổng hợp attendance từ danh sách records
     */
    private AttendanceSummary buildAttendanceSummary(List<AttendanceRecordEntity> records) {
        return buildAttendanceSummaryWithBreak(records, null);
    }

    /**
     * Tổng hợp attendance từ danh sách records với break info.
     * Sử dụng aggregated break time từ tất cả break sessions (multiple breaks
     * support).
     */
    private AttendanceSummary buildAttendanceSummaryWithBreak(
            List<AttendanceRecordEntity> records, BreakConfig breakConfig) {
        int workingDays = 0;
        int workingMinutes = 0;
        int absenceDays = 0;
        int lateCount = 0;
        int totalLateMinutes = 0;
        int earlyLeaveCount = 0;
        int totalEarlyLeaveMinutes = 0;
        int totalOvertimeMinutes = 0;
        int totalBreakMinutes = 0;

        for (AttendanceRecordEntity record : records) {
            if (record.getStatus() == AttendanceStatus.PRESENT) {
                workingDays++;
                workingMinutes += nullToZeroInt(record.getWorkingMinutes());
                totalOvertimeMinutes += nullToZeroInt(record.getOvertimeMinutes());

                // Tính tổng break minutes từ tất cả break sessions (multiple breaks support)
                List<BreakRecordEntity> breakRecords = breakRecordRepository
                        .findByAttendanceRecordId(record.getId());

                if (!breakRecords.isEmpty()) {
                    // Sử dụng breakCalculator để tính tổng từ tất cả break sessions
                    totalBreakMinutes += breakCalculator.calculateTotalBreakMinutes(breakRecords);
                } else if (breakConfig != null && breakConfig.getDefaultBreakMinutes() != null) {
                    // Sử dụng default break nếu không có break records
                    totalBreakMinutes += breakConfig.getDefaultBreakMinutes();
                }

                if (record.getLateMinutes() != null && record.getLateMinutes() > 0) {
                    lateCount++;
                    totalLateMinutes += record.getLateMinutes();
                }

                if (record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0) {
                    earlyLeaveCount++;
                    totalEarlyLeaveMinutes += record.getEarlyLeaveMinutes();
                }
            } else if (record.getStatus() == AttendanceStatus.ABSENT) {
                absenceDays++;
            }
        }

        return AttendanceSummary.builder()
                .workingDays(workingDays)
                .workingHours(workingMinutes / 60)
                .absenceDays(absenceDays)
                .lateCount(lateCount)
                .totalLateMinutes(totalLateMinutes)
                .earlyLeaveCount(earlyLeaveCount)
                .totalEarlyLeaveMinutes(totalEarlyLeaveMinutes)
                .totalOvertimeMinutes(totalOvertimeMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .build();
    }

    /**
     * Tạo daily overtime details từ attendance records
     */
    private List<DailyOvertimeDetail> buildDailyOvertimeDetails(List<AttendanceRecordEntity> records) {
        return buildDailyOvertimeDetailsWithNight(records, null, null);
    }

    /**
     * Tạo daily overtime details từ attendance records với night minutes
     */
    private List<DailyOvertimeDetail> buildDailyOvertimeDetailsWithNight(
            List<AttendanceRecordEntity> records,
            OvertimeConfig overtimeConfig,
            Long employeeId) {
        List<DailyOvertimeDetail> details = new ArrayList<>();

        for (AttendanceRecordEntity record : records) {
            if (record.getOvertimeMinutes() != null && record.getOvertimeMinutes() > 0) {
                LocalDate date = record.getWorkDate();
                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;

                int nightMinutes = 0;
                int regularMinutes = record.getOvertimeMinutes();

                // Tính night minutes nếu có overtime config và check-in/check-out
                if (overtimeConfig != null && record.getRoundedCheckIn() != null
                        && record.getRoundedCheckOut() != null && employeeId != null) {
                    // Lấy break records cho ngày này
                    List<BreakRecordEntity> breakRecords = breakRecordRepository
                            .findByEmployeeIdAndWorkDate(employeeId, date);

                    nightMinutes = overtimeCalculator.calculateNightMinutes(
                            record.getRoundedCheckIn(),
                            record.getRoundedCheckOut(),
                            breakRecords,
                            overtimeConfig);

                    // Night minutes không vượt quá overtime minutes
                    nightMinutes = Math.min(nightMinutes, record.getOvertimeMinutes());
                    regularMinutes = record.getOvertimeMinutes() - nightMinutes;
                }

                details.add(DailyOvertimeDetail.builder()
                        .date(date)
                        .regularMinutes(regularMinutes)
                        .nightMinutes(nightMinutes)
                        .isHoliday(isHoliday(date))
                        .isWeekend(isWeekend)
                        .build());
            }
        }

        return details;
    }

    /**
     * Tính break deduction amount
     */
    private BigDecimal calculateBreakDeduction(
            Integer totalBreakMinutes,
            EmployeeSalaryInfo salaryInfo,
            PayrollConfig payrollConfig,
            BreakConfig breakConfig) {

        // Chỉ tính deduction nếu break là UNPAID
        if (breakConfig == null || breakConfig.getBreakType() != BreakType.UNPAID) {
            return BigDecimal.ZERO;
        }

        if (totalBreakMinutes == null || totalBreakMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        // Tính hourly rate
        BigDecimal hourlyRate = calculateHourlyRate(salaryInfo, payrollConfig);
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Break deduction = (break minutes / 60) * hourly rate
        BigDecimal breakHours = BigDecimal.valueOf(totalBreakMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        return breakHours.multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính hourly rate từ salary info
     */
    private BigDecimal calculateHourlyRate(EmployeeSalaryInfo salaryInfo, PayrollConfig config) {
        if (salaryInfo == null) {
            return BigDecimal.ZERO;
        }

        if (salaryInfo.getHourlyRate() != null) {
            return salaryInfo.getHourlyRate();
        }

        if (salaryInfo.getDailyRate() != null && config != null) {
            int hoursPerDay = config.getStandardWorkingHoursPerDay() != null
                    ? config.getStandardWorkingHoursPerDay()
                    : 8;
            return salaryInfo.getDailyRate()
                    .divide(BigDecimal.valueOf(hoursPerDay), 4, RoundingMode.HALF_UP);
        }

        if (salaryInfo.getMonthlySalary() != null && config != null) {
            int daysPerMonth = config.getStandardWorkingDaysPerMonth() != null
                    ? config.getStandardWorkingDaysPerMonth()
                    : 22;
            int hoursPerDay = config.getStandardWorkingHoursPerDay() != null
                    ? config.getStandardWorkingHoursPerDay()
                    : 8;
            int hoursPerMonth = daysPerMonth * hoursPerDay;

            return salaryInfo.getMonthlySalary()
                    .divide(BigDecimal.valueOf(hoursPerMonth), 4, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Build PayrollRecordResponse với break info
     */
    private PayrollRecordResponse buildPayrollResponseWithBreak(
            Long employeeId, YearMonth period,
            PayrollResult result, AttendanceSummary attendance,
            String employeeName, String employeeCode,
            BreakConfig breakConfig, BigDecimal breakDeductionAmount) {

        PayrollRecordResponse response = buildPayrollResponse(
                employeeId, period, result, attendance, employeeName, employeeCode);

        // Thêm break info
        if (response != null && breakConfig != null) {
            response.setTotalBreakMinutes(attendance.getTotalBreakMinutes());
            response.setBreakType(breakConfig.getBreakType());
            response.setBreakDeductionAmount(breakDeductionAmount);
        }

        return response;
    }

    /**
     * Build PayrollRecordEntity với break info
     */
    private PayrollRecordEntity buildPayrollEntityWithBreak(
            Long employeeId, YearMonth period,
            PayrollResult result, AttendanceSummary attendance,
            BreakConfig breakConfig, BigDecimal breakDeductionAmount) {

        PayrollRecordEntity entity = buildPayrollEntity(
                employeeId, period, result, attendance);

        // Thêm break info
        if (entity != null && breakConfig != null) {
            entity.setTotalBreakMinutes(attendance.getTotalBreakMinutes());
            entity.setBreakType(breakConfig.getBreakType());
            entity.setBreakDeductionAmount(breakDeductionAmount);
        }

        return entity;
    }

    /**
     * Build PayrollRecordResponse từ kết quả tính toán
     */
    private PayrollRecordResponse buildPayrollResponse(
            Long employeeId, YearMonth period,
            PayrollResult result, AttendanceSummary attendance,
            String employeeName, String employeeCode) {

        OvertimeResult overtime = result.getOvertimeResult();

        return PayrollRecordResponse.builder()
                .employeeId(employeeId)
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .year(period.getYear())
                .month(period.getMonthValue())
                .period(formatPeriod(period))
                .salaryType(result.getSalaryType())
                .baseSalary(result.getBaseSalary())
                .workingDays(attendance.getWorkingDays())
                .workingHours(attendance.getWorkingHours())
                .regularOvertimePay(overtime != null ? overtime.getRegularOvertimePay() : BigDecimal.ZERO)
                .nightOvertimePay(overtime != null ? overtime.getNightOvertimePay() : BigDecimal.ZERO)
                .holidayOvertimePay(overtime != null ? overtime.getHolidayOvertimePay() : BigDecimal.ZERO)
                .weekendOvertimePay(overtime != null ? overtime.getWeekendOvertimePay() : BigDecimal.ZERO)
                .totalOvertimePay(result.getTotalOvertimePay())
                .regularOvertimeHours(overtime != null ? overtime.getRegularOvertimeMinutes() / 60 : 0)
                .nightOvertimeHours(overtime != null ? overtime.getNightOvertimeMinutes() / 60 : 0)
                .holidayOvertimeHours(overtime != null ? overtime.getHolidayOvertimeMinutes() / 60 : 0)
                .weekendOvertimeHours(overtime != null ? overtime.getWeekendOvertimeMinutes() / 60 : 0)
                .allowanceDetails(buildAllowanceItemResponses(result.getAllowanceResult()))
                .totalAllowances(result.getTotalAllowances())
                .deductionDetails(buildDeductionItemResponses(result.getDeductionResult()))
                .totalDeductions(result.getTotalDeductions())
                .grossSalary(result.getGrossSalary())
                .netSalary(result.getNetSalary())
                .status(PayrollStatus.DRAFT)
                .build();
    }

    /**
     * Build PayrollRecordEntity từ kết quả tính toán
     */
    private PayrollRecordEntity buildPayrollEntity(
            Long employeeId, YearMonth period,
            PayrollResult result, AttendanceSummary attendance) {

        PayrollRecordEntity entity = new PayrollRecordEntity();
        entity.setEmployeeId(employeeId);
        entity.setYear(period.getYear());
        entity.setMonth(period.getMonthValue());
        entity.setSalaryType(result.getSalaryType() != null ? result.getSalaryType() : SalaryType.MONTHLY);
        entity.setBaseSalary(result.getBaseSalary());
        entity.setWorkingDays(attendance.getWorkingDays());
        entity.setWorkingHours(attendance.getWorkingHours());

        OvertimeResult overtime = result.getOvertimeResult();
        if (overtime != null) {
            entity.setRegularOvertimePay(overtime.getRegularOvertimePay());
            entity.setNightOvertimePay(overtime.getNightOvertimePay());
            entity.setHolidayOvertimePay(overtime.getHolidayOvertimePay());
            entity.setWeekendOvertimePay(overtime.getWeekendOvertimePay());
            entity.setRegularOvertimeHours(overtime.getRegularOvertimeMinutes() / 60);
            entity.setNightOvertimeHours(overtime.getNightOvertimeMinutes() / 60);
            entity.setHolidayOvertimeHours(overtime.getHolidayOvertimeMinutes() / 60);
            entity.setWeekendOvertimeHours(overtime.getWeekendOvertimeMinutes() / 60);
        }
        entity.setTotalOvertimePay(result.getTotalOvertimePay());

        // Serialize allowance details
        if (result.getAllowanceResult() != null && result.getAllowanceResult().getItems() != null) {
            try {
                entity.setAllowanceDetails(objectMapper.writeValueAsString(result.getAllowanceResult().getItems()));
            } catch (Exception e) {
                log.warn("Không thể serialize allowance details: {}", e.getMessage());
            }
        }
        entity.setTotalAllowances(result.getTotalAllowances());

        // Serialize deduction details
        if (result.getDeductionResult() != null && result.getDeductionResult().getItems() != null) {
            try {
                entity.setDeductionDetails(objectMapper.writeValueAsString(result.getDeductionResult().getItems()));
            } catch (Exception e) {
                log.warn("Không thể serialize deduction details: {}", e.getMessage());
            }
        }
        entity.setTotalDeductions(result.getTotalDeductions());

        entity.setGrossSalary(result.getGrossSalary());
        entity.setNetSalary(result.getNetSalary());
        entity.setStatus(PayrollStatus.DRAFT);

        return entity;
    }

    /**
     * Build period summary từ danh sách records
     */
    private PayrollPeriodSummaryResponse buildPeriodSummary(
            YearMonth period, List<PayrollRecordEntity> records) {

        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalGrossSalary = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        int paidEmployees = 0;
        int pendingEmployees = 0;
        int failedEmployees = 0;
        int notificationsSent = 0;
        int notificationsPending = 0;

        PayrollStatus overallStatus = PayrollStatus.DRAFT;
        LocalDateTime finalizedAt = null;
        Long finalizedBy = null;

        for (PayrollRecordEntity record : records) {
            totalBaseSalary = totalBaseSalary.add(nullToZero(record.getBaseSalary()));
            totalOvertimePay = totalOvertimePay.add(nullToZero(record.getTotalOvertimePay()));
            totalAllowances = totalAllowances.add(nullToZero(record.getTotalAllowances()));
            totalDeductions = totalDeductions.add(nullToZero(record.getTotalDeductions()));
            totalGrossSalary = totalGrossSalary.add(nullToZero(record.getGrossSalary()));
            totalNetSalary = totalNetSalary.add(nullToZero(record.getNetSalary()));

            if (record.getPaymentStatus() == PaymentStatus.PAID) {
                paidEmployees++;
                totalPaid = totalPaid.add(nullToZero(record.getNetSalary()));
            } else if (record.getPaymentStatus() == PaymentStatus.FAILED) {
                failedEmployees++;
            } else {
                pendingEmployees++;
                totalPending = totalPending.add(nullToZero(record.getNetSalary()));
            }

            if (Boolean.TRUE.equals(record.getNotificationSent())) {
                notificationsSent++;
            } else {
                notificationsPending++;
            }

            // Lấy thông tin finalization từ record đầu tiên có
            if (record.getStatus() == PayrollStatus.FINALIZED || record.getStatus() == PayrollStatus.PAID) {
                overallStatus = record.getStatus();
                if (finalizedAt == null) {
                    finalizedAt = record.getFinalizedAt();
                    finalizedBy = record.getFinalizedBy();
                }
            }
        }

        String finalizedByName = finalizedBy != null ? getEmployeeName(finalizedBy) : null;

        return PayrollPeriodSummaryResponse.builder()
                .year(period.getYear())
                .month(period.getMonthValue())
                .period(formatPeriod(period))
                .status(overallStatus)
                .finalizedAt(finalizedAt)
                .finalizedBy(finalizedBy)
                .finalizedByName(finalizedByName)
                .totalEmployees(records.size())
                .paidEmployees(paidEmployees)
                .pendingEmployees(pendingEmployees)
                .failedEmployees(failedEmployees)
                .totalBaseSalary(totalBaseSalary)
                .totalOvertimePay(totalOvertimePay)
                .totalAllowances(totalAllowances)
                .totalDeductions(totalDeductions)
                .totalGrossSalary(totalGrossSalary)
                .totalNetSalary(totalNetSalary)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .notificationsSent(notificationsSent)
                .notificationsPending(notificationsPending)
                .build();
    }

    /**
     * Build allowance item responses
     */
    private List<PayrollRecordResponse.AllowanceItemResponse> buildAllowanceItemResponses(AllowanceResult result) {
        if (result == null || result.getItems() == null) {
            return new ArrayList<>();
        }

        return result.getItems().stream()
                .map(item -> PayrollRecordResponse.AllowanceItemResponse.builder()
                        .code(item.getCode())
                        .name(item.getName())
                        .amount(item.getAmount())
                        .taxable(item.getTaxable())
                        .build())
                .toList();
    }

    /**
     * Build deduction item responses
     */
    private List<PayrollRecordResponse.DeductionItemResponse> buildDeductionItemResponses(DeductionResult result) {
        if (result == null || result.getItems() == null) {
            return new ArrayList<>();
        }

        return result.getItems().stream()
                .map(item -> PayrollRecordResponse.DeductionItemResponse.builder()
                        .code(item.getCode())
                        .name(item.getName())
                        .amount(item.getAmount())
                        .build())
                .toList();
    }

    /**
     * Tìm bản ghi lương theo ID
     */
    private PayrollRecordEntity findRecordById(Long recordId) {
        return payrollRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi lương", ErrorCode.PAYROLL_NOT_FOUND));
    }

    /**
     * Chuyển entity sang response
     */
    private PayrollRecordResponse toResponse(PayrollRecordEntity entity) {
        String employeeName = getEmployeeName(entity.getEmployeeId());
        String employeeCode = getEmployeeCode(entity.getEmployeeId());
        return payrollMapper.toResponse(entity, employeeName, employeeCode);
    }

    /**
     * Lấy tên nhân viên
     */
    private String getEmployeeName(Long employeeId) {
        return userRepository.findById(employeeId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    /**
     * Lấy mã nhân viên
     */
    private String getEmployeeCode(Long employeeId) {
        return userRepository.findById(employeeId)
                .map(UserEntity::getEmployeeCode)
                .orElse(null);
    }

    /**
     * Format period thành chuỗi YYYY-MM
     */
    private String formatPeriod(YearMonth period) {
        return String.format("%d-%02d", period.getYear(), period.getMonthValue());
    }

    /**
     * Null-safe BigDecimal
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Null-safe Integer
     */
    private int nullToZeroInt(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Kiểm tra ngày có phải ngày nghỉ lễ không
     */
    private boolean isHoliday(LocalDate date) {
        return holidayRepository.isHoliday(date);
    }
}
