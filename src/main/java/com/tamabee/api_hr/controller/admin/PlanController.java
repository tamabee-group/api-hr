package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.wallet.PlanCreateRequest;
import com.tamabee.api_hr.dto.request.wallet.PlanUpdateRequest;
import com.tamabee.api_hr.dto.response.wallet.PlanResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.admin.interfaces.IPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý gói dịch vụ (Plan) cho admin Tamabee
 * Chỉ ADMIN_TAMABEE có quyền truy cập
 */
@RestController
@RequestMapping("/api/admin/plans")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE)
public class PlanController {

    private final IPlanService planService;

    /**
     * Tạo plan mới
     * POST /api/admin/plans
     */
    @PostMapping
    public ResponseEntity<BaseResponse<PlanResponse>> create(
            @Valid @RequestBody PlanCreateRequest request) {
        PlanResponse plan = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(plan, "Tạo gói dịch vụ thành công"));
    }

    /**
     * Cập nhật plan
     * PUT /api/admin/plans/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<PlanResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PlanUpdateRequest request) {
        PlanResponse plan = planService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(plan, "Cập nhật gói dịch vụ thành công"));
    }

    /**
     * Xóa plan (soft delete)
     * DELETE /api/admin/plans/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa gói dịch vụ thành công"));
    }

    /**
     * Lấy thông tin plan theo ID
     * GET /api/admin/plans/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<PlanResponse>> getById(@PathVariable Long id) {
        PlanResponse plan = planService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(plan));
    }

    /**
     * Lấy danh sách tất cả plans (phân trang)
     * GET /api/admin/plans
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<PlanResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PlanResponse> plans = planService.getAll(pageable);
        return ResponseEntity.ok(BaseResponse.success(plans));
    }
}
