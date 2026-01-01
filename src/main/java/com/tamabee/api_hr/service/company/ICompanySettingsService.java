package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.request.*;
import com.tamabee.api_hr.dto.response.CompanySettingsResponse;

/**
 * Service quản lý cấu hình chấm công và tính lương của công ty.
 * Mỗi công ty có 1 bộ settings duy nhất với các config được lưu dưới dạng JSON.
 */
public interface ICompanySettingsService {

    /**
     * Lấy toàn bộ settings của công ty
     *
     * @param companyId ID công ty
     * @return settings của công ty
     */
    CompanySettingsResponse getSettings(Long companyId);

    /**
     * Cập nhật cấu hình chấm công
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    AttendanceConfig updateAttendanceConfig(Long companyId, AttendanceConfigRequest request);

    /**
     * Cập nhật cấu hình tính lương
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    PayrollConfig updatePayrollConfig(Long companyId, PayrollConfigRequest request);

    /**
     * Cập nhật cấu hình tăng ca
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    OvertimeConfig updateOvertimeConfig(Long companyId, OvertimeConfigRequest request);

    /**
     * Cập nhật cấu hình phụ cấp
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    AllowanceConfig updateAllowanceConfig(Long companyId, AllowanceConfigRequest request);

    /**
     * Cập nhật cấu hình khấu trừ
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    DeductionConfig updateDeductionConfig(Long companyId, DeductionConfigRequest request);

    /**
     * Khởi tạo settings mặc định cho công ty mới.
     * Được gọi khi tạo công ty mới.
     *
     * @param companyId ID công ty
     */
    void initializeDefaultSettings(Long companyId);

    /**
     * Lấy cấu hình chấm công của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình chấm công
     */
    AttendanceConfig getAttendanceConfig(Long companyId);

    /**
     * Lấy cấu hình tính lương của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình tính lương
     */
    PayrollConfig getPayrollConfig(Long companyId);

    /**
     * Lấy cấu hình tăng ca của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình tăng ca
     */
    OvertimeConfig getOvertimeConfig(Long companyId);

    /**
     * Lấy cấu hình phụ cấp của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình phụ cấp
     */
    AllowanceConfig getAllowanceConfig(Long companyId);

    /**
     * Lấy cấu hình khấu trừ của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình khấu trừ
     */
    DeductionConfig getDeductionConfig(Long companyId);

    /**
     * Lấy cấu hình giờ giải lao của công ty
     *
     * @param companyId ID công ty
     * @return cấu hình giờ giải lao
     */
    BreakConfig getBreakConfig(Long companyId);

    /**
     * Cập nhật cấu hình giờ giải lao
     *
     * @param companyId ID công ty
     * @param request   cấu hình mới
     * @return cấu hình đã cập nhật
     */
    BreakConfig updateBreakConfig(Long companyId, BreakConfigRequest request);
}
