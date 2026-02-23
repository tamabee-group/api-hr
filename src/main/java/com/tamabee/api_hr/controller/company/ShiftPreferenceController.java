package com.tamabee.api_hr.controller.company;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.attendance.ApplyPreferenceRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftPreferenceRequest;
import com.tamabee.api_hr.dto.response.attendance.ShiftPreferenceResponse;
import com.tamabee.api_hr.dto.response.attendance.SuggestionResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.interfaces.IShiftPreferenceService;
import com.tamabee.api_hr.service.core.interfaces.ISuggestionEngine;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý nguyện vọng ca làm việc.
 * Employee: gửi, cập nhật, xóa nguyện vọng.
 * Manager: xem danh sách, áp dụng nguyện vọng, lấy gợi ý phân ca.
 */
@RestController
@RequestMapping("/api/company/shift-preferences")
@RequiredArgsConstructor
public class ShiftPreferenceController {

    private final IShiftPreferenceService shiftPreferenceService;
    private final ISuggestionEngine suggestionEngine;
    private final UserRepository userRepository;

    // ==================== Manager Endpoints ====================

    /**
     * Lấy danh sách nguyện vọng theo tuần (Manager+)
     * GET /api/company/shift-preferences?year=2025&weekNumber=25
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<ShiftPreferenceResponse>>> getPreferencesByWeek(
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {
        List<ShiftPreferenceResponse> preferences = shiftPreferenceService.getPreferencesByWeek(year, weekNumber);
        return ResponseEntity.ok(BaseResponse.success(preferences, "Lấy danh sách nguyện vọng thành công"));
    }

    /**
     * Lấy danh sách nguyện vọng theo nhiều tuần (Manager+, dùng cho month mode)
     * GET /api/company/shift-preferences/by-weeks?year=2025&weekNumbers=5,6,7,8,9
     */
    @GetMapping("/by-weeks")
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<ShiftPreferenceResponse>>> getPreferencesByWeeks(
            @RequestParam Integer year,
            @RequestParam List<Integer> weekNumbers) {
        List<ShiftPreferenceResponse> preferences = shiftPreferenceService.getPreferencesByWeeks(year, weekNumbers);
        return ResponseEntity.ok(BaseResponse.success(preferences, "Lấy danh sách nguyện vọng thành công"));
    }

    /**
     * Áp dụng nguyện vọng thành phân ca (Manager+)
     * POST /api/company/shift-preferences/{id}/apply
     */
    @PostMapping("/{id}/apply")
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<ShiftPreferenceResponse>> applyPreference(
            @PathVariable Long id,
            @Valid @RequestBody ApplyPreferenceRequest request) {
        ShiftPreferenceResponse response = shiftPreferenceService.applyPreference(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Áp dụng nguyện vọng thành phân ca thành công"));
    }

    /**
     * Hoàn tác áp dụng nguyện vọng (Manager+)
     * POST /api/company/shift-preferences/{id}/revert
     */
    @PostMapping("/{id}/revert")
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Void>> revertPreference(@PathVariable Long id) {
        shiftPreferenceService.revertPreference(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Hoàn tác áp dụng nguyện vọng thành công"));
    }

    /**
     * Lấy gợi ý phân ca theo tuần (Manager+)
     * GET /api/company/shift-preferences/suggestions?year=2025&weekNumber=25
     */
    @GetMapping("/suggestions")
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<SuggestionResponse>>> getSuggestions(
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {
        List<SuggestionResponse> suggestions = suggestionEngine.getSuggestions(year, weekNumber);
        return ResponseEntity.ok(BaseResponse.success(suggestions, "Lấy gợi ý phân ca thành công"));
    }

    // ==================== Employee Endpoints ====================

    /**
     * Employee lấy nguyện vọng của mình theo tuần
     * GET /api/company/shift-preferences/my?year=2025&weekNumber=25
     */
    @GetMapping("/my")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<ShiftPreferenceResponse>>> getMyPreferences(
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {
        Long employeeId = getCurrentUserId();
        List<ShiftPreferenceResponse> preferences = shiftPreferenceService.getMyPreferences(employeeId, year, weekNumber);
        return ResponseEntity.ok(BaseResponse.success(preferences, "Lấy nguyện vọng của bạn thành công"));
    }

    /**
     * Employee tạo nguyện vọng mới
     * POST /api/company/shift-preferences
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<ShiftPreferenceResponse>>> createPreference(
            @Valid @RequestBody ShiftPreferenceRequest request) {
        Long employeeId = getCurrentUserId();
        List<ShiftPreferenceResponse> responses = shiftPreferenceService.createPreference(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(responses, "Tạo nguyện vọng ca làm việc thành công"));
    }

    /**
     * Employee cập nhật nguyện vọng (chỉ khi status = PENDING)
     * PUT /api/company/shift-preferences/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<ShiftPreferenceResponse>> updatePreference(
            @PathVariable Long id,
            @Valid @RequestBody ShiftPreferenceRequest request) {
        ShiftPreferenceResponse response = shiftPreferenceService.updatePreference(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật nguyện vọng thành công"));
    }

    /**
     * Employee xóa nguyện vọng (chỉ khi status = PENDING)
     * DELETE /api/company/shift-preferences/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Void>> deletePreference(@PathVariable Long id) {
        shiftPreferenceService.deletePreference(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa nguyện vọng thành công"));
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
