package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.BreakRecordResponse;
import com.tamabee.api_hr.dto.response.BreakSummaryResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IBreakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller cho nhân viên ghi nhận giờ giải lao.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/attendance")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeeBreakController {

    private final IBreakService breakService;
    private final UserRepository userRepository;

    /**
     * Bắt đầu giờ giải lao
     * POST /api/employee/attendance/break/start
     */
    @PostMapping("/break/start")
    public ResponseEntity<BaseResponse<BreakRecordResponse>> startBreak(
            @Valid @RequestBody StartBreakRequest request) {
        UserEntity currentUser = getCurrentUser();
        BreakRecordResponse record = breakService.startBreak(
                currentUser.getId(),
                currentUser.getCompanyId(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(record, "Bắt đầu giờ giải lao thành công"));
    }

    /**
     * Kết thúc giờ giải lao
     * POST /api/employee/attendance/break/{breakRecordId}/end
     */
    @PostMapping("/break/{breakRecordId}/end")
    public ResponseEntity<BaseResponse<BreakRecordResponse>> endBreak(
            @PathVariable Long breakRecordId) {
        UserEntity currentUser = getCurrentUser();
        BreakRecordResponse record = breakService.endBreak(
                currentUser.getId(),
                breakRecordId);
        return ResponseEntity.ok(BaseResponse.success(record, "Kết thúc giờ giải lao thành công"));
    }

    /**
     * Lấy danh sách giờ giải lao theo ngày
     * GET /api/employee/attendance/{date}/breaks
     */
    @GetMapping("/{date}/breaks")
    public ResponseEntity<BaseResponse<BreakSummaryResponse>> getBreaksByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UserEntity currentUser = getCurrentUser();
        BreakSummaryResponse summary = breakService.getBreakSummary(
                currentUser.getId(),
                date);
        return ResponseEntity.ok(BaseResponse.success(summary, "Lấy thông tin giờ giải lao thành công"));
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
