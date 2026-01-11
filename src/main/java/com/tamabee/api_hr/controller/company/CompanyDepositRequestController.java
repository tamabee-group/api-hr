package com.tamabee.api_hr.controller.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.wallet.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.wallet.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.wallet.DepositRequestResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.company.interfaces.ICompanyDepositService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý yêu cầu nạp tiền cho Company
 * ADMIN_COMPANY có quyền tạo yêu cầu nạp tiền
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền xem danh sách yêu cầu
 */
@RestController
@RequestMapping("/api/company/deposits")
@RequiredArgsConstructor
public class CompanyDepositRequestController {

    private final ICompanyDepositService companyDepositService;
    private final IUploadService uploadService;
    private final ISettingService settingService;

    /**
     * Tạo yêu cầu nạp tiền mới
     * POST /api/company/deposits
     * Chỉ ADMIN_COMPANY có quyền
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> create(
            @Valid @RequestBody DepositRequestCreateRequest request) {
        DepositRequestResponse response = companyDepositService.create(request);
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
        Page<DepositRequestResponse> deposits = companyDepositService.getMyRequests(filter, pageable);

        return ResponseEntity.ok(BaseResponse.success(deposits));
    }

    /**
     * Hủy yêu cầu nạp tiền đang chờ duyệt
     * DELETE /api/company/deposits/{id}
     * Chỉ ADMIN_COMPANY có quyền, chỉ hủy được khi status = PENDING
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> cancel(@PathVariable Long id) {
        DepositRequestResponse response = companyDepositService.cancel(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Hủy yêu cầu nạp tiền thành công"));
    }

    /**
     * Upload ảnh chứng từ chuyển khoản
     * POST /api/company/deposits/upload-proof
     * Chỉ ADMIN_COMPANY có quyền
     */
    @PostMapping("/upload-proof")
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<String>> uploadTransferProof(
            @RequestParam("file") MultipartFile file) {
        String proofUrl = uploadService.uploadFile(file, "deposit", "proof");
        return ResponseEntity.ok(BaseResponse.success(proofUrl, "Tải ảnh chứng từ thành công"));
    }

    /**
     * Lấy số tiền nạp tối thiểu
     * GET /api/company/deposits/min-amount
     */
    @GetMapping("/min-amount")
    @PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<Integer>> getMinDepositAmount() {
        int minAmount = settingService.getMinDepositAmount();
        return ResponseEntity.ok(BaseResponse.success(minAmount));
    }
}
