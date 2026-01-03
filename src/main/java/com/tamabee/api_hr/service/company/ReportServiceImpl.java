package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.ReportQuery;
import com.tamabee.api_hr.dto.response.report.*;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftSwapRequestEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.*;
import com.tamabee.api_hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service triển khai các báo cáo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

        private final AttendanceRecordRepository attendanceRecordRepository;
        private final BreakRecordRepository breakRecordRepository;
        private final UserRepository userRepository;
        private final ShiftAssignmentRepository shiftAssignmentRepository;
        private final ShiftSwapRequestRepository shiftSwapRequestRepository;
        private final ShiftTemplateRepository shiftTemplateRepository;
        private final PayrollPeriodRepository payrollPeriodRepository;
        private final PayrollItemRepository payrollItemRepository;
        private final EmploymentContractRepository employmentContractRepository;
        private final ReportExportService reportExportService;

        @Override
        @Transactional(readOnly = true)
        public AttendanceSummaryReport generateAttendanceSummary(Long companyId, ReportQuery query) {
                log.info("Generating attendance summary report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy danh sách nhân viên
                List<UserEntity> employees = getFilteredEmployees(companyId, query);
                if (employees.isEmpty()) {
                        return buildEmptyAttendanceSummaryReport(companyId, startDate, endDate);
                }

                // Tính toán cho từng nhân viên
                List<EmployeeAttendanceSummary> employeeSummaries = new ArrayList<>();
                int totalPresentDays = 0;
                int totalAbsentDays = 0;
                int totalLateCount = 0;
                int totalEarlyLeaveCount = 0;

                for (UserEntity employee : employees) {
                        EmployeeAttendanceSummary summary = calculateEmployeeAttendanceSummary(
                                        employee, startDate, endDate);
                        employeeSummaries.add(summary);

                        totalPresentDays += summary.getPresentDays();
                        totalAbsentDays += summary.getAbsentDays();
                        totalLateCount += summary.getLateCount();
                        totalEarlyLeaveCount += summary.getEarlyLeaveCount();
                }

                // Tính số ngày làm việc trong khoảng thời gian
                int totalWorkingDays = calculateWorkingDays(startDate, endDate);
                int totalExpectedDays = totalWorkingDays * employees.size();

                // Tính tỷ lệ
                double attendanceRate = totalExpectedDays > 0
                                ? (double) totalPresentDays / totalExpectedDays * 100
                                : 0.0;
                double punctualityRate = totalPresentDays > 0
                                ? (double) (totalPresentDays - totalLateCount) / totalPresentDays * 100
                                : 0.0;

                return AttendanceSummaryReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(employees.size())
                                .totalWorkingDays(totalWorkingDays)
                                .totalPresentDays(totalPresentDays)
                                .totalAbsentDays(totalAbsentDays)
                                .totalLateCount(totalLateCount)
                                .totalEarlyLeaveCount(totalEarlyLeaveCount)
                                .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                                .punctualityRate(Math.round(punctualityRate * 100.0) / 100.0)
                                .employeeSummaries(employeeSummaries)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public OvertimeReport generateOvertimeReport(Long companyId, ReportQuery query) {
                log.info("Generating overtime report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy danh sách nhân viên
                List<UserEntity> employees = getFilteredEmployees(companyId, query);
                if (employees.isEmpty()) {
                        return buildEmptyOvertimeReport(companyId, startDate, endDate);
                }

                // Tính toán cho từng nhân viên
                List<EmployeeOvertimeSummary> employeeSummaries = new ArrayList<>();
                int totalRegularOT = 0, totalNightOT = 0, totalHolidayOT = 0, totalWeekendOT = 0;
                BigDecimal totalRegularPay = BigDecimal.ZERO;
                BigDecimal totalNightPay = BigDecimal.ZERO;
                BigDecimal totalHolidayPay = BigDecimal.ZERO;
                BigDecimal totalWeekendPay = BigDecimal.ZERO;
                int employeesWithOvertime = 0;

                for (UserEntity employee : employees) {
                        EmployeeOvertimeSummary summary = calculateEmployeeOvertimeSummary(
                                        employee, startDate, endDate);

                        if (summary.getTotalOvertimeMinutes() > 0) {
                                employeeSummaries.add(summary);
                                employeesWithOvertime++;

                                totalRegularOT += summary.getRegularOvertimeMinutes();
                                totalNightOT += summary.getNightOvertimeMinutes();
                                totalHolidayOT += summary.getHolidayOvertimeMinutes();
                                totalWeekendOT += summary.getWeekendOvertimeMinutes();

                                totalRegularPay = totalRegularPay.add(
                                                summary.getRegularOvertimePay() != null
                                                                ? summary.getRegularOvertimePay()
                                                                : BigDecimal.ZERO);
                                totalNightPay = totalNightPay.add(
                                                summary.getNightOvertimePay() != null ? summary.getNightOvertimePay()
                                                                : BigDecimal.ZERO);
                                totalHolidayPay = totalHolidayPay.add(
                                                summary.getHolidayOvertimePay() != null
                                                                ? summary.getHolidayOvertimePay()
                                                                : BigDecimal.ZERO);
                                totalWeekendPay = totalWeekendPay.add(
                                                summary.getWeekendOvertimePay() != null
                                                                ? summary.getWeekendOvertimePay()
                                                                : BigDecimal.ZERO);
                        }
                }

                int totalOT = totalRegularOT + totalNightOT + totalHolidayOT + totalWeekendOT;
                BigDecimal totalPay = totalRegularPay.add(totalNightPay).add(totalHolidayPay).add(totalWeekendPay);

                return OvertimeReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployeesWithOvertime(employeesWithOvertime)
                                .totalRegularOvertimeMinutes(totalRegularOT)
                                .totalNightOvertimeMinutes(totalNightOT)
                                .totalHolidayOvertimeMinutes(totalHolidayOT)
                                .totalWeekendOvertimeMinutes(totalWeekendOT)
                                .totalOvertimeMinutes(totalOT)
                                .totalRegularOvertimePay(totalRegularPay)
                                .totalNightOvertimePay(totalNightPay)
                                .totalHolidayOvertimePay(totalHolidayPay)
                                .totalWeekendOvertimePay(totalWeekendPay)
                                .totalOvertimePay(totalPay)
                                .employeeSummaries(employeeSummaries)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public BreakComplianceReport generateBreakComplianceReport(Long companyId, ReportQuery query) {
                log.info("Generating break compliance report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy danh sách nhân viên
                List<UserEntity> employees = getFilteredEmployees(companyId, query);
                if (employees.isEmpty()) {
                        return buildEmptyBreakComplianceReport(companyId, startDate, endDate);
                }

                // Tính toán cho từng nhân viên
                List<EmployeeBreakSummary> employeeSummaries = new ArrayList<>();
                int totalBreakCount = 0;
                int totalBreakMinutes = 0;
                int totalCompliant = 0;
                int totalNonCompliant = 0;

                for (UserEntity employee : employees) {
                        EmployeeBreakSummary summary = calculateEmployeeBreakSummary(
                                        employee, startDate, endDate);
                        employeeSummaries.add(summary);

                        totalBreakCount += summary.getTotalBreakCount();
                        totalBreakMinutes += summary.getTotalBreakMinutes();
                        totalCompliant += summary.getCompliantBreakCount();
                        totalNonCompliant += summary.getNonCompliantBreakCount();
                }

                int avgBreakMinutes = employees.size() > 0 ? totalBreakMinutes / employees.size() : 0;
                double complianceRate = (totalCompliant + totalNonCompliant) > 0
                                ? (double) totalCompliant / (totalCompliant + totalNonCompliant) * 100
                                : 100.0;

                return BreakComplianceReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(employees.size())
                                .totalBreakCount(totalBreakCount)
                                .totalBreakMinutes(totalBreakMinutes)
                                .averageBreakMinutesPerEmployee(avgBreakMinutes)
                                .compliantBreakCount(totalCompliant)
                                .nonCompliantBreakCount(totalNonCompliant)
                                .overallComplianceRate(Math.round(complianceRate * 100.0) / 100.0)
                                .employeeSummaries(employeeSummaries)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public PayrollSummaryReport generatePayrollSummary(Long companyId, ReportQuery query) {
                log.info("Generating payroll summary report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy các payroll periods trong khoảng thời gian
                List<PayrollPeriodEntity> periods = payrollPeriodRepository
                                .findByCompanyIdAndDateRange(companyId, startDate, endDate);

                if (periods.isEmpty()) {
                        return buildEmptyPayrollSummaryReport(companyId, startDate, endDate);
                }

                // Lấy tất cả payroll items từ các periods
                List<PayrollItemEntity> allItems = new ArrayList<>();
                for (PayrollPeriodEntity period : periods) {
                        List<PayrollItemEntity> items = payrollItemRepository
                                        .findByPayrollPeriodIdAndDeletedFalse(period.getId());
                        allItems.addAll(items);
                }

                // Filter theo employeeIds nếu có
                if (query.getEmployeeIds() != null && !query.getEmployeeIds().isEmpty()) {
                        Set<Long> employeeIdSet = new HashSet<>(query.getEmployeeIds());
                        allItems = allItems.stream()
                                        .filter(item -> employeeIdSet.contains(item.getEmployeeId()))
                                        .collect(Collectors.toList());
                }

                // Filter theo salaryTypes nếu có
                if (query.getSalaryTypes() != null && !query.getSalaryTypes().isEmpty()) {
                        Set<SalaryType> salaryTypeSet = new HashSet<>(query.getSalaryTypes());
                        allItems = allItems.stream()
                                        .filter(item -> salaryTypeSet.contains(item.getSalaryType()))
                                        .collect(Collectors.toList());
                }

                if (allItems.isEmpty()) {
                        return buildEmptyPayrollSummaryReport(companyId, startDate, endDate);
                }

                // Tính tổng
                BigDecimal totalBaseSalary = BigDecimal.ZERO;
                BigDecimal totalOvertimePay = BigDecimal.ZERO;
                BigDecimal totalAllowances = BigDecimal.ZERO;
                BigDecimal totalDeductions = BigDecimal.ZERO;
                BigDecimal totalGrossSalary = BigDecimal.ZERO;
                BigDecimal totalNetSalary = BigDecimal.ZERO;

                // Group by employee để tạo summary
                Map<Long, List<PayrollItemEntity>> itemsByEmployee = allItems.stream()
                                .collect(Collectors.groupingBy(PayrollItemEntity::getEmployeeId));

                List<EmployeePayrollSummary> employeeSummaries = new ArrayList<>();
                Map<Long, UserEntity> employeeCache = new HashMap<>();

                for (Map.Entry<Long, List<PayrollItemEntity>> entry : itemsByEmployee.entrySet()) {
                        Long employeeId = entry.getKey();
                        List<PayrollItemEntity> employeeItems = entry.getValue();

                        // Lấy thông tin nhân viên
                        UserEntity employee = employeeCache.computeIfAbsent(employeeId,
                                        id -> userRepository.findByIdAndDeletedFalse(id).orElse(null));

                        if (employee == null)
                                continue;

                        // Tính tổng cho nhân viên
                        BigDecimal empBaseSalary = BigDecimal.ZERO;
                        BigDecimal empCalculatedBase = BigDecimal.ZERO;
                        BigDecimal empOvertimePay = BigDecimal.ZERO;
                        BigDecimal empAllowances = BigDecimal.ZERO;
                        BigDecimal empDeductions = BigDecimal.ZERO;
                        BigDecimal empGross = BigDecimal.ZERO;
                        BigDecimal empNet = BigDecimal.ZERO;
                        SalaryType salaryType = null;

                        for (PayrollItemEntity item : employeeItems) {
                                if (salaryType == null)
                                        salaryType = item.getSalaryType();

                                empBaseSalary = empBaseSalary.add(
                                                item.getBaseSalary() != null ? item.getBaseSalary() : BigDecimal.ZERO);
                                empCalculatedBase = empCalculatedBase.add(
                                                item.getCalculatedBaseSalary() != null ? item.getCalculatedBaseSalary()
                                                                : BigDecimal.ZERO);
                                empOvertimePay = empOvertimePay.add(
                                                item.getTotalOvertimePay() != null ? item.getTotalOvertimePay()
                                                                : BigDecimal.ZERO);
                                empAllowances = empAllowances.add(
                                                item.getTotalAllowances() != null ? item.getTotalAllowances()
                                                                : BigDecimal.ZERO);
                                empDeductions = empDeductions.add(
                                                item.getTotalDeductions() != null ? item.getTotalDeductions()
                                                                : BigDecimal.ZERO);
                                empGross = empGross.add(
                                                item.getGrossSalary() != null ? item.getGrossSalary()
                                                                : BigDecimal.ZERO);
                                empNet = empNet.add(
                                                item.getNetSalary() != null ? item.getNetSalary() : BigDecimal.ZERO);
                        }

                        String employeeName = employee.getProfile() != null
                                        ? employee.getProfile().getName()
                                        : employee.getEmail();

                        employeeSummaries.add(EmployeePayrollSummary.builder()
                                        .employeeId(employeeId)
                                        .employeeCode(employee.getEmployeeCode())
                                        .employeeName(employeeName)
                                        .salaryType(salaryType)
                                        .baseSalary(empBaseSalary)
                                        .calculatedBaseSalary(empCalculatedBase)
                                        .overtimePay(empOvertimePay)
                                        .totalAllowances(empAllowances)
                                        .totalDeductions(empDeductions)
                                        .grossSalary(empGross)
                                        .netSalary(empNet)
                                        .build());

                        totalBaseSalary = totalBaseSalary.add(empCalculatedBase);
                        totalOvertimePay = totalOvertimePay.add(empOvertimePay);
                        totalAllowances = totalAllowances.add(empAllowances);
                        totalDeductions = totalDeductions.add(empDeductions);
                        totalGrossSalary = totalGrossSalary.add(empGross);
                        totalNetSalary = totalNetSalary.add(empNet);
                }

                int totalEmployees = employeeSummaries.size();
                BigDecimal avgGross = totalEmployees > 0
                                ? totalGrossSalary.divide(BigDecimal.valueOf(totalEmployees), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                BigDecimal avgNet = totalEmployees > 0
                                ? totalNetSalary.divide(BigDecimal.valueOf(totalEmployees), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                return PayrollSummaryReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(totalEmployees)
                                .totalBaseSalary(totalBaseSalary)
                                .totalOvertimePay(totalOvertimePay)
                                .totalAllowances(totalAllowances)
                                .totalDeductions(totalDeductions)
                                .totalGrossSalary(totalGrossSalary)
                                .totalNetSalary(totalNetSalary)
                                .averageGrossSalary(avgGross)
                                .averageNetSalary(avgNet)
                                .employeeSummaries(employeeSummaries)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public CostAnalysisReport generateCostAnalysis(Long companyId, ReportQuery query) {
                log.info("Generating cost analysis report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy các payroll periods trong khoảng thời gian
                List<PayrollPeriodEntity> periods = payrollPeriodRepository
                                .findByCompanyIdAndDateRange(companyId, startDate, endDate);

                if (periods.isEmpty()) {
                        return buildEmptyCostAnalysisReport(companyId, startDate, endDate);
                }

                // Lấy tất cả payroll items từ các periods
                List<PayrollItemEntity> allItems = new ArrayList<>();
                for (PayrollPeriodEntity period : periods) {
                        List<PayrollItemEntity> items = payrollItemRepository
                                        .findByPayrollPeriodIdAndDeletedFalse(period.getId());
                        allItems.addAll(items);
                }

                if (allItems.isEmpty()) {
                        return buildEmptyCostAnalysisReport(companyId, startDate, endDate);
                }

                // Tính tổng chi phí
                BigDecimal totalLaborCost = BigDecimal.ZERO;
                BigDecimal totalBaseSalaryCost = BigDecimal.ZERO;
                BigDecimal totalOvertimeCost = BigDecimal.ZERO;
                BigDecimal totalAllowanceCost = BigDecimal.ZERO;
                BigDecimal totalDeductionAmount = BigDecimal.ZERO;

                for (PayrollItemEntity item : allItems) {
                        BigDecimal gross = item.getGrossSalary() != null ? item.getGrossSalary() : BigDecimal.ZERO;
                        BigDecimal base = item.getCalculatedBaseSalary() != null ? item.getCalculatedBaseSalary()
                                        : BigDecimal.ZERO;
                        BigDecimal overtime = item.getTotalOvertimePay() != null ? item.getTotalOvertimePay()
                                        : BigDecimal.ZERO;
                        BigDecimal allowance = item.getTotalAllowances() != null ? item.getTotalAllowances()
                                        : BigDecimal.ZERO;
                        BigDecimal deduction = item.getTotalDeductions() != null ? item.getTotalDeductions()
                                        : BigDecimal.ZERO;

                        totalLaborCost = totalLaborCost.add(gross);
                        totalBaseSalaryCost = totalBaseSalaryCost.add(base);
                        totalOvertimeCost = totalOvertimeCost.add(overtime);
                        totalAllowanceCost = totalAllowanceCost.add(allowance);
                        totalDeductionAmount = totalDeductionAmount.add(deduction);
                }

                // Phân tích theo loại lương
                Map<SalaryType, List<PayrollItemEntity>> itemsBySalaryType = allItems.stream()
                                .collect(Collectors.groupingBy(PayrollItemEntity::getSalaryType));

                List<CostBySalaryType> costBySalaryType = new ArrayList<>();
                for (Map.Entry<SalaryType, List<PayrollItemEntity>> entry : itemsBySalaryType.entrySet()) {
                        SalaryType salaryType = entry.getKey();
                        List<PayrollItemEntity> items = entry.getValue();

                        BigDecimal typeTotalCost = BigDecimal.ZERO;
                        BigDecimal typeBaseCost = BigDecimal.ZERO;
                        BigDecimal typeOvertimeCost = BigDecimal.ZERO;
                        BigDecimal typeAllowanceCost = BigDecimal.ZERO;

                        for (PayrollItemEntity item : items) {
                                typeTotalCost = typeTotalCost.add(
                                                item.getGrossSalary() != null ? item.getGrossSalary()
                                                                : BigDecimal.ZERO);
                                typeBaseCost = typeBaseCost.add(
                                                item.getCalculatedBaseSalary() != null ? item.getCalculatedBaseSalary()
                                                                : BigDecimal.ZERO);
                                typeOvertimeCost = typeOvertimeCost.add(
                                                item.getTotalOvertimePay() != null ? item.getTotalOvertimePay()
                                                                : BigDecimal.ZERO);
                                typeAllowanceCost = typeAllowanceCost.add(
                                                item.getTotalAllowances() != null ? item.getTotalAllowances()
                                                                : BigDecimal.ZERO);
                        }

                        double percentage = totalLaborCost.compareTo(BigDecimal.ZERO) > 0
                                        ? typeTotalCost.divide(totalLaborCost, 4, RoundingMode.HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                                        : 0.0;

                        costBySalaryType.add(CostBySalaryType.builder()
                                        .salaryType(salaryType)
                                        .employeeCount(
                                                        items.stream().map(PayrollItemEntity::getEmployeeId)
                                                                        .collect(Collectors.toSet()).size())
                                        .totalCost(typeTotalCost)
                                        .baseSalaryCost(typeBaseCost)
                                        .overtimeCost(typeOvertimeCost)
                                        .allowanceCost(typeAllowanceCost)
                                        .percentageOfTotal(Math.round(percentage * 100.0) / 100.0)
                                        .build());
                }

                // Phân tích theo loại hợp đồng
                List<CostByContractType> costByContractType = calculateCostByContractType(
                                companyId, allItems, totalLaborCost, startDate, endDate);

                return CostAnalysisReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalLaborCost(totalLaborCost)
                                .totalBaseSalaryCost(totalBaseSalaryCost)
                                .totalOvertimeCost(totalOvertimeCost)
                                .totalAllowanceCost(totalAllowanceCost)
                                .totalDeductionAmount(totalDeductionAmount)
                                .costByContractType(costByContractType)
                                .costBySalaryType(costBySalaryType)
                                .build();
        }

        /**
         * Tính chi phí theo loại hợp đồng
         */
        private List<CostByContractType> calculateCostByContractType(
                        Long companyId, List<PayrollItemEntity> allItems, BigDecimal totalLaborCost,
                        LocalDate startDate, LocalDate endDate) {

                // Lấy contracts active trong khoảng thời gian
                List<com.tamabee.api_hr.entity.contract.EmploymentContractEntity> contracts = employmentContractRepository
                                .findActiveContractsByCompanyIdAndDateRange(
                                                companyId, startDate, endDate);

                // Map employeeId -> contractType
                Map<Long, ContractType> employeeContractType = new HashMap<>();
                for (com.tamabee.api_hr.entity.contract.EmploymentContractEntity contract : contracts) {
                        employeeContractType.put(contract.getEmployeeId(), contract.getContractType());
                }

                // Group items by contract type
                Map<ContractType, List<PayrollItemEntity>> itemsByContractType = new HashMap<>();
                for (PayrollItemEntity item : allItems) {
                        ContractType contractType = employeeContractType.getOrDefault(
                                        item.getEmployeeId(), ContractType.FULL_TIME);
                        itemsByContractType.computeIfAbsent(contractType, k -> new ArrayList<>()).add(item);
                }

                List<CostByContractType> result = new ArrayList<>();
                for (Map.Entry<ContractType, List<PayrollItemEntity>> entry : itemsByContractType.entrySet()) {
                        ContractType contractType = entry.getKey();
                        List<PayrollItemEntity> items = entry.getValue();

                        BigDecimal typeTotalCost = BigDecimal.ZERO;
                        BigDecimal typeBaseCost = BigDecimal.ZERO;
                        BigDecimal typeOvertimeCost = BigDecimal.ZERO;
                        BigDecimal typeAllowanceCost = BigDecimal.ZERO;

                        for (PayrollItemEntity item : items) {
                                typeTotalCost = typeTotalCost.add(
                                                item.getGrossSalary() != null ? item.getGrossSalary()
                                                                : BigDecimal.ZERO);
                                typeBaseCost = typeBaseCost.add(
                                                item.getCalculatedBaseSalary() != null ? item.getCalculatedBaseSalary()
                                                                : BigDecimal.ZERO);
                                typeOvertimeCost = typeOvertimeCost.add(
                                                item.getTotalOvertimePay() != null ? item.getTotalOvertimePay()
                                                                : BigDecimal.ZERO);
                                typeAllowanceCost = typeAllowanceCost.add(
                                                item.getTotalAllowances() != null ? item.getTotalAllowances()
                                                                : BigDecimal.ZERO);
                        }

                        double percentage = totalLaborCost.compareTo(BigDecimal.ZERO) > 0
                                        ? typeTotalCost.divide(totalLaborCost, 4, RoundingMode.HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                                        : 0.0;

                        result.add(CostByContractType.builder()
                                        .contractType(contractType)
                                        .employeeCount(
                                                        items.stream().map(PayrollItemEntity::getEmployeeId)
                                                                        .collect(Collectors.toSet()).size())
                                        .totalCost(typeTotalCost)
                                        .baseSalaryCost(typeBaseCost)
                                        .overtimeCost(typeOvertimeCost)
                                        .allowanceCost(typeAllowanceCost)
                                        .percentageOfTotal(Math.round(percentage * 100.0) / 100.0)
                                        .build());
                }

                return result;
        }

        @Override
        @Transactional(readOnly = true)
        public ShiftUtilizationReport generateShiftUtilization(Long companyId, ReportQuery query) {
                log.info("Generating shift utilization report for company: {}", companyId);

                LocalDate startDate = query.getStartDate();
                LocalDate endDate = query.getEndDate();

                // Lấy tất cả shift templates của công ty
                List<ShiftTemplateEntity> shiftTemplates = shiftTemplateRepository
                                .findByCompanyIdAndDeletedFalse(companyId);

                if (shiftTemplates.isEmpty()) {
                        return buildEmptyShiftUtilizationReport(companyId, startDate, endDate);
                }

                // Lấy tất cả shift assignments trong khoảng thời gian
                List<ShiftAssignmentEntity> allAssignments = new ArrayList<>();
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        List<ShiftAssignmentEntity> dailyAssignments = shiftAssignmentRepository
                                        .findByCompanyIdAndWorkDateAndDeletedFalse(companyId, date);
                        allAssignments.addAll(dailyAssignments);
                }

                // Filter theo shiftTemplateIds nếu có
                if (query.getShiftTemplateIds() != null && !query.getShiftTemplateIds().isEmpty()) {
                        Set<Long> shiftTemplateIdSet = new HashSet<>(query.getShiftTemplateIds());
                        allAssignments = allAssignments.stream()
                                        .filter(a -> shiftTemplateIdSet.contains(a.getShiftTemplateId()))
                                        .collect(Collectors.toList());
                }

                // Tính tổng quan
                int totalAssignments = allAssignments.size();
                int completedShifts = 0;
                int cancelledShifts = 0;
                int swappedShifts = 0;

                for (ShiftAssignmentEntity assignment : allAssignments) {
                        switch (assignment.getStatus()) {
                                case COMPLETED:
                                        completedShifts++;
                                        break;
                                case CANCELLED:
                                        cancelledShifts++;
                                        break;
                                case SWAPPED:
                                        swappedShifts++;
                                        break;
                                default:
                                        break;
                        }
                }

                double shiftCompletionRate = totalAssignments > 0
                                ? (double) completedShifts / totalAssignments * 100
                                : 0.0;

                // Thống kê swap requests
                List<ShiftSwapRequestEntity> swapRequests = getSwapRequestsInDateRange(companyId, startDate, endDate);
                int totalSwapRequests = swapRequests.size();
                int approvedSwaps = 0;
                int rejectedSwaps = 0;
                int pendingSwaps = 0;

                for (ShiftSwapRequestEntity request : swapRequests) {
                        switch (request.getStatus()) {
                                case APPROVED:
                                        approvedSwaps++;
                                        break;
                                case REJECTED:
                                        rejectedSwaps++;
                                        break;
                                case PENDING:
                                        pendingSwaps++;
                                        break;
                        }
                }

                double swapApprovalRate = (approvedSwaps + rejectedSwaps) > 0
                                ? (double) approvedSwaps / (approvedSwaps + rejectedSwaps) * 100
                                : 0.0;

                // Chi tiết theo ca
                Map<Long, List<ShiftAssignmentEntity>> assignmentsByTemplate = allAssignments.stream()
                                .collect(Collectors.groupingBy(ShiftAssignmentEntity::getShiftTemplateId));

                List<ShiftTemplateSummary> shiftSummaries = new ArrayList<>();
                for (ShiftTemplateEntity template : shiftTemplates) {
                        List<ShiftAssignmentEntity> templateAssignments = assignmentsByTemplate
                                        .getOrDefault(template.getId(), Collections.emptyList());

                        int templateTotal = templateAssignments.size();
                        int templateCompleted = 0;
                        int templateCancelled = 0;
                        int templateSwapped = 0;

                        for (ShiftAssignmentEntity assignment : templateAssignments) {
                                switch (assignment.getStatus()) {
                                        case COMPLETED:
                                                templateCompleted++;
                                                break;
                                        case CANCELLED:
                                                templateCancelled++;
                                                break;
                                        case SWAPPED:
                                                templateSwapped++;
                                                break;
                                        default:
                                                break;
                                }
                        }

                        double utilizationRate = totalAssignments > 0
                                        ? (double) templateTotal / totalAssignments * 100
                                        : 0.0;
                        double completionRate = templateTotal > 0
                                        ? (double) templateCompleted / templateTotal * 100
                                        : 0.0;

                        shiftSummaries.add(ShiftTemplateSummary.builder()
                                        .shiftTemplateId(template.getId())
                                        .shiftName(template.getName())
                                        .startTime(template.getStartTime())
                                        .endTime(template.getEndTime())
                                        .totalAssignments(templateTotal)
                                        .completedAssignments(templateCompleted)
                                        .cancelledAssignments(templateCancelled)
                                        .swappedAssignments(templateSwapped)
                                        .utilizationRate(Math.round(utilizationRate * 100.0) / 100.0)
                                        .completionRate(Math.round(completionRate * 100.0) / 100.0)
                                        .build());
                }

                return ShiftUtilizationReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalShiftAssignments(totalAssignments)
                                .completedShifts(completedShifts)
                                .cancelledShifts(cancelledShifts)
                                .swappedShifts(swappedShifts)
                                .shiftCompletionRate(Math.round(shiftCompletionRate * 100.0) / 100.0)
                                .totalSwapRequests(totalSwapRequests)
                                .approvedSwaps(approvedSwaps)
                                .rejectedSwaps(rejectedSwaps)
                                .pendingSwaps(pendingSwaps)
                                .swapApprovalRate(Math.round(swapApprovalRate * 100.0) / 100.0)
                                .shiftSummaries(shiftSummaries)
                                .build();
        }

        /**
         * Lấy swap requests trong khoảng thời gian
         */
        private List<ShiftSwapRequestEntity> getSwapRequestsInDateRange(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                // Lấy tất cả swap requests của công ty
                List<ShiftSwapRequestEntity> allRequests = shiftSwapRequestRepository
                                .findByCompanyId(companyId, org.springframework.data.domain.Pageable.unpaged())
                                .getContent();

                // Filter theo ngày tạo trong khoảng thời gian
                return allRequests.stream()
                                .filter(r -> {
                                        LocalDate createdDate = r.getCreatedAt().toLocalDate();
                                        return !createdDate.isBefore(startDate) && !createdDate.isAfter(endDate);
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public byte[] exportReport(ReportType type, Long companyId, ReportQuery query, ExportFormat format,
                        String language) {
                log.info("Exporting report type: {} for company: {} in format: {} with language: {}",
                                type, companyId, format, language);

                // Mặc định là tiếng Việt nếu không có language
                String lang = language != null ? language : "vi";

                try {
                        switch (type) {
                                case ATTENDANCE_SUMMARY:
                                        AttendanceSummaryReport attendanceReport = generateAttendanceSummary(companyId,
                                                        query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportAttendanceSummaryToCsv(
                                                                        attendanceReport, lang)
                                                        : reportExportService.exportAttendanceSummaryToPdf(
                                                                        attendanceReport, lang);

                                case OVERTIME:
                                        OvertimeReport overtimeReport = generateOvertimeReport(companyId, query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportOvertimeToCsv(overtimeReport, lang)
                                                        : reportExportService.exportOvertimeToPdf(overtimeReport, lang);

                                case BREAK_COMPLIANCE:
                                        BreakComplianceReport breakReport = generateBreakComplianceReport(companyId,
                                                        query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportBreakComplianceToCsv(breakReport,
                                                                        lang)
                                                        : reportExportService.exportBreakComplianceToPdf(breakReport,
                                                                        lang);

                                case PAYROLL_SUMMARY:
                                        PayrollSummaryReport payrollReport = generatePayrollSummary(companyId, query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportPayrollSummaryToCsv(payrollReport,
                                                                        lang)
                                                        : reportExportService.exportPayrollSummaryToPdf(payrollReport,
                                                                        lang);

                                case COST_ANALYSIS:
                                        CostAnalysisReport costReport = generateCostAnalysis(companyId, query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportCostAnalysisToCsv(costReport, lang)
                                                        : reportExportService.exportCostAnalysisToPdf(costReport, lang);

                                case SHIFT_UTILIZATION:
                                        ShiftUtilizationReport shiftReport = generateShiftUtilization(companyId, query);
                                        return format == ExportFormat.CSV
                                                        ? reportExportService.exportShiftUtilizationToCsv(shiftReport,
                                                                        lang)
                                                        : reportExportService.exportShiftUtilizationToPdf(shiftReport,
                                                                        lang);

                                default:
                                        throw new IllegalArgumentException("Unsupported report type: " + type);
                        }
                } catch (Exception e) {
                        log.error("Error exporting report: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to export report", e);
                }
        }

        // ==================== Helper Methods ====================

        /**
         * Lấy danh sách nhân viên theo filter
         */
        private List<UserEntity> getFilteredEmployees(Long companyId, ReportQuery query) {
                List<UserEntity> employees = userRepository.findByCompanyIdAndDeletedFalse(companyId);

                // Filter theo employeeIds nếu có
                if (query.getEmployeeIds() != null && !query.getEmployeeIds().isEmpty()) {
                        Set<Long> employeeIdSet = new HashSet<>(query.getEmployeeIds());
                        employees = employees.stream()
                                        .filter(e -> employeeIdSet.contains(e.getId()))
                                        .collect(Collectors.toList());
                }

                return employees;
        }

        /**
         * Tính số ngày làm việc (không tính thứ 7, CN)
         */
        private int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
                int workingDays = 0;
                LocalDate date = startDate;
                while (!date.isAfter(endDate)) {
                        int dayOfWeek = date.getDayOfWeek().getValue();
                        if (dayOfWeek < 6) { // Thứ 2 - Thứ 6
                                workingDays++;
                        }
                        date = date.plusDays(1);
                }
                return workingDays;
        }

        /**
         * Tính tổng hợp chấm công của một nhân viên
         */
        private EmployeeAttendanceSummary calculateEmployeeAttendanceSummary(
                        UserEntity employee, LocalDate startDate, LocalDate endDate) {

                List<AttendanceRecordEntity> records = attendanceRecordRepository
                                .findByEmployeeIdAndWorkDateBetween(employee.getId(), startDate, endDate);

                int presentDays = 0;
                int lateCount = 0;
                int totalLateMinutes = 0;
                int earlyLeaveCount = 0;
                int totalEarlyLeaveMinutes = 0;
                int totalWorkingMinutes = 0;

                for (AttendanceRecordEntity record : records) {
                        if (record.getStatus() == AttendanceStatus.PRESENT ||
                                        record.getStatus() == AttendanceStatus.LATE) {
                                presentDays++;
                        }

                        if (record.getLateMinutes() != null && record.getLateMinutes() > 0) {
                                lateCount++;
                                totalLateMinutes += record.getLateMinutes();
                        }

                        if (record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0) {
                                earlyLeaveCount++;
                                totalEarlyLeaveMinutes += record.getEarlyLeaveMinutes();
                        }

                        if (record.getWorkingMinutes() != null) {
                                totalWorkingMinutes += record.getWorkingMinutes();
                        }
                }

                int totalWorkingDays = calculateWorkingDays(startDate, endDate);
                int absentDays = totalWorkingDays - presentDays;
                int avgWorkingMinutes = presentDays > 0 ? totalWorkingMinutes / presentDays : 0;

                String employeeName = employee.getProfile() != null
                                ? employee.getProfile().getName()
                                : employee.getEmail();

                return EmployeeAttendanceSummary.builder()
                                .employeeId(employee.getId())
                                .employeeCode(employee.getEmployeeCode())
                                .employeeName(employeeName)
                                .totalWorkingDays(totalWorkingDays)
                                .presentDays(presentDays)
                                .absentDays(Math.max(0, absentDays))
                                .lateCount(lateCount)
                                .totalLateMinutes(totalLateMinutes)
                                .earlyLeaveCount(earlyLeaveCount)
                                .totalEarlyLeaveMinutes(totalEarlyLeaveMinutes)
                                .totalWorkingMinutes(totalWorkingMinutes)
                                .averageWorkingMinutesPerDay(avgWorkingMinutes)
                                .build();
        }

        /**
         * Tính tổng hợp overtime của một nhân viên
         */
        private EmployeeOvertimeSummary calculateEmployeeOvertimeSummary(
                        UserEntity employee, LocalDate startDate, LocalDate endDate) {

                List<AttendanceRecordEntity> records = attendanceRecordRepository
                                .findByEmployeeIdAndWorkDateBetween(employee.getId(), startDate, endDate);

                int regularOT = 0, nightOT = 0, holidayOT = 0, weekendOT = 0;

                for (AttendanceRecordEntity record : records) {
                        if (record.getOvertimeMinutes() != null && record.getOvertimeMinutes() > 0) {
                                // Phân loại overtime dựa vào ngày trong tuần
                                int dayOfWeek = record.getWorkDate().getDayOfWeek().getValue();
                                if (dayOfWeek == 6 || dayOfWeek == 7) {
                                        weekendOT += record.getOvertimeMinutes();
                                } else {
                                        regularOT += record.getOvertimeMinutes();
                                }
                        }
                }

                int totalOT = regularOT + nightOT + holidayOT + weekendOT;

                String employeeName = employee.getProfile() != null
                                ? employee.getProfile().getName()
                                : employee.getEmail();

                return EmployeeOvertimeSummary.builder()
                                .employeeId(employee.getId())
                                .employeeCode(employee.getEmployeeCode())
                                .employeeName(employeeName)
                                .regularOvertimeMinutes(regularOT)
                                .nightOvertimeMinutes(nightOT)
                                .holidayOvertimeMinutes(holidayOT)
                                .weekendOvertimeMinutes(weekendOT)
                                .totalOvertimeMinutes(totalOT)
                                .regularOvertimePay(BigDecimal.ZERO)
                                .nightOvertimePay(BigDecimal.ZERO)
                                .holidayOvertimePay(BigDecimal.ZERO)
                                .weekendOvertimePay(BigDecimal.ZERO)
                                .totalOvertimePay(BigDecimal.ZERO)
                                .build();
        }

        /**
         * Tính tổng hợp break của một nhân viên
         */
        private EmployeeBreakSummary calculateEmployeeBreakSummary(
                        UserEntity employee, LocalDate startDate, LocalDate endDate) {

                List<AttendanceRecordEntity> attendanceRecords = attendanceRecordRepository
                                .findByEmployeeIdAndWorkDateBetween(employee.getId(), startDate, endDate);

                int totalBreakCount = 0;
                int totalBreakMinutes = 0;
                int compliantCount = 0;
                int nonCompliantCount = 0;
                int daysWithBreaks = 0;

                for (AttendanceRecordEntity attendance : attendanceRecords) {
                        List<BreakRecordEntity> breaks = breakRecordRepository
                                        .findByAttendanceRecordIdAndDeletedFalse(attendance.getId());

                        if (!breaks.isEmpty()) {
                                daysWithBreaks++;
                                totalBreakCount += breaks.size();

                                for (BreakRecordEntity br : breaks) {
                                        if (br.getActualBreakMinutes() != null) {
                                                totalBreakMinutes += br.getActualBreakMinutes();
                                        }
                                }

                                // Kiểm tra compliance dựa vào attendance record
                                if (attendance.getBreakCompliant() != null && attendance.getBreakCompliant()) {
                                        compliantCount++;
                                } else if (attendance.getBreakCompliant() != null) {
                                        nonCompliantCount++;
                                }
                        }
                }

                int avgBreakMinutes = daysWithBreaks > 0 ? totalBreakMinutes / daysWithBreaks : 0;
                double complianceRate = (compliantCount + nonCompliantCount) > 0
                                ? (double) compliantCount / (compliantCount + nonCompliantCount) * 100
                                : 100.0;

                String employeeName = employee.getProfile() != null
                                ? employee.getProfile().getName()
                                : employee.getEmail();

                return EmployeeBreakSummary.builder()
                                .employeeId(employee.getId())
                                .employeeCode(employee.getEmployeeCode())
                                .employeeName(employeeName)
                                .totalBreakCount(totalBreakCount)
                                .totalBreakMinutes(totalBreakMinutes)
                                .averageBreakMinutesPerDay(avgBreakMinutes)
                                .compliantBreakCount(compliantCount)
                                .nonCompliantBreakCount(nonCompliantCount)
                                .complianceRate(Math.round(complianceRate * 100.0) / 100.0)
                                .build();
        }

        // ==================== Empty Report Builders ====================

        private AttendanceSummaryReport buildEmptyAttendanceSummaryReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return AttendanceSummaryReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(0)
                                .totalWorkingDays(calculateWorkingDays(startDate, endDate))
                                .totalPresentDays(0)
                                .totalAbsentDays(0)
                                .totalLateCount(0)
                                .totalEarlyLeaveCount(0)
                                .attendanceRate(0.0)
                                .punctualityRate(0.0)
                                .employeeSummaries(Collections.emptyList())
                                .build();
        }

        private OvertimeReport buildEmptyOvertimeReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return OvertimeReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployeesWithOvertime(0)
                                .totalRegularOvertimeMinutes(0)
                                .totalNightOvertimeMinutes(0)
                                .totalHolidayOvertimeMinutes(0)
                                .totalWeekendOvertimeMinutes(0)
                                .totalOvertimeMinutes(0)
                                .totalRegularOvertimePay(BigDecimal.ZERO)
                                .totalNightOvertimePay(BigDecimal.ZERO)
                                .totalHolidayOvertimePay(BigDecimal.ZERO)
                                .totalWeekendOvertimePay(BigDecimal.ZERO)
                                .totalOvertimePay(BigDecimal.ZERO)
                                .employeeSummaries(Collections.emptyList())
                                .build();
        }

        private BreakComplianceReport buildEmptyBreakComplianceReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return BreakComplianceReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(0)
                                .totalBreakCount(0)
                                .totalBreakMinutes(0)
                                .averageBreakMinutesPerEmployee(0)
                                .compliantBreakCount(0)
                                .nonCompliantBreakCount(0)
                                .overallComplianceRate(100.0)
                                .employeeSummaries(Collections.emptyList())
                                .build();
        }

        private PayrollSummaryReport buildEmptyPayrollSummaryReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return PayrollSummaryReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalEmployees(0)
                                .totalBaseSalary(BigDecimal.ZERO)
                                .totalOvertimePay(BigDecimal.ZERO)
                                .totalAllowances(BigDecimal.ZERO)
                                .totalDeductions(BigDecimal.ZERO)
                                .totalGrossSalary(BigDecimal.ZERO)
                                .totalNetSalary(BigDecimal.ZERO)
                                .averageGrossSalary(BigDecimal.ZERO)
                                .averageNetSalary(BigDecimal.ZERO)
                                .employeeSummaries(Collections.emptyList())
                                .build();
        }

        private CostAnalysisReport buildEmptyCostAnalysisReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return CostAnalysisReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalLaborCost(BigDecimal.ZERO)
                                .totalBaseSalaryCost(BigDecimal.ZERO)
                                .totalOvertimeCost(BigDecimal.ZERO)
                                .totalAllowanceCost(BigDecimal.ZERO)
                                .totalDeductionAmount(BigDecimal.ZERO)
                                .costByContractType(Collections.emptyList())
                                .costBySalaryType(Collections.emptyList())
                                .build();
        }

        private ShiftUtilizationReport buildEmptyShiftUtilizationReport(
                        Long companyId, LocalDate startDate, LocalDate endDate) {
                return ShiftUtilizationReport.builder()
                                .companyId(companyId)
                                .startDate(startDate)
                                .endDate(endDate)
                                .totalShiftAssignments(0)
                                .completedShifts(0)
                                .cancelledShifts(0)
                                .swappedShifts(0)
                                .shiftCompletionRate(0.0)
                                .totalSwapRequests(0)
                                .approvedSwaps(0)
                                .rejectedSwaps(0)
                                .pendingSwaps(0)
                                .swapApprovalRate(0.0)
                                .shiftSummaries(Collections.emptyList())
                                .build();
        }
}
