package com.tamabee.api_hr.service.company;

import java.util.Map;

import com.tamabee.api_hr.dto.response.dashboard.DashboardStatsResponse;

/**
 * Service interface cho Dashboard thống kê
 */
public interface IDashboardService {

    /**
     * Lấy thống kê tổng quan cho Dashboard
     */
    DashboardStatsResponse getStats();

    /**
     * Lấy số yêu cầu chờ duyệt (điều chỉnh + nghỉ phép)
     */
    Map<String, Long> getPendingCounts();
}
