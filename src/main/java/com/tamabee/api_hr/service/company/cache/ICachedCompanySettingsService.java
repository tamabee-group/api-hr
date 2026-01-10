package com.tamabee.api_hr.service.company.cache;

import com.tamabee.api_hr.dto.config.*;

/**
 * Service cung cấp company settings với caching và fallback to defaults.
 * Sử dụng request-scoped cache để tránh truy vấn database nhiều lần trong cùng
 * request.
 * Tự động sử dụng default values khi config bị thiếu và log warning.
 */
public interface ICachedCompanySettingsService {

    /**
     * Lấy AttendanceConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return AttendanceConfig (không bao giờ null)
     */
    AttendanceConfig getAttendanceConfig(Long companyId);

    /**
     * Lấy PayrollConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return PayrollConfig (không bao giờ null)
     */
    PayrollConfig getPayrollConfig(Long companyId);

    /**
     * Lấy OvertimeConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return OvertimeConfig (không bao giờ null)
     */
    OvertimeConfig getOvertimeConfig(Long companyId);

    /**
     * Lấy AllowanceConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return AllowanceConfig (không bao giờ null)
     */
    AllowanceConfig getAllowanceConfig(Long companyId);

    /**
     * Lấy DeductionConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return DeductionConfig (không bao giờ null)
     */
    DeductionConfig getDeductionConfig(Long companyId);

    /**
     * Lấy BreakConfig với caching và fallback to defaults
     * 
     * @param companyId ID công ty
     * @return BreakConfig (không bao giờ null)
     */
    BreakConfig getBreakConfig(Long companyId);

    /**
     * Invalidate cache cho một companyId (gọi khi settings được cập nhật)
     * 
     * @param companyId ID công ty
     */
    void invalidateCache(Long companyId);
}
