package com.tamabee.api_hr.controller.company;

import java.time.Year;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.attendance.AttendanceConfigRequest;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollConfigRequest;
import com.tamabee.api_hr.dto.response.attendance.BreakConfigResponse;
import com.tamabee.api_hr.dto.response.company.CompanySettingsResponse;
import com.tamabee.api_hr.dto.response.leave.HolidayResponse;
import com.tamabee.api_hr.dto.response.payroll.OvertimeConfigResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.mapper.company.BreakConfigMapper;
import com.tamabee.api_hr.mapper.company.HolidayMapper;
import com.tamabee.api_hr.mapper.company.OvertimeConfigMapper;
import com.tamabee.api_hr.repository.leave.HolidayRepository;
import com.tamabee.api_hr.service.company.cache.ICachedCompanySettingsService;
import com.tamabee.api_hr.service.company.interfaces.ICompanySettingsService;
import com.tamabee.api_hr.service.company.interfaces.IGoogleCalendarService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý cấu hình chấm công và tính lương của công ty.
 * Chỉ ADMIN_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/settings")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
public class CompanySettingsController {

    private final ICompanySettingsService companySettingsService;
    private final ICachedCompanySettingsService cachedSettingsService;
    private final IGoogleCalendarService googleCalendarService;
    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;
    private final BreakConfigMapper breakConfigMapper;
    private final OvertimeConfigMapper overtimeConfigMapper;

    /**
     * Lấy toàn bộ cấu hình của công ty - cho phép tất cả nhân viên truy cập (read-only)
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<CompanySettingsResponse>> getSettings() {
        CompanySettingsResponse settings = companySettingsService.getSettings();
        return ResponseEntity.ok(BaseResponse.success(settings, "Lấy cấu hình công ty thành công"));
    }

    /**
     * Lấy cấu hình chấm công - cho phép tất cả nhân viên truy cập
     */
    @GetMapping("/attendance")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<AttendanceConfig>> getAttendanceConfig() {
        AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
        return ResponseEntity.ok(BaseResponse.success(config, "Lấy cấu hình chấm công thành công"));
    }

    /**
     * Cập nhật cấu hình chấm công
     */
    @PutMapping("/attendance")
    public ResponseEntity<BaseResponse<AttendanceConfig>> updateAttendanceConfig(
            @Valid @RequestBody AttendanceConfigRequest request) {
        AttendanceConfig config = companySettingsService.updateAttendanceConfig(request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình chấm công thành công"));
    }

    /**
     * Cập nhật cấu hình tính lương
     */
    @PutMapping("/payroll")
    public ResponseEntity<BaseResponse<PayrollConfig>> updatePayrollConfig(
            @Valid @RequestBody PayrollConfigRequest request) {
        PayrollConfig config = companySettingsService.updatePayrollConfig(request);
        return ResponseEntity.ok(BaseResponse.success(config, "Cập nhật cấu hình tính lương thành công"));
    }

    /**
     * Cập nhật cấu hình tăng ca
     */
    @PutMapping("/overtime")
    public ResponseEntity<BaseResponse<OvertimeConfigResponse>> updateOvertimeConfig(
            @Valid @RequestBody OvertimeConfigRequest request) {
        OvertimeConfig config = companySettingsService.updateOvertimeConfig(request);
        OvertimeConfigResponse response = overtimeConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật cấu hình tăng ca thành công"));
    }

    /**
     * Lấy cấu hình tăng ca - cho phép tất cả nhân viên truy cập
     */
    @GetMapping("/overtime")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<OvertimeConfigResponse>> getOvertimeConfig() {
        OvertimeConfig config = cachedSettingsService.getOvertimeConfig();
        OvertimeConfigResponse response = overtimeConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy cấu hình tăng ca thành công"));
    }

    /**
     * Cập nhật cấu hình giờ giải lao
     */
    @PutMapping("/break")
    public ResponseEntity<BaseResponse<Void>> updateBreakConfig(
            @Valid @RequestBody BreakConfigRequest request) {
        companySettingsService.updateBreakConfig(request);
        return ResponseEntity.ok(BaseResponse.success(null, "Cập nhật cấu hình giờ giải lao thành công"));
    }

    /**
     * Lấy cấu hình giờ giải lao - cho phép tất cả nhân viên truy cập
     */
    @GetMapping("/break")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<BreakConfigResponse>> getBreakConfig() {
        BreakConfig config = cachedSettingsService.getBreakConfig();
        BreakConfigResponse response = breakConfigMapper.toResponse(config);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy cấu hình giờ giải lao thành công"));
    }

    /**
     * Sync ngày lễ quốc gia từ Google Calendar API.
     * Region được tự động lấy từ company trong master DB.
     */
    @PostMapping("/holidays/sync")
    public ResponseEntity<BaseResponse<List<HolidayResponse>>> syncHolidays(
            @RequestParam Integer year) {
        List<HolidayResponse> holidays = googleCalendarService.syncHolidays(year);
        return ResponseEntity.ok(BaseResponse.success(holidays, "Sync ngày lễ thành công"));
    }

    /**
     * Lấy danh sách ngày lễ quốc gia đã sync theo năm
     */
    @GetMapping("/holidays")
    public ResponseEntity<BaseResponse<List<HolidayResponse>>> getHolidays(
            @RequestParam(defaultValue = "0") Integer year) {
        int targetYear = (year == null || year == 0) ? Year.now().getValue() : year;
        List<HolidayResponse> holidays = holidayRepository.findNationalHolidaysByYear(targetYear)
                .stream()
                .map(holidayMapper::toResponse)
                .toList();
        return ResponseEntity.ok(BaseResponse.success(holidays, "Lấy danh sách ngày lễ thành công"));
    }
}
