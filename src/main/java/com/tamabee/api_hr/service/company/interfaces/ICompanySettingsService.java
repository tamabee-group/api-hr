package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.attendance.AttendanceConfigRequest;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollConfigRequest;
import com.tamabee.api_hr.dto.response.company.CompanySettingsResponse;

/**
 * Service quản lý cấu hình chấm công và tính lương của công ty.
 */
public interface ICompanySettingsService {

    /**
     * Lấy toàn bộ settings của tenant hiện tại
     */
    CompanySettingsResponse getSettings();

    /**
     * Cập nhật cấu hình chấm công
     */
    AttendanceConfig updateAttendanceConfig(AttendanceConfigRequest request);

    /**
     * Cập nhật cấu hình tính lương
     */
    PayrollConfig updatePayrollConfig(PayrollConfigRequest request);

    /**
     * Cập nhật cấu hình tăng ca
     */
    OvertimeConfig updateOvertimeConfig(OvertimeConfigRequest request);

    /**
     * Khởi tạo settings mặc định cho tenant mới
     */
    void initializeDefaultSettings();

    /**
     * Lấy cấu hình chấm công
     */
    AttendanceConfig getAttendanceConfig();

    /**
     * Lấy cấu hình tính lương
     */
    PayrollConfig getPayrollConfig();

    /**
     * Lấy cấu hình tăng ca
     */
    OvertimeConfig getOvertimeConfig();

    /**
     * Lấy cấu hình giờ giải lao
     */
    BreakConfig getBreakConfig();

    /**
     * Cập nhật cấu hình giờ giải lao
     */
    BreakConfig updateBreakConfig(BreakConfigRequest request);
}
