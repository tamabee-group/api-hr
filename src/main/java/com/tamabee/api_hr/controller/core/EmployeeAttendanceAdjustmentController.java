package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.CreateAdjustmentRequest;
import com.tamabee.api_hr.dto.response.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IAttendanceAdjustmentService;
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

/**
 * Controller cho nhân viên tạo yêu cầu điều chỉnh chấm công.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/attendance-adjustments")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeeAttendanceAdjustmentController {

        private final IAttendanceAdjustmentService adjustmentService;
        private final UserRepository userRepository;

        /**
         * Tạo yêu cầu điều chỉnh chấm công
         * POST /api/employee/attendance-adjustments
         */
        @PostMapping
        public ResponseEntity<BaseResponse<AdjustmentRequestResponse>> createAdjustmentRequest(
                        @Valid @RequestBody CreateAdjustmentRequest request) {
                UserEntity currentUser = getCurrentUser();
                AdjustmentRequestResponse response = adjustmentService.createAdjustmentRequest(
                                currentUser.getId(),
                                request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.created(response, "Tạo yêu cầu điều chỉnh thành công"));
        }

        /**
         * Lấy danh sách yêu cầu điều chỉnh của nhân viên (phân trang)
         * GET /api/employee/attendance-adjustments
         */
        @GetMapping
        public ResponseEntity<BaseResponse<Page<AdjustmentRequestResponse>>> getMyAdjustmentRequests(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                UserEntity currentUser = getCurrentUser();
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<AdjustmentRequestResponse> requests = adjustmentService.getEmployeeRequests(
                                currentUser.getId(), pageable);
                return ResponseEntity.ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu điều chỉnh thành công"));
        }

        /**
         * Lấy chi tiết yêu cầu điều chỉnh theo ID
         * GET /api/employee/attendance-adjustments/{id}
         */
        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse<AdjustmentRequestResponse>> getAdjustmentRequestById(@PathVariable Long id) {
                AdjustmentRequestResponse request = adjustmentService.getRequestById(id);
                return ResponseEntity.ok(BaseResponse.success(request, "Lấy thông tin yêu cầu điều chỉnh thành công"));
        }

        /**
         * Lấy danh sách yêu cầu điều chỉnh theo ngày làm việc
         * GET /api/employee/attendance-adjustments/by-date/{date}
         */
        @GetMapping("/by-date/{date}")
        public ResponseEntity<BaseResponse<java.util.List<AdjustmentRequestResponse>>> getAdjustmentRequestsByDate(
                        @PathVariable String date) {
                UserEntity currentUser = getCurrentUser();
                java.time.LocalDate workDate = java.time.LocalDate.parse(date);
                java.util.List<AdjustmentRequestResponse> requests = adjustmentService.getEmployeeRequestsByWorkDate(
                                currentUser.getId(), workDate);
                return ResponseEntity.ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu điều chỉnh thành công"));
        }

        /**
         * Thu hồi yêu cầu điều chỉnh (chỉ khi đang PENDING)
         * DELETE /api/employee/attendance-adjustments/{id}
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<BaseResponse<Void>> cancelAdjustmentRequest(@PathVariable Long id) {
                UserEntity currentUser = getCurrentUser();
                adjustmentService.cancelAdjustmentRequest(id, currentUser.getId());
                return ResponseEntity.ok(BaseResponse.success(null, "Thu hồi yêu cầu điều chỉnh thành công"));
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
