package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.ApproverResponse;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.AttendanceSummaryResponse;
import com.tamabee.api_hr.dto.response.PayrollRecordResponse;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IAttendanceService;
import com.tamabee.api_hr.service.company.ICompanyEmployeeService;
import com.tamabee.api_hr.service.company.IPayrollService;
import com.tamabee.api_hr.service.company.IWorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Controller quản lý nhân viên công ty
 * Dành cho ADMIN_COMPANY và MANAGER_COMPANY
 */
@RestController
@RequestMapping("/api/company/employees")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class CompanyEmployeeController {

    private final ICompanyEmployeeService companyEmployeeService;
    private final IWorkScheduleService workScheduleService;
    private final IAttendanceService attendanceService;
    private final IPayrollService payrollService;

    /**
     * Lấy danh sách nhân viên công ty (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<UserResponse>>> getEmployees(Pageable pageable) {
        Page<UserResponse> employees = companyEmployeeService.getCompanyEmployees(pageable);
        return ResponseEntity.ok(BaseResponse.success(employees, "Lấy danh sách nhân viên thành công"));
    }

    /**
     * Lấy thông tin chi tiết nhân viên theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> getEmployee(@PathVariable Long id) {
        UserResponse employee = companyEmployeeService.getCompanyEmployee(id);
        return ResponseEntity.ok(BaseResponse.success(employee, "Lấy thông tin nhân viên thành công"));
    }

    /**
     * Tạo nhân viên mới cho công ty
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<UserResponse>> createEmployee(
            @Valid @RequestBody CreateCompanyEmployeeRequest request) {
        UserResponse employee = companyEmployeeService.createCompanyEmployee(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created(employee, "Tạo nhân viên thành công"));
    }

    /**
     * Cập nhật thông tin nhân viên
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponse employee = companyEmployeeService.updateCompanyEmployee(id, request);
        return ResponseEntity.ok(BaseResponse.success(employee, "Cập nhật thông tin nhân viên thành công"));
    }

    /**
     * Upload avatar cho nhân viên
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<BaseResponse<String>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("avatar") MultipartFile file) {
        String avatarUrl = companyEmployeeService.uploadEmployeeAvatar(id, file);
        return ResponseEntity.ok(BaseResponse.success(avatarUrl, "Tải ảnh đại diện thành công"));
    }

    /**
     * Lấy lịch làm việc hiệu lực của nhân viên
     * 
     * @param id   ID nhân viên
     * @param date Ngày cần lấy lịch (mặc định là ngày hiện tại)
     */
    @GetMapping("/{id}/effective-schedule")
    public ResponseEntity<BaseResponse<WorkScheduleResponse>> getEffectiveSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        WorkScheduleResponse schedule = workScheduleService.getEffectiveSchedule(id, effectiveDate);
        return ResponseEntity.ok(BaseResponse.success(schedule, "Lấy lịch làm việc hiệu lực thành công"));
    }

    /**
     * Lấy tổng hợp chấm công của nhân viên
     * GET /api/company/employees/{id}/attendance/summary
     * 
     * @param id     ID nhân viên
     * @param period Kỳ lương (mặc định là tháng hiện tại)
     */
    @GetMapping("/{id}/attendance/summary")
    public ResponseEntity<BaseResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
        YearMonth effectivePeriod = period != null ? period : YearMonth.now();
        AttendanceSummaryResponse summary = attendanceService.getAttendanceSummary(id, effectivePeriod);
        return ResponseEntity.ok(BaseResponse.success(summary, "Lấy tổng hợp chấm công thành công"));
    }

    /**
     * Lấy danh sách chấm công của nhân viên theo tháng (cho calendar view)
     * GET /api/company/employees/{id}/attendance/month?year=2024&month=1
     */
    @GetMapping("/{id}/attendance/month")
    public ResponseEntity<BaseResponse<Page<AttendanceRecordResponse>>> getEmployeeAttendanceByMonth(
            @PathVariable Long id,
            @RequestParam int year,
            @RequestParam int month) {
        Page<AttendanceRecordResponse> records = attendanceService.getEmployeeAttendanceByMonth(id, year, month);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách chấm công thành công"));
    }

    /**
     * Lấy danh sách người có quyền duyệt (admin và manager)
     * Cho phép tất cả nhân viên công ty truy cập để chọn người duyệt khi tạo yêu
     * cầu
     */
    @GetMapping("/approvers")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<ApproverResponse>>> getApprovers() {
        List<ApproverResponse> approvers = companyEmployeeService.getApprovers();
        return ResponseEntity.ok(BaseResponse.success(approvers, "Lấy danh sách người duyệt thành công"));
    }

    /**
     * Lấy lịch sử bảng lương của nhân viên (phân trang)
     * GET /api/company/employees/{id}/payroll
     */
    @GetMapping("/{id}/payroll")
    public ResponseEntity<BaseResponse<Page<PayrollRecordResponse>>> getEmployeePayrollHistory(
            @PathVariable Long id,
            Pageable pageable) {
        Page<PayrollRecordResponse> payrollHistory = payrollService.getEmployeePayrollHistory(id, pageable);
        return ResponseEntity.ok(BaseResponse.success(payrollHistory, "Lấy lịch sử bảng lương thành công"));
    }
}
