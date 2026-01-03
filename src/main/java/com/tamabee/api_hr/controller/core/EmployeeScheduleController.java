package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.EmployeeSwapRequest;
import com.tamabee.api_hr.dto.response.EmployeeScheduleDataResponse;
import com.tamabee.api_hr.dto.response.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.ShiftSwapRequestResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.core.IAuthService;
import com.tamabee.api_hr.service.core.IEmployeeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho employee xem lịch làm việc của mình
 */
@RestController
@RequestMapping("/api/employee/schedule")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EmployeeScheduleController {

    private final IEmployeeScheduleService employeeScheduleService;
    private final IAuthService authService;

    /**
     * Lấy tất cả dữ liệu lịch làm việc (ca + lịch sử đổi ca) trong 1 API call
     */
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<EmployeeScheduleDataResponse>> getAllScheduleData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long employeeId = authService.getCurrentUser().getId();
        EmployeeScheduleDataResponse data = employeeScheduleService.getAllScheduleData(
                employeeId, startDate, endDate);
        return ResponseEntity.ok(BaseResponse.success(data));
    }

    /**
     * Lấy lịch làm việc của nhân viên trong khoảng thời gian
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<ShiftAssignmentResponse>>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long employeeId = authService.getCurrentUser().getId();
        List<ShiftAssignmentResponse> schedule = employeeScheduleService.getMySchedule(
                employeeId, startDate, endDate);
        return ResponseEntity.ok(BaseResponse.success(schedule));
    }

    /**
     * Lấy lịch sử đổi ca của nhân viên
     */
    @GetMapping("/swap-history")
    public ResponseEntity<BaseResponse<List<ShiftSwapRequestResponse>>> getSwapHistory() {
        Long employeeId = authService.getCurrentUser().getId();
        List<ShiftSwapRequestResponse> history = employeeScheduleService.getSwapHistory(employeeId);
        return ResponseEntity.ok(BaseResponse.success(history));
    }

    /**
     * Lấy danh sách ca có thể đổi từ nhân viên khác
     */
    @GetMapping("/available-swaps")
    public ResponseEntity<BaseResponse<List<ShiftAssignmentResponse>>> getAvailableShiftsForSwap(
            @RequestParam Long myShiftId) {
        Long employeeId = authService.getCurrentUser().getId();
        List<ShiftAssignmentResponse> availableShifts = employeeScheduleService.getAvailableShiftsForSwap(
                employeeId, myShiftId);
        return ResponseEntity.ok(BaseResponse.success(availableShifts));
    }

    /**
     * Tạo yêu cầu đổi ca
     */
    @PostMapping("/swap")
    public ResponseEntity<BaseResponse<ShiftSwapRequestResponse>> createSwapRequest(
            @Valid @RequestBody EmployeeSwapRequest request) {
        Long employeeId = authService.getCurrentUser().getId();
        ShiftSwapRequestResponse response = employeeScheduleService.createSwapRequest(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo yêu cầu đổi ca thành công"));
    }
}
