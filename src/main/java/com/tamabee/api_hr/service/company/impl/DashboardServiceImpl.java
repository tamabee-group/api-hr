package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.response.dashboard.DashboardStatsResponse;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.repository.attendance.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.leave.LeaveRequestRepository;
import com.tamabee.api_hr.repository.payroll.PayrollPeriodRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.IDashboardService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;

/**
 * Service thống kê tổng quan cho Dashboard
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceAdjustmentRequestRepository adjustmentRequestRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDate monthStart = today.withDayOfMonth(1);

        // Thống kê nhân sự
        var allUsers = userRepository.findByDeletedFalse();
        long totalEmployees = allUsers.size();
        long activeEmployees = allUsers.stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .count();
        long inactiveEmployees = totalEmployees - activeEmployees;

        // Chấm công hôm nay
        var todayRecords = attendanceRecordRepository
                .findByWorkDateBetween(today, today, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        long todayPresent = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                .count();
        long todayLate = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.LATE)
                .count();
        long todayOnLeave = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.LEAVE)
                .count();
        long todayAbsent = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                .count();

        // Yêu cầu chờ duyệt
        long pendingLeave = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        long pendingAdjustment = adjustmentRequestRepository.countPending();

        // Chấm công 7 ngày gần nhất
        List<DashboardStatsResponse.DailyAttendance> weeklyAttendance = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            var records = attendanceRecordRepository
                    .findByWorkDateBetween(date, date, org.springframework.data.domain.Pageable.unpaged())
                    .getContent();

            weeklyAttendance.add(DashboardStatsResponse.DailyAttendance.builder()
                    .date(date.toString())
                    .present(records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count())
                    .absent(records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count())
                    .late(records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count())
                    .onLeave(records.stream().filter(r -> r.getStatus() == AttendanceStatus.LEAVE).count())
                    .build());
        }

        // Nghỉ phép tháng này
        var monthlyLeaves = leaveRequestRepository.findAll(org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(l -> !l.getStartDate().isBefore(monthStart) && !l.getStartDate().isAfter(today))
                .toList();

        long monthlyLeaveApproved = monthlyLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        long monthlyLeaveRejected = monthlyLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();
        long monthlyLeavePending = monthlyLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING).count();

        // Tổng hợp lương 6 tháng gần nhất
        List<DashboardStatsResponse.MonthlyPayroll> payrollOverview = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = today.minusMonths(i);
            int targetYear = targetMonth.getYear();
            int targetMonthValue = targetMonth.getMonthValue();
            String monthKey = String.format("%d-%02d", targetYear, targetMonthValue);

            var periodOpt = payrollPeriodRepository.findByYearAndMonth(targetYear, targetMonthValue);
            if (periodOpt.isPresent()) {
                var period = periodOpt.get();
                payrollOverview.add(DashboardStatsResponse.MonthlyPayroll.builder()
                        .month(monthKey)
                        .totalGross(period.getTotalGrossSalary() != null
                                ? period.getTotalGrossSalary().longValue() : 0)
                        .totalNet(period.getTotalNetSalary() != null
                                ? period.getTotalNetSalary().longValue() : 0)
                        .totalEmployees(period.getTotalEmployees() != null
                                ? period.getTotalEmployees() : 0)
                        .status(period.getStatus().name())
                        .build());
            } else {
                payrollOverview.add(DashboardStatsResponse.MonthlyPayroll.builder()
                        .month(monthKey)
                        .totalGross(0)
                        .totalNet(0)
                        .totalEmployees(0)
                        .status(null)
                        .build());
            }
        }

        // Tổng thời gian chấm công 12 tháng gần nhất
        List<DashboardStatsResponse.MonthlyAttendanceHours> monthlyAttendanceHours = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate targetMonth = today.minusMonths(i);
            LocalDate monthStartDate = targetMonth.withDayOfMonth(1);
            LocalDate monthEndDate = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
            String monthKey = String.format("%d-%02d", targetMonth.getYear(), targetMonth.getMonthValue());

            long workingMinutes = attendanceRecordRepository.sumWorkingMinutesByWorkDateBetween(monthStartDate, monthEndDate);
            long overtimeMinutes = attendanceRecordRepository.sumOvertimeMinutesByWorkDateBetween(monthStartDate, monthEndDate);

            monthlyAttendanceHours.add(DashboardStatsResponse.MonthlyAttendanceHours.builder()
                    .month(monthKey)
                    .totalWorkingHours(workingMinutes / 60)
                    .totalOvertimeHours(overtimeMinutes / 60)
                    .build());
        }

        return DashboardStatsResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .inactiveEmployees(inactiveEmployees)
                .todayPresent(todayPresent)
                .todayAbsent(todayAbsent)
                .todayLate(todayLate)
                .todayOnLeave(todayOnLeave)
                .pendingLeaveRequests(pendingLeave)
                .pendingAdjustmentRequests(pendingAdjustment)
                .weeklyAttendance(weeklyAttendance)
                .monthlyLeaveApproved(monthlyLeaveApproved)
                .monthlyLeaveRejected(monthlyLeaveRejected)
                .monthlyLeavePending(monthlyLeavePending)
                .payrollOverview(payrollOverview)
                .monthlyAttendanceHours(monthlyAttendanceHours)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getPendingCounts() {
        long pendingAdjustments = adjustmentRequestRepository.countPending();
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        return Map.of(
                "pendingAdjustments", pendingAdjustments,
                "pendingLeaves", pendingLeaves
        );
    }
}
