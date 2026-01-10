package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.payroll.AllowanceConfigRequest;
import com.tamabee.api_hr.dto.request.attendance.AttendanceConfigRequest;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.DeductionConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollConfigRequest;
import com.tamabee.api_hr.dto.request.schedule.WorkModeConfigRequest;
import com.tamabee.api_hr.dto.response.company.CompanySettingsResponse;
import com.tamabee.api_hr.dto.response.schedule.WorkModeChangeLogResponse;
import com.tamabee.api_hr.dto.response.schedule.WorkModeConfigResponse;

/**
 * Service quản lý cấu hình chấm công và tính lương của công ty.
 */
public interface ICompanySettingsService {

    /**
     * Lấy toàn bộ settings của tenant hiện tại
     */
    CompanySettingsResponse getSettings();

    /**
     * Lấy cấu hình work mode
     */
    WorkModeConfigResponse getWorkModeConfig();

    /**
     * Cập nhật cấu hình work mode
     */
    WorkModeConfigResponse updateWorkModeConfig(WorkModeConfigRequest request, String changedBy);

    /**
     * Lấy lịch sử thay đổi work mode
     */
    List<WorkModeChangeLogResponse> getWorkModeChangeLogs();

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
     * Cập nhật cấu hình phụ cấp
     */
    AllowanceConfig updateAllowanceConfig(AllowanceConfigRequest request);

    /**
     * Cập nhật cấu hình khấu trừ
     */
    DeductionConfig updateDeductionConfig(DeductionConfigRequest request);

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
     * Lấy cấu hình phụ cấp
     */
    AllowanceConfig getAllowanceConfig();

    /**
     * Lấy cấu hình khấu trừ
     */
    DeductionConfig getDeductionConfig();

    /**
     * Lấy cấu hình giờ giải lao
     */
    BreakConfig getBreakConfig();

    /**
     * Cập nhật cấu hình giờ giải lao
     */
    BreakConfig updateBreakConfig(BreakConfigRequest request);
}
