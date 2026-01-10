package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.response.DailyBreakReportResponse;
import com.tamabee.api_hr.dto.response.DailyBreakReportResponse.BreakSessionInfo;
import com.tamabee.api_hr.dto.response.DailyBreakReportResponse.EmployeeBreakDetail;
import com.tamabee.api_hr.dto.response.MonthlyBreakReportResponse;
import com.tamabee.api_hr.dto.response.MonthlyBreakReportResponse.EmployeeMonthlyBreakDetail;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import com.tamabee.api_hr.service.company.IBreakReportService;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation của IBreakReportService.
 * Tạo báo cáo giờ giải lao hàng ngày và hàng tháng.
 */
@Service
@RequiredArgsConstructor
public class BreakReportServiceImpl implements IBreakReportService {

        private final BreakRecordRepository breakRecordRepository;
        private final UserRepository userRepository;
        private final ICompanySettingsService companySettingsService;
        private final LegalBreakRequirements legalBreakRequirements;

        // Ngưỡng để đánh dấu "thường xuyên" không tuân thủ
        private static final int FREQUENT_THRESHOLD = 3;

        @Override
        @Transactional(readOnly = true)
        public DailyBreakReportResponse generateDailyBreakReport(LocalDate date) {
                // Lấy cấu hình break của công ty
                BreakConfig breakConfig = companySettingsService.getBreakConfig();

                // Lấy tất cả bản ghi giải lao trong ngày
                List<BreakRecordEntity> breakRecords = breakRecordRepository
                                .findByWorkDateBetween(date, date);

                // Lấy danh sách nhân viên của công ty
                List<UserEntity> employees = userRepository.findByDeletedFalse();
                Map<Long, UserEntity> employeeMap = employees.stream()
                                .collect(Collectors.toMap(UserEntity::getId, e -> e));

                // Nhóm bản ghi theo nhân viên
                Map<Long, List<BreakRecordEntity>> recordsByEmployee = breakRecords.stream()
                                .collect(Collectors.groupingBy(BreakRecordEntity::getEmployeeId));

                // Tính toán chi tiết cho từng nhân viên
                List<EmployeeBreakDetail> employeeDetails = new ArrayList<>();
                int compliantCount = 0;
                int nonCompliantCount = 0;
                int exceededMaxCount = 0;
                int totalBreakMinutes = 0;

                for (Map.Entry<Long, List<BreakRecordEntity>> entry : recordsByEmployee.entrySet()) {
                        Long employeeId = entry.getKey();
                        List<BreakRecordEntity> empRecords = entry.getValue();
                        UserEntity employee = employeeMap.get(employeeId);

                        // Sắp xếp breaks theo breakNumber
                        empRecords.sort(Comparator.comparing(
                                        BreakRecordEntity::getBreakNumber,
                                        Comparator.nullsLast(Integer::compareTo)));

                        // Tạo breakSessions list từ các break records
                        List<BreakSessionInfo> breakSessions = empRecords.stream()
                                        .map(record -> BreakSessionInfo.builder()
                                                        .breakRecordId(record.getId())
                                                        .breakNumber(record.getBreakNumber())
                                                        .breakStart(record.getBreakStart())
                                                        .breakEnd(record.getBreakEnd())
                                                        .durationMinutes(record.getActualBreakMinutes())
                                                        .build())
                                        .collect(Collectors.toList());

                        // Tính tổng thời gian giải lao từ tất cả sessions
                        int actualMinutes = empRecords.stream()
                                        .mapToInt(r -> r.getActualBreakMinutes() != null ? r.getActualBreakMinutes()
                                                        : 0)
                                        .sum();
                        int effectiveMinutes = empRecords.stream()
                                        .mapToInt(r -> r.getEffectiveBreakMinutes() != null
                                                        ? r.getEffectiveBreakMinutes()
                                                        : 0)
                                        .sum();

                        // Lấy minimum break yêu cầu (giả sử 8 giờ làm việc)
                        int minimumRequired = getMinimumBreakRequired(breakConfig, 8);

                        // Kiểm tra tuân thủ
                        boolean isCompliant = effectiveMinutes >= minimumRequired;
                        boolean exceededMax = actualMinutes > breakConfig.getMaximumBreakMinutes();

                        if (isCompliant) {
                                compliantCount++;
                        } else {
                                nonCompliantCount++;
                        }
                        if (exceededMax) {
                                exceededMaxCount++;
                        }
                        totalBreakMinutes += effectiveMinutes;

                        EmployeeBreakDetail detail = EmployeeBreakDetail.builder()
                                        .employeeId(employeeId)
                                        .employeeCode(employee != null ? employee.getEmployeeCode() : null)
                                        .employeeName(employee != null && employee.getProfile() != null
                                                        ? employee.getProfile().getName()
                                                        : null)
                                        .totalActualBreakMinutes(actualMinutes)
                                        .totalEffectiveBreakMinutes(effectiveMinutes)
                                        .breakCount(empRecords.size())
                                        .breakCompliant(isCompliant)
                                        .minimumBreakRequired(minimumRequired)
                                        .exceededMaximum(exceededMax)
                                        .breakSessions(breakSessions)
                                        .build();

                        employeeDetails.add(detail);
                }

                // Sắp xếp theo tên nhân viên
                employeeDetails.sort(Comparator.comparing(
                                d -> d.getEmployeeName() != null ? d.getEmployeeName() : "",
                                Comparator.nullsLast(String::compareTo)));

                int totalEmployees = recordsByEmployee.size();
                double averageBreak = totalEmployees > 0 ? (double) totalBreakMinutes / totalEmployees : 0;
                double complianceRate = totalEmployees > 0 ? (double) compliantCount / totalEmployees * 100 : 0;

                return DailyBreakReportResponse.builder()
                                .reportDate(date)
                                .totalEmployees(totalEmployees)
                                .totalBreakMinutes(totalBreakMinutes)
                                .averageBreakMinutes(Math.round(averageBreak * 100.0) / 100.0)
                                .compliantEmployees(compliantCount)
                                .nonCompliantEmployees(nonCompliantCount)
                                .complianceRate(Math.round(complianceRate * 100.0) / 100.0)
                                .exceededMaximumEmployees(exceededMaxCount)
                                .breakType(breakConfig.getBreakType())
                                .employeeDetails(employeeDetails)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public MonthlyBreakReportResponse generateMonthlyBreakReport(YearMonth yearMonth) {
                // Lấy cấu hình break của công ty
                BreakConfig breakConfig = companySettingsService.getBreakConfig();

                // Xác định khoảng thời gian
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                // Lấy tất cả bản ghi giải lao trong tháng
                List<BreakRecordEntity> breakRecords = breakRecordRepository
                                .findByWorkDateBetween(startDate, endDate);

                // Lấy danh sách nhân viên của công ty
                List<UserEntity> employees = userRepository.findByDeletedFalse();
                Map<Long, UserEntity> employeeMap = employees.stream()
                                .collect(Collectors.toMap(UserEntity::getId, e -> e));

                // Nhóm bản ghi theo nhân viên
                Map<Long, List<BreakRecordEntity>> recordsByEmployee = breakRecords.stream()
                                .collect(Collectors.groupingBy(BreakRecordEntity::getEmployeeId));

                // Tính số ngày làm việc có bản ghi
                Set<LocalDate> workingDates = breakRecords.stream()
                                .map(BreakRecordEntity::getWorkDate)
                                .collect(Collectors.toSet());

                // Tính toán chi tiết cho từng nhân viên
                List<EmployeeMonthlyBreakDetail> employeeDetails = new ArrayList<>();
                long totalBreakMinutes = 0;
                int totalNonCompliant = 0;
                int totalExceededMax = 0;

                for (Map.Entry<Long, List<BreakRecordEntity>> entry : recordsByEmployee.entrySet()) {
                        Long employeeId = entry.getKey();
                        List<BreakRecordEntity> empRecords = entry.getValue();
                        UserEntity employee = employeeMap.get(employeeId);

                        // Nhóm theo ngày để tính số ngày làm việc và compliance
                        Map<LocalDate, List<BreakRecordEntity>> recordsByDate = empRecords.stream()
                                        .collect(Collectors.groupingBy(BreakRecordEntity::getWorkDate));

                        int empWorkingDays = recordsByDate.size();
                        long empActualMinutes = 0;
                        long empEffectiveMinutes = 0;
                        int empNonCompliantCount = 0;
                        int empExceededMaxCount = 0;

                        for (Map.Entry<LocalDate, List<BreakRecordEntity>> dateEntry : recordsByDate.entrySet()) {
                                List<BreakRecordEntity> dayRecords = dateEntry.getValue();

                                int dayActual = dayRecords.stream()
                                                .mapToInt(r -> r.getActualBreakMinutes() != null
                                                                ? r.getActualBreakMinutes()
                                                                : 0)
                                                .sum();
                                int dayEffective = dayRecords.stream()
                                                .mapToInt(r -> r.getEffectiveBreakMinutes() != null
                                                                ? r.getEffectiveBreakMinutes()
                                                                : 0)
                                                .sum();

                                empActualMinutes += dayActual;
                                empEffectiveMinutes += dayEffective;

                                // Kiểm tra tuân thủ cho ngày này
                                int minimumRequired = getMinimumBreakRequired(breakConfig, 8);
                                if (dayEffective < minimumRequired) {
                                        empNonCompliantCount++;
                                }
                                if (dayActual > breakConfig.getMaximumBreakMinutes()) {
                                        empExceededMaxCount++;
                                }
                        }

                        totalBreakMinutes += empEffectiveMinutes;
                        totalNonCompliant += empNonCompliantCount;
                        totalExceededMax += empExceededMaxCount;

                        double avgPerDay = empWorkingDays > 0 ? (double) empEffectiveMinutes / empWorkingDays : 0;
                        int compliantDays = empWorkingDays - empNonCompliantCount;
                        double complianceRate = empWorkingDays > 0 ? (double) compliantDays / empWorkingDays * 100 : 0;

                        EmployeeMonthlyBreakDetail detail = EmployeeMonthlyBreakDetail.builder()
                                        .employeeId(employeeId)
                                        .employeeCode(employee != null ? employee.getEmployeeCode() : null)
                                        .employeeName(employee != null && employee.getProfile() != null
                                                        ? employee.getProfile().getName()
                                                        : null)
                                        .workingDays(empWorkingDays)
                                        .totalActualBreakMinutes(empActualMinutes)
                                        .totalEffectiveBreakMinutes(empEffectiveMinutes)
                                        .averageBreakMinutesPerDay(Math.round(avgPerDay * 100.0) / 100.0)
                                        .nonCompliantCount(empNonCompliantCount)
                                        .exceededMaximumCount(empExceededMaxCount)
                                        .complianceRate(Math.round(complianceRate * 100.0) / 100.0)
                                        .frequentlyNonCompliant(empNonCompliantCount >= FREQUENT_THRESHOLD)
                                        .frequentlyExceededMaximum(empExceededMaxCount >= FREQUENT_THRESHOLD)
                                        .build();

                        employeeDetails.add(detail);
                }

                // Sắp xếp theo tên nhân viên
                employeeDetails.sort(Comparator.comparing(
                                d -> d.getEmployeeName() != null ? d.getEmployeeName() : "",
                                Comparator.nullsLast(String::compareTo)));

                int totalEmployees = recordsByEmployee.size();
                int totalWorkingDays = workingDates.size();
                double avgPerDay = (totalEmployees > 0 && totalWorkingDays > 0)
                                ? (double) totalBreakMinutes / totalEmployees / totalWorkingDays
                                : 0;

                // Tính tỷ lệ tuân thủ trung bình
                double avgComplianceRate = employeeDetails.isEmpty() ? 0
                                : employeeDetails.stream()
                                                .mapToDouble(EmployeeMonthlyBreakDetail::getComplianceRate)
                                                .average()
                                                .orElse(0);

                return MonthlyBreakReportResponse.builder()
                                .reportMonth(yearMonth)
                                .totalEmployees(totalEmployees)
                                .totalWorkingDays(totalWorkingDays)
                                .totalBreakMinutes(totalBreakMinutes)
                                .averageBreakMinutesPerDay(Math.round(avgPerDay * 100.0) / 100.0)
                                .totalNonCompliantCount(totalNonCompliant)
                                .totalExceededMaximumCount(totalExceededMax)
                                .averageComplianceRate(Math.round(avgComplianceRate * 100.0) / 100.0)
                                .breakType(breakConfig.getBreakType())
                                .employeeDetails(employeeDetails)
                                .build();
        }

        /**
         * Lấy thời gian giải lao tối thiểu yêu cầu dựa trên cấu hình
         */
        private int getMinimumBreakRequired(BreakConfig config, int workingHours) {
                if (config.getUseLegalMinimum()) {
                        int legalMinimum = legalBreakRequirements.getMinimumBreak(
                                        config.getLocale(), workingHours, false);
                        return Math.max(legalMinimum, config.getMinimumBreakMinutes());
                }
                return config.getMinimumBreakMinutes();
        }
}
