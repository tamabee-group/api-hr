package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.SelectScheduleRequest;
import com.tamabee.api_hr.dto.response.ScheduleSelectionResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.SelectionStatus;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IScheduleSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho nhân viên chọn lịch làm việc.
 * EMPLOYEE_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'EMPLOYEE_COMPANY', 'ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE')")
public class EmployeeScheduleSelectionController {

        private final IScheduleSelectionService scheduleSelectionService;
        private final UserRepository userRepository;

        /**
         * Nhân viên chọn lịch làm việc
         * POST /api/employee/schedule-selections
         */
        @PostMapping("/schedule-selections")
        public ResponseEntity<BaseResponse<ScheduleSelectionResponse>> selectSchedule(
                        @Valid @RequestBody SelectScheduleRequest request) {
                UserEntity currentUser = getCurrentUser();
                ScheduleSelectionResponse selection = scheduleSelectionService.selectSchedule(
                                currentUser.getId(),
                                request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.created(selection, "Gửi yêu cầu chọn lịch làm việc thành công"));
        }

        /**
         * Lấy lịch làm việc hiện tại của nhân viên
         * GET /api/employee/schedule-selections/current
         */
        @GetMapping("/schedule-selections/current")
        public ResponseEntity<BaseResponse<ScheduleSelectionResponse>> getCurrentSchedule() {
                UserEntity currentUser = getCurrentUser();
                List<ScheduleSelectionResponse> history = scheduleSelectionService
                                .getEmployeeSelectionHistory(currentUser.getId());

                // Tìm lịch đang có hiệu lực (APPROVED và trong khoảng thời gian)
                LocalDate today = LocalDate.now();
                ScheduleSelectionResponse current = history.stream()
                                .filter(s -> SelectionStatus.APPROVED.equals(s.getStatus()))
                                .filter(s -> !s.getEffectiveFrom().isAfter(today))
                                .filter(s -> s.getEffectiveTo() == null || !s.getEffectiveTo().isBefore(today))
                                .findFirst()
                                .orElse(null);

                return ResponseEntity.ok(BaseResponse.success(current, "Lấy lịch làm việc hiện tại thành công"));
        }

        /**
         * Lấy danh sách lịch làm việc sắp tới của nhân viên
         * GET /api/employee/schedule-selections/upcoming
         */
        @GetMapping("/schedule-selections/upcoming")
        public ResponseEntity<BaseResponse<List<ScheduleSelectionResponse>>> getUpcomingSchedules() {
                UserEntity currentUser = getCurrentUser();
                List<ScheduleSelectionResponse> history = scheduleSelectionService
                                .getEmployeeSelectionHistory(currentUser.getId());

                // Lọc các lịch sắp tới (APPROVED và effectiveFrom > today) hoặc PENDING
                LocalDate today = LocalDate.now();
                List<ScheduleSelectionResponse> upcoming = history.stream()
                                .filter(s -> SelectionStatus.PENDING.equals(s.getStatus()) ||
                                                (SelectionStatus.APPROVED.equals(s.getStatus())
                                                                && s.getEffectiveFrom().isAfter(today)))
                                .toList();

                return ResponseEntity.ok(BaseResponse.success(upcoming, "Lấy danh sách lịch sắp tới thành công"));
        }

        /**
         * Lấy danh sách lịch làm việc có thể chọn
         * GET /api/employee/schedule-selections/available
         */
        @GetMapping("/schedule-selections/available")
        public ResponseEntity<BaseResponse<List<WorkScheduleResponse>>> getAvailableSchedules() {
                List<WorkScheduleResponse> available = scheduleSelectionService.getAvailableSchedules(
                                LocalDate.now());
                return ResponseEntity.ok(BaseResponse.success(available, "Lấy danh sách lịch có sẵn thành công"));
        }

        /**
         * Lấy danh sách lịch gợi ý cho nhân viên
         * GET /api/employee/schedule-selections/suggested
         */
        @GetMapping("/schedule-selections/suggested")
        public ResponseEntity<BaseResponse<List<WorkScheduleResponse>>> getSuggestedSchedules() {
                UserEntity currentUser = getCurrentUser();
                List<WorkScheduleResponse> suggestions = scheduleSelectionService.getSuggestedSchedules(
                                currentUser.getId());
                return ResponseEntity.ok(BaseResponse.success(suggestions, "Lấy danh sách lịch gợi ý thành công"));
        }

        /**
         * Lấy lịch sử chọn lịch của nhân viên
         * GET /api/employee/schedule-selections/history
         */
        @GetMapping("/schedule-selections/history")
        public ResponseEntity<BaseResponse<List<ScheduleSelectionResponse>>> getSelectionHistory() {
                UserEntity currentUser = getCurrentUser();
                List<ScheduleSelectionResponse> history = scheduleSelectionService
                                .getEmployeeSelectionHistory(currentUser.getId());
                return ResponseEntity.ok(BaseResponse.success(history, "Lấy lịch sử chọn lịch thành công"));
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
