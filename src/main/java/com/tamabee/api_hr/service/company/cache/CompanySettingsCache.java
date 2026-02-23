package com.tamabee.api_hr.service.company.cache;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;
import com.tamabee.api_hr.entity.company.BreakSettingEntity;
import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;
import com.tamabee.api_hr.entity.company.PayrollSettingEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache company settings trong phạm vi một HTTP request.
 * Cache từng setting entity riêng biệt để tránh truy vấn database nhiều lần.
 * 
 * Sử dụng @RequestScope để mỗi request có một instance cache riêng,
 * tự động được dọn dẹp khi request kết thúc.
 */
@Slf4j
@Component
@RequestScope
public class CompanySettingsCache {

    // Cache từng setting entity riêng biệt
    private AttendanceSettingEntity cachedAttendanceSetting;
    private BreakSettingEntity cachedBreakSetting;
    private PayrollSettingEntity cachedPayrollSetting;
    private OvertimeSettingEntity cachedOvertimeSetting;

    // Cờ đánh dấu đã query từng loại (kể cả khi không tìm thấy)
    private boolean attendanceQueried = false;
    private boolean breakQueried = false;
    private boolean payrollQueried = false;
    private boolean overtimeQueried = false;

    // ==================== Attendance ====================

    public AttendanceSettingEntity getAttendanceSetting() {
        return cachedAttendanceSetting;
    }

    public void putAttendanceSetting(AttendanceSettingEntity entity) {
        this.cachedAttendanceSetting = entity;
        this.attendanceQueried = true;
        log.debug("Cached AttendanceSettingEntity");
    }

    public boolean isAttendanceQueried() {
        return attendanceQueried;
    }

    public void invalidateAttendance() {
        cachedAttendanceSetting = null;
        attendanceQueried = false;
        log.debug("Invalidated attendance settings cache");
    }

    // ==================== Break ====================

    public BreakSettingEntity getBreakSetting() {
        return cachedBreakSetting;
    }

    public void putBreakSetting(BreakSettingEntity entity) {
        this.cachedBreakSetting = entity;
        this.breakQueried = true;
        log.debug("Cached BreakSettingEntity");
    }

    public boolean isBreakQueried() {
        return breakQueried;
    }

    public void invalidateBreak() {
        cachedBreakSetting = null;
        breakQueried = false;
        log.debug("Invalidated break settings cache");
    }

    // ==================== Payroll ====================

    public PayrollSettingEntity getPayrollSetting() {
        return cachedPayrollSetting;
    }

    public void putPayrollSetting(PayrollSettingEntity entity) {
        this.cachedPayrollSetting = entity;
        this.payrollQueried = true;
        log.debug("Cached PayrollSettingEntity");
    }

    public boolean isPayrollQueried() {
        return payrollQueried;
    }

    public void invalidatePayroll() {
        cachedPayrollSetting = null;
        payrollQueried = false;
        log.debug("Invalidated payroll settings cache");
    }

    // ==================== Overtime ====================

    public OvertimeSettingEntity getOvertimeSetting() {
        return cachedOvertimeSetting;
    }

    public void putOvertimeSetting(OvertimeSettingEntity entity) {
        this.cachedOvertimeSetting = entity;
        this.overtimeQueried = true;
        log.debug("Cached OvertimeSettingEntity");
    }

    public boolean isOvertimeQueried() {
        return overtimeQueried;
    }

    public void invalidateOvertime() {
        cachedOvertimeSetting = null;
        overtimeQueried = false;
        log.debug("Invalidated overtime settings cache");
    }

    // ==================== Invalidate tất cả ====================

    /**
     * Xóa toàn bộ cache
     */
    public void invalidateAll() {
        invalidateAttendance();
        invalidateBreak();
        invalidatePayroll();
        invalidateOvertime();
        log.debug("Invalidated all company settings cache");
    }
}
