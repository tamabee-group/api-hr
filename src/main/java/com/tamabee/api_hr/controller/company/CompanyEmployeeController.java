package com.tamabee.api_hr.controller.company;

import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.leave.UpdateLeaveBalanceRequest;
import com.tamabee.api_hr.dto.request.user.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.user.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceSummaryResponse;
import com.tamabee.api_hr.dto.response.department.DefaultApproverResponse;
import com.tamabee.api_hr.dto.response.employee.EmployeeDocumentResponse;
import com.tamabee.api_hr.dto.response.employee.EmployeePersonalInfoResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveBalanceResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveRequestResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.user.ApproverResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;
import com.tamabee.api_hr.service.company.interfaces.ICompanyEmployeeService;
import com.tamabee.api_hr.service.company.interfaces.IDepartmentService;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeDocumentService;
import com.tamabee.api_hr.service.company.interfaces.ILeaveService;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;
import com.tamabee.api_hr.service.core.interfaces.IEmailVerificationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

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
    private final IDepartmentService departmentService;
    private final IAttendanceService attendanceService;
    private final IPayrollPeriodService payrollPeriodService;
    private final ILeaveService leaveService;
    private final IEmailVerificationService emailVerificationService;
    private final IEmployeeDocumentService employeeDocumentService;

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
     * Lấy chi tiết chấm công của nhân viên theo ngày
     * GET /api/company/employees/{id}/attendance/date/{date}
     */
    @GetMapping("/{id}/attendance/date/{date}")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getEmployeeAttendanceByDate(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        AttendanceRecordResponse record = attendanceService.getAttendanceByEmployeeAndDate(id, date);
        return ResponseEntity.ok(BaseResponse.success(record, "Lấy thông tin chấm công thành công"));
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
     * Lấy người duyệt mặc định cho nhân viên (department manager)
     * GET /api/company/employees/{id}/default-approver
     */
    @GetMapping("/{id}/default-approver")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<DefaultApproverResponse>> getDefaultApprover(@PathVariable Long id) {
        DefaultApproverResponse approver = departmentService.getDefaultApprover(id);
        return ResponseEntity.ok(BaseResponse.success(approver, "Lấy người duyệt mặc định thành công"));
    }

    /**
     * Lấy lịch sử bảng lương của nhân viên (phân trang)
     * GET /api/company/employees/{id}/payroll
     */
    @GetMapping("/{id}/payroll")
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getEmployeePayrollHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<PayrollItemResponse> payrollHistory = payrollPeriodService.getEmployeePayslips(id, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(payrollHistory, "Lấy lịch sử bảng lương thành công"));
    }

    /**
     * Gửi mã xác thực email cho nhân viên trong tenant
     * POST /api/company/employees/send-verification
     * Dùng cho: đổi email, xác thực email mới của nhân viên
     */
    @PostMapping("/send-verification")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Void>> sendVerification(
            @RequestParam @NotBlank @Email String email,
            @RequestParam(defaultValue = "vi") String language) {
        // Kiểm tra email chưa tồn tại trong tenant
        companyEmployeeService.validateEmailNotExists(email);
        emailVerificationService.sendVerificationCode(email, "", language);
        return ResponseEntity.ok(BaseResponse.success(null, "Mã xác thực đã được gửi"));
    }

    /**
     * Xác thực mã OTP cho nhân viên
     * POST /api/company/employees/verify-email
     */
    @PostMapping("/verify-email")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Boolean>> verifyEmail(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String code) {
        boolean verified = emailVerificationService.verifyCode(email, code);
        String message = verified ? "Xác thực email thành công" : "Mã xác thực không hợp lệ";
        return ResponseEntity.ok(BaseResponse.success(verified, message));
    }

    /**
     * Lấy personal info đầy đủ của nhân viên
     * GET /api/company/employees/{id}/personal-info
     */
    @GetMapping("/{id}/personal-info")
    public ResponseEntity<BaseResponse<EmployeePersonalInfoResponse>> getEmployeePersonalInfo(@PathVariable Long id) {
        EmployeePersonalInfoResponse personalInfo = companyEmployeeService.getEmployeePersonalInfo(id);
        return ResponseEntity.ok(BaseResponse.success(personalInfo, "Lấy thông tin cá nhân thành công"));
    }

    /**
     * Lấy danh sách documents của nhân viên (phân trang)
     * GET /api/company/employees/{id}/documents
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<BaseResponse<Page<EmployeeDocumentResponse>>> getEmployeeDocuments(
            @PathVariable Long id,
            Pageable pageable) {
        Page<EmployeeDocumentResponse> documents = employeeDocumentService.getEmployeeDocuments(id, pageable);
        return ResponseEntity.ok(BaseResponse.success(documents, "Lấy danh sách tài liệu thành công"));
    }

    /**
     * Upload document mới cho nhân viên
     * POST /api/company/employees/{id}/documents
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<BaseResponse<EmployeeDocumentResponse>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "OTHER") String documentType) {
        EmployeeDocumentResponse document = employeeDocumentService.uploadDocument(id, file, documentType);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created(document, "Tải tài liệu thành công"));
    }

    /**
     * Xóa document của nhân viên
     * DELETE /api/company/employees/{id}/documents/{docId}
     */
    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<BaseResponse<Void>> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long docId) {
        employeeDocumentService.deleteDocument(id, docId);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa tài liệu thành công"));
    }

    /**
     * Lấy danh sách yêu cầu nghỉ phép của nhân viên (phân trang)
     * GET /api/company/employees/{id}/leave-requests
     */
    @GetMapping("/{id}/leave-requests")
    public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> getEmployeeLeaveRequests(
            @PathVariable Long id,
            Pageable pageable) {
        Page<LeaveRequestResponse> leaveRequests = leaveService.getEmployeeLeaveRequests(id, pageable);
        return ResponseEntity.ok(BaseResponse.success(leaveRequests, "Lấy danh sách yêu cầu nghỉ phép thành công"));
    }

    /**
     * Lấy số ngày phép còn lại của nhân viên
     * GET /api/company/employees/{id}/leave-balance
     */
    @GetMapping("/{id}/leave-balance")
    public ResponseEntity<BaseResponse<List<LeaveBalanceResponse>>> getEmployeeLeaveBalance(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        Integer effectiveYear = year != null ? year : java.time.Year.now().getValue();
        List<LeaveBalanceResponse> leaveBalance = leaveService.getLeaveBalance(id, effectiveYear);
        return ResponseEntity.ok(BaseResponse.success(leaveBalance, "Lấy số ngày phép còn lại thành công"));
    }

    /**
     * Cập nhật số ngày phép cho nhân viên
     * PUT /api/company/employees/{id}/leave-balance
     */
    @PutMapping("/{id}/leave-balance")
    public ResponseEntity<BaseResponse<LeaveBalanceResponse>> updateEmployeeLeaveBalance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeaveBalanceRequest request) {
        LeaveBalanceResponse leaveBalance = leaveService.updateEmployeeLeaveBalance(id, request);
        return ResponseEntity.ok(BaseResponse.success(leaveBalance, "Cập nhật số ngày phép thành công"));
    }

    /**
     * Xóa nhân viên vĩnh viễn (hard delete)
     * DELETE /api/company/employees/{id}
     * Chỉ Admin có quyền thực hiện
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> deleteEmployee(@PathVariable Long id) {
        companyEmployeeService.deleteEmployee(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa nhân viên thành công"));
    }
}
