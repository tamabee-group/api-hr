package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.leave.CreateHolidayRequest;
import com.tamabee.api_hr.dto.request.leave.UpdateHolidayRequest;
import com.tamabee.api_hr.dto.response.leave.HolidayResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IHolidayService;
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
import java.util.List;

/**
 * Controller quản lý ngày nghỉ lễ của công ty.
 * ADMIN_COMPANY có quyền CRUD ngày nghỉ.
 */
@RestController
@RequestMapping("/api/company/holidays")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
public class HolidayController {

    private final IHolidayService holidayService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách ngày nghỉ của công ty (phân trang, filter theo năm)
     * GET /api/company/holidays?year=2026
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<HolidayResponse>>> getHolidays(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Integer year) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "date"));
        Page<HolidayResponse> holidays = holidayService.getHolidays(year, pageable);
        return ResponseEntity.ok(BaseResponse.success(holidays, "Lấy danh sách ngày nghỉ thành công"));
    }

    /**
     * Lấy chi tiết ngày nghỉ theo ID
     * GET /api/company/holidays/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<HolidayResponse>> getHolidayById(@PathVariable Long id) {
        HolidayResponse holiday = holidayService.getHolidayById(id);
        return ResponseEntity.ok(BaseResponse.success(holiday, "Lấy thông tin ngày nghỉ thành công"));
    }

    /**
     * Tạo ngày nghỉ mới
     * POST /api/company/holidays
     */
    @PostMapping
    public ResponseEntity<BaseResponse<HolidayResponse>> createHoliday(
            @Valid @RequestBody CreateHolidayRequest request) {
        HolidayResponse holiday = holidayService.createHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(holiday, "Tạo ngày nghỉ thành công"));
    }

    /**
     * Cập nhật ngày nghỉ
     * PUT /api/company/holidays/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<HolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHolidayRequest request) {
        HolidayResponse holiday = holidayService.updateHoliday(id, request);
        return ResponseEntity.ok(BaseResponse.success(holiday, "Cập nhật ngày nghỉ thành công"));
    }

    /**
     * Xóa ngày nghỉ
     * DELETE /api/company/holidays/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa ngày nghỉ thành công"));
    }

    /**
     * Lấy danh sách ngày nghỉ theo khoảng thời gian
     * GET /api/company/holidays/range?startDate=2025-01-01&endDate=2025-12-31
     * Tất cả nhân viên công ty có quyền xem
     */
    @GetMapping("/range")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<HolidayResponse>>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<HolidayResponse> holidays = holidayService.getHolidaysByDateRange(startDate, endDate);
        return ResponseEntity
                .ok(BaseResponse.success(holidays, "Lấy danh sách ngày nghỉ theo khoảng thời gian thành công"));
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
