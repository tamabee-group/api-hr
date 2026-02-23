package com.tamabee.api_hr.dto.response.dashboard;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Response chứa thống kê tổng quan cho Dashboard
 */
@Data
@Builder
public class DashboardStatsResponse {

    // Nhân sự
    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;

    // Chấm công hôm nay
    private long todayPresent;
    private long todayAbsent;
    private long todayLate;
    private long todayOnLeave;

    // Yêu cầu chờ duyệt
    private long pendingLeaveRequests;
    private long pendingAdjustmentRequests;

    // Chấm công 7 ngày gần nhất (cho biểu đồ)
    private List<DailyAttendance> weeklyAttendance;

    // Phân bổ trạng thái nghỉ phép tháng này
    private long monthlyLeaveApproved;
    private long monthlyLeaveRejected;
    private long monthlyLeavePending;

    // Tổng hợp lương 6 tháng gần nhất (cho biểu đồ)
    private List<MonthlyPayroll> payrollOverview;

    // Tổng thời gian chấm công 12 tháng gần nhất (cho biểu đồ)
    private List<MonthlyAttendanceHours> monthlyAttendanceHours;

    @Data
    @Builder
    public static class DailyAttendance {
        private String date;
        private long present;
        private long absent;
        private long late;
        private long onLeave;
    }

    @Data
    @Builder
    public static class MonthlyPayroll {
        private String month;
        private long totalGross;
        private long totalNet;
        private int totalEmployees;
        private String status;
    }

    @Data
    @Builder
    public static class MonthlyAttendanceHours {
        private String month;
        private long totalWorkingHours;
        private long totalOvertimeHours;
    }
}
