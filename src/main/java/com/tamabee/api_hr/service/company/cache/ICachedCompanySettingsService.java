package com.tamabee.api_hr.service.company.cache;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;

/**
 * Service cung cấp company settings với per-entity caching.
 * Sử dụng request-scoped cache để tránh truy vấn database nhiều lần trong cùng request.
 * Cache từng setting entity riêng biệt (attendance, break, payroll, overtime).
 */
public interface ICachedCompanySettingsService {

    /**
     * Lấy AttendanceConfig với caching
     */
    AttendanceConfig getAttendanceConfig();

    /**
     * Lấy PayrollConfig với caching
     */
    PayrollConfig getPayrollConfig();

    /**
     * Lấy OvertimeConfig với caching
     */
    OvertimeConfig getOvertimeConfig();

    /**
     * Lấy BreakConfig với caching
     */
    BreakConfig getBreakConfig();

    /**
     * Invalidate toàn bộ cache
     */
    void invalidateCache();

    /**
     * Invalidate chỉ attendance cache
     */
    void invalidateAttendanceCache();

    /**
     * Invalidate chỉ break cache
     */
    void invalidateBreakCache();

    /**
     * Invalidate chỉ payroll cache
     */
    void invalidatePayrollCache();

    /**
     * Invalidate chỉ overtime cache
     */
    void invalidateOvertimeCache();
}
