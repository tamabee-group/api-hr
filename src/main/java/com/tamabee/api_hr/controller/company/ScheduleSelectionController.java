package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.company.RejectRequest;
import com.tamabee.api_hr.dto.response.schedule.ScheduleSelectionResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IScheduleSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý yêu cầu chọn lịch làm việc của nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền phê duyệt/từ chối.
 */
@RestController
@RequestMapping("/api/company/schedule-selections")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class ScheduleSelectionController {

    private final IScheduleSelectionService scheduleSelectionService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách yêu cầu chọn lịch đang chờ duyệt (phân trang)
     * GET /api/company/schedule-selections/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<Page<ScheduleSelectionResponse>>> getPendingSelections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScheduleSelectionResponse> selections = scheduleSelectionService.getPendingSelections(pageable);
        return ResponseEntity.ok(BaseResponse.success(selections, "Lấy danh sách yêu cầu chờ duyệt thành công"));
    }

    /**
     * Lấy chi tiết yêu cầu chọn lịch theo ID
     * GET /api/company/schedule-selections/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ScheduleSelectionResponse>> getSelectionById(@PathVariable Long id) {
        ScheduleSelectionResponse selection = scheduleSelectionService.getSelectionById(id);
        return ResponseEntity.ok(BaseResponse.success(selection, "Lấy thông tin yêu cầu chọn lịch thành công"));
    }

    /**
     * Phê duyệt yêu cầu chọn lịch
     * PUT /api/company/schedule-selections/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<ScheduleSelectionResponse>> approveSelection(@PathVariable Long id) {
        Long managerId = getCurrentUserId();
        ScheduleSelectionResponse selection = scheduleSelectionService.approveSelection(id, managerId);
        return ResponseEntity.ok(BaseResponse.success(selection, "Phê duyệt yêu cầu chọn lịch thành công"));
    }

    /**
     * Từ chối yêu cầu chọn lịch
     * PUT /api/company/schedule-selections/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<ScheduleSelectionResponse>> rejectSelection(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        Long managerId = getCurrentUserId();
        ScheduleSelectionResponse selection = scheduleSelectionService.rejectSelection(id, managerId,
                request.getRejectionReason());
        return ResponseEntity.ok(BaseResponse.success(selection, "Từ chối yêu cầu chọn lịch thành công"));
    }

    /**
     * Lấy userId của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Lấy thông tin user đang đăng nhập
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
    }
}
