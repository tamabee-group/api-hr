package com.tamabee.api_hr.controller.company;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.response.dashboard.DashboardStatsResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.IDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho Dashboard thống kê tổng quan
 */
@RestController
@RequestMapping("/api/company/dashboard")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class DashboardController {

    private final IDashboardService dashboardService;

    /**
     * Lấy thống kê tổng quan
     * GET /api/company/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<BaseResponse<DashboardStatsResponse>> getStats() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return ResponseEntity.ok(BaseResponse.success(stats, "Lấy thống kê thành công"));
    }

    /**
     * Lấy số yêu cầu chờ duyệt (sidebar badge)
     * GET /api/company/dashboard/pending-counts
     */
    @GetMapping("/pending-counts")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getPendingCounts() {
        Map<String, Long> counts = dashboardService.getPendingCounts();
        return ResponseEntity.ok(BaseResponse.success(counts, "Lấy pending counts thành công"));
    }
}
