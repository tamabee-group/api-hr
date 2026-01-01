package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.AssignScheduleRequest;
import com.tamabee.api_hr.dto.request.CreateWorkScheduleRequest;
import com.tamabee.api_hr.dto.request.UpdateWorkScheduleRequest;
import com.tamabee.api_hr.dto.response.WorkScheduleAssignmentResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IWorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý lịch làm việc của công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/schedules")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class WorkScheduleController {

    private final IWorkScheduleService workScheduleService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách lịch làm việc của công ty (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<WorkScheduleResponse>>> getSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long companyId = getCurrentUserCompanyId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkScheduleResponse> schedules = workScheduleService.getSchedules(companyId, pageable);
        return ResponseEntity.ok(BaseResponse.success(schedules, "Lấy danh sách lịch làm việc thành công"));
    }

    /**
     * Lấy thông tin chi tiết lịch làm việc theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<WorkScheduleResponse>> getScheduleById(@PathVariable Long id) {
        WorkScheduleResponse schedule = workScheduleService.getScheduleById(id);
        return ResponseEntity.ok(BaseResponse.success(schedule, "Lấy thông tin lịch làm việc thành công"));
    }

    /**
     * Tạo lịch làm việc mới
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<WorkScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateWorkScheduleRequest request) {
        Long companyId = getCurrentUserCompanyId();
        WorkScheduleResponse schedule = workScheduleService.createSchedule(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(schedule, "Tạo lịch làm việc thành công"));
    }

    /**
     * Cập nhật lịch làm việc
     */
    @PutMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<WorkScheduleResponse>> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkScheduleRequest request) {
        WorkScheduleResponse schedule = workScheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(BaseResponse.success(schedule, "Cập nhật lịch làm việc thành công"));
    }

    /**
     * Xóa lịch làm việc (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<Void>> deleteSchedule(@PathVariable Long id) {
        workScheduleService.deleteSchedule(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa lịch làm việc thành công"));
    }

    /**
     * Gán lịch làm việc cho nhiều nhân viên
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<List<WorkScheduleAssignmentResponse>>> assignScheduleToEmployees(
            @PathVariable Long id,
            @Valid @RequestBody AssignScheduleRequest request) {
        List<WorkScheduleAssignmentResponse> assignments = workScheduleService.assignScheduleToEmployees(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(assignments, "Gán lịch làm việc cho nhân viên thành công"));
    }

    /**
     * Lấy danh sách assignment của một lịch làm việc (phân trang)
     */
    @GetMapping("/{id}/assignments")
    public ResponseEntity<BaseResponse<Page<WorkScheduleAssignmentResponse>>> getAssignmentsBySchedule(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkScheduleAssignmentResponse> assignments = workScheduleService.getAssignmentsBySchedule(id, pageable);
        return ResponseEntity.ok(BaseResponse.success(assignments, "Lấy danh sách assignment thành công"));
    }

    /**
     * Lấy companyId của user đang đăng nhập
     */
    private Long getCurrentUserCompanyId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getCompanyId();
    }
}
