package com.tamabee.api_hr.service.company.cache;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache company settings trong phạm vi một HTTP request.
 * Tránh truy vấn database nhiều lần cho cùng một companyId trong cùng request.
 * 
 * Sử dụng @RequestScope để mỗi request có một instance cache riêng,
 * tự động được dọn dẹp khi request kết thúc.
 */
@Slf4j
@Component
@RequestScope
public class CompanySettingsCache {

    // Cache entity theo companyId
    private final Map<Long, CompanySettingEntity> entityCache = new ConcurrentHashMap<>();

    // Cache các config đã deserialize
    private final Map<Long, AttendanceConfig> attendanceConfigCache = new ConcurrentHashMap<>();
    private final Map<Long, PayrollConfig> payrollConfigCache = new ConcurrentHashMap<>();
    private final Map<Long, OvertimeConfig> overtimeConfigCache = new ConcurrentHashMap<>();
    private final Map<Long, AllowanceConfig> allowanceConfigCache = new ConcurrentHashMap<>();
    private final Map<Long, DeductionConfig> deductionConfigCache = new ConcurrentHashMap<>();
    private final Map<Long, BreakConfig> breakConfigCache = new ConcurrentHashMap<>();

    // Flag để track xem entity đã được query chưa (kể cả khi không tìm thấy)
    private final Map<Long, Boolean> entityQueried = new ConcurrentHashMap<>();

    /**
     * Lấy entity từ cache
     */
    public CompanySettingEntity getEntity(Long companyId) {
        return entityCache.get(companyId);
    }

    /**
     * Lưu entity vào cache
     */
    public void putEntity(Long companyId, CompanySettingEntity entity) {
        if (entity != null) {
            entityCache.put(companyId, entity);
        }
        entityQueried.put(companyId, true);
        log.debug("Cached CompanySettingEntity for companyId: {}", companyId);
    }

    /**
     * Kiểm tra entity có trong cache không
     */
    public boolean hasEntity(Long companyId) {
        return entityCache.containsKey(companyId);
    }

    /**
     * Kiểm tra entity đã được query chưa (kể cả khi không tìm thấy)
     */
    public boolean isEntityQueried(Long companyId) {
        return entityQueried.getOrDefault(companyId, false);
    }

    /**
     * Lấy AttendanceConfig từ cache
     */
    public AttendanceConfig getAttendanceConfig(Long companyId) {
        return attendanceConfigCache.get(companyId);
    }

    /**
     * Lưu AttendanceConfig vào cache
     */
    public void putAttendanceConfig(Long companyId, AttendanceConfig config) {
        attendanceConfigCache.put(companyId, config);
    }

    /**
     * Lấy PayrollConfig từ cache
     */
    public PayrollConfig getPayrollConfig(Long companyId) {
        return payrollConfigCache.get(companyId);
    }

    /**
     * Lưu PayrollConfig vào cache
     */
    public void putPayrollConfig(Long companyId, PayrollConfig config) {
        payrollConfigCache.put(companyId, config);
    }

    /**
     * Lấy OvertimeConfig từ cache
     */
    public OvertimeConfig getOvertimeConfig(Long companyId) {
        return overtimeConfigCache.get(companyId);
    }

    /**
     * Lưu OvertimeConfig vào cache
     */
    public void putOvertimeConfig(Long companyId, OvertimeConfig config) {
        overtimeConfigCache.put(companyId, config);
    }

    /**
     * Lấy AllowanceConfig từ cache
     */
    public AllowanceConfig getAllowanceConfig(Long companyId) {
        return allowanceConfigCache.get(companyId);
    }

    /**
     * Lưu AllowanceConfig vào cache
     */
    public void putAllowanceConfig(Long companyId, AllowanceConfig config) {
        allowanceConfigCache.put(companyId, config);
    }

    /**
     * Lấy DeductionConfig từ cache
     */
    public DeductionConfig getDeductionConfig(Long companyId) {
        return deductionConfigCache.get(companyId);
    }

    /**
     * Lưu DeductionConfig vào cache
     */
    public void putDeductionConfig(Long companyId, DeductionConfig config) {
        deductionConfigCache.put(companyId, config);
    }

    /**
     * Lấy BreakConfig từ cache
     */
    public BreakConfig getBreakConfig(Long companyId) {
        return breakConfigCache.get(companyId);
    }

    /**
     * Lưu BreakConfig vào cache
     */
    public void putBreakConfig(Long companyId, BreakConfig config) {
        breakConfigCache.put(companyId, config);
    }

    /**
     * Xóa cache cho một companyId (khi settings được cập nhật)
     */
    public void invalidate(Long companyId) {
        entityCache.remove(companyId);
        entityQueried.remove(companyId);
        attendanceConfigCache.remove(companyId);
        payrollConfigCache.remove(companyId);
        overtimeConfigCache.remove(companyId);
        allowanceConfigCache.remove(companyId);
        deductionConfigCache.remove(companyId);
        breakConfigCache.remove(companyId);
        log.debug("Invalidated cache for companyId: {}", companyId);
    }

    /**
     * Xóa toàn bộ cache
     */
    public void invalidate() {
        clear();
    }

    /**
     * Xóa toàn bộ cache
     */
    public void clear() {
        entityCache.clear();
        entityQueried.clear();
        attendanceConfigCache.clear();
        payrollConfigCache.clear();
        overtimeConfigCache.clear();
        allowanceConfigCache.clear();
        deductionConfigCache.clear();
        breakConfigCache.clear();
        log.debug("Cleared all company settings cache");
    }
}
