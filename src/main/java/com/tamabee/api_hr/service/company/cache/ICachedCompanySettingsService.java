package com.tamabee.api_hr.service.company.cache;

import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;

/**
 * Service cung cấp company settings với caching và fallback to defaults.
 * Sử dụng request-scoped cache để tránh truy vấn database nhiều lần trong cùng request.
 * Tự động sử dụng default values khi config bị thiếu và log warning.
 */
public interface ICachedCompanySettingsService {

    /**
     * Lấy AttendanceConfig với caching và fallback to defaults
     */
    AttendanceConfig getAttendanceConfig();

    /**
     * Lấy PayrollConfig với caching và fallback to defaults
     */
    PayrollConfig getPayrollConfig();

    /**
     * Lấy OvertimeConfig với caching và fallback to defaults
     */
    OvertimeConfig getOvertimeConfig();

    /**
     * Lấy AllowanceConfig với caching và fallback to defaults
     */
    AllowanceConfig getAllowanceConfig();

    /**
     * Lấy DeductionConfig với caching và fallback to defaults
     */
    DeductionConfig getDeductionConfig();

    /**
     * Lấy BreakConfig với caching và fallback to defaults
     */
    BreakConfig getBreakConfig();

    /**
     * Invalidate cache (gọi khi settings được cập nhật)
     */
    void invalidateCache();
}
