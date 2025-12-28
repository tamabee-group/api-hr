package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IDepositRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý yêu cầu nạp tiền cho Company
 * ADMIN_COMPANY có quyền tạo yêu cầu nạp tiền
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền xem danh sách yêu cầu
 */
@RestController
@RequestMapping("/api/company/deposits")
@RequiredArgsConstructor
public class CompanyDepositRequestController {

    private final IDepositRequestService depositRequestService;

    /**
     * Tạo yêu cầu nạp tiền mới
     * POST /api/company/deposits
     * Chỉ ADMIN_COMPANY có quyền
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> create(
            @Valid @RequestBody DepositRequestCreateRequest request) {
        DepositRequestResponse response = depositRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo yêu cầu nạp tiền thành công"));
    }

    /**
     * Lấy danh sách yêu cầu nạp tiền của company hiện tại
     * GET /api/company/deposits
     * 
     * @param page   số trang (mặc định 0)
     * @param size   số lượng mỗi trang (mặc định 20)
     * @param status trạng thái yêu cầu (filter)
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Page<DepositRequestResponse>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DepositStatus status) {

        // Tạo filter request
        DepositFilterRequest filter = new DepositFilterRequest();
        filter.setStatus(status);

        Pageable pageable = PageRequest.of(page, size);
        Page<DepositRequestResponse> deposits = depositRequestService.getMyRequests(filter, pageable);

        return ResponseEntity.ok(BaseResponse.success(deposits));
    }
}
