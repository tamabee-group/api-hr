package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.attendance.*;
import com.tamabee.api_hr.dto.response.attendance.BatchAssignmentResult;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftTemplateResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import com.tamabee.api_hr.enums.SwapRequestStatus;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller quản lý ca làm việc cho admin/manager công ty.
 * Bao gồm CRUD shift template, phân ca, và đổi ca.
 */
@RestController
@RequestMapping("/api/company/shifts")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class ShiftController {

    private final IShiftService shiftService;
    private final UserRepository userRepository;

    // ==================== Shift Template Endpoints ====================

    /**
     * Lấy danh sách mẫu ca làm việc của công ty
     * GET /api/company/shifts/templates
     */
    @GetMapping("/templates")
    public ResponseEntity<BaseResponse<Page<ShiftTemplateResponse>>> getShiftTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<ShiftTemplateResponse> templates = shiftService.getShiftTemplates(pageable);
        return ResponseEntity.ok(BaseResponse.success(templates, "Lấy danh sách mẫu ca làm việc thành công"));
    }

    /**
     * Lấy tất cả mẫu ca làm việc của công ty (không phân trang)
     * GET /api/company/shifts/templates/all
     */
    @GetMapping("/templates/all")
    public ResponseEntity<BaseResponse<Page<ShiftTemplateResponse>>> getAllShiftTemplates() {
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "name"));
        Page<ShiftTemplateResponse> templates = shiftService.getShiftTemplates(pageable);
        return ResponseEntity.ok(BaseResponse.success(templates, "Lấy danh sách mẫu ca làm việc thành công"));
    }

    /**
     * Tạo mẫu ca làm việc mới
     * POST /api/company/shifts/templates
     */
    @PostMapping("/templates")
    public ResponseEntity<BaseResponse<ShiftTemplateResponse>> createShiftTemplate(
            @Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse response = shiftService.createShiftTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo mẫu ca làm việc thành công"));
    }

    /**
     * Cập nhật mẫu ca làm việc
     * PUT /api/company/shifts/templates/{id}
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<BaseResponse<ShiftTemplateResponse>> updateShiftTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse response = shiftService.updateShiftTemplate(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật mẫu ca làm việc thành công"));
    }

    /**
     * Xóa mẫu ca làm việc
     * DELETE /api/company/shifts/templates/{id}
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteShiftTemplate(@PathVariable Long id) {
        shiftService.deleteShiftTemplate(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa mẫu ca làm việc thành công"));
    }

    // ==================== Shift Assignment Endpoints ====================

    /**
     * Lấy danh sách phân ca với filter
     * GET /api/company/shifts/assignments
     */
    @GetMapping("/assignments")
    public ResponseEntity<BaseResponse<Page<ShiftAssignmentResponse>>> getShiftAssignments(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long shiftTemplateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDateTo,
            @RequestParam(required = false) ShiftAssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ShiftAssignmentQuery query = new ShiftAssignmentQuery();
        query.setEmployeeId(employeeId);
        query.setShiftTemplateId(shiftTemplateId);
        query.setWorkDateFrom(workDateFrom);
        query.setWorkDateTo(workDateTo);
        query.setStatus(status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<ShiftAssignmentResponse> assignments = shiftService.getShiftAssignments(query, pageable);
        return ResponseEntity.ok(BaseResponse.success(assignments, "Lấy danh sách phân ca thành công"));
    }

    /**
     * Phân ca cho nhân viên
     * POST /api/company/shifts/assignments
     */
    @PostMapping("/assignments")
    public ResponseEntity<BaseResponse<ShiftAssignmentResponse>> assignShift(
            @Valid @RequestBody ShiftAssignmentRequest request) {
        ShiftAssignmentResponse response = shiftService.assignShift(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Phân ca thành công"));
    }

    /**
     * Phân ca cho nhiều nhân viên cùng lúc
     * POST /api/company/shifts/assignments/batch
     */
    @PostMapping("/assignments/batch")
    public ResponseEntity<BaseResponse<BatchAssignmentResult>> batchAssignShift(
            @Valid @RequestBody BatchShiftAssignmentRequest request) {
        BatchAssignmentResult result = shiftService.batchAssignShift(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(result, "Phân ca hàng loạt thành công"));
    }

    /**
     * Hủy phân ca
     * DELETE /api/company/shifts/assignments/{id}
     */
    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<BaseResponse<Void>> unassignShift(@PathVariable Long id) {
        shiftService.unassignShift(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Hủy phân ca thành công"));
    }

    // ==================== Shift Swap Endpoints ====================

    /**
     * Lấy danh sách yêu cầu đổi ca với filter
     * GET /api/company/shifts/swaps
     */
    @GetMapping("/swaps")
    public ResponseEntity<BaseResponse<Page<ShiftSwapRequestResponse>>> getSwapRequests(
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long targetEmployeeId,
            @RequestParam(required = false) SwapRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SwapRequestQuery query = new SwapRequestQuery();
        query.setRequesterId(requesterId);
        query.setTargetEmployeeId(targetEmployeeId);
        query.setStatus(status);
        query.setCreatedFrom(createdFrom);
        query.setCreatedTo(createdTo);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ShiftSwapRequestResponse> swapRequests = shiftService.getSwapRequests(query, pageable);
        return ResponseEntity.ok(BaseResponse.success(swapRequests, "Lấy danh sách yêu cầu đổi ca thành công"));
    }

    /**
     * Tạo yêu cầu đổi ca
     * POST /api/company/shifts/swaps
     */
    @PostMapping("/swaps")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<ShiftSwapRequestResponse>> requestSwap(
            @Valid @RequestBody ShiftSwapRequest request) {
        Long employeeId = getCurrentUserId();
        ShiftSwapRequestResponse response = shiftService.requestSwap(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo yêu cầu đổi ca thành công"));
    }

    /**
     * Duyệt yêu cầu đổi ca
     * POST /api/company/shifts/swaps/{id}/approve
     */
    @PostMapping("/swaps/{id}/approve")
    public ResponseEntity<BaseResponse<ShiftSwapRequestResponse>> approveSwap(@PathVariable Long id) {
        Long approverId = getCurrentUserId();
        ShiftSwapRequestResponse response = shiftService.approveSwap(id, approverId);
        return ResponseEntity.ok(BaseResponse.success(response, "Duyệt yêu cầu đổi ca thành công"));
    }

    /**
     * Từ chối yêu cầu đổi ca
     * POST /api/company/shifts/swaps/{id}/reject
     */
    @PostMapping("/swaps/{id}/reject")
    public ResponseEntity<BaseResponse<ShiftSwapRequestResponse>> rejectSwap(
            @PathVariable Long id,
            @RequestParam String reason) {
        Long approverId = getCurrentUserId();
        ShiftSwapRequestResponse response = shiftService.rejectSwap(id, approverId, reason);
        return ResponseEntity.ok(BaseResponse.success(response, "Từ chối yêu cầu đổi ca thành công"));
    }

    /**
     * Lấy ID của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getId();
    }
}
