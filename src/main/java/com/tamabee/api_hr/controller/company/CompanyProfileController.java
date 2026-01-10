package com.tamabee.api_hr.controller.company;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.request.UpdateCompanyProfileRequest;
import com.tamabee.api_hr.dto.response.CompanyProfileResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.company.ICompanyProfileService;
import com.tamabee.api_hr.service.core.IUploadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý thông tin công ty
 */
@RestController
@RequestMapping("/api/company/profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final ICompanyProfileService companyProfileService;
    private final IUploadService uploadService;

    /**
     * Lấy thông tin công ty hiện tại
     */
    @GetMapping
    public ResponseEntity<BaseResponse<CompanyProfileResponse>> getMyCompanyProfile() {
        CompanyProfileResponse response = companyProfileService.getMyCompanyProfile();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Cập nhật thông tin công ty (chỉ Admin/Manager)
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'ADMIN_TAMABEE', 'MANAGER_TAMABEE')")
    public ResponseEntity<BaseResponse<CompanyProfileResponse>> updateCompanyProfile(
            @Valid @RequestBody UpdateCompanyProfileRequest request) {
        CompanyProfileResponse response = companyProfileService.updateCompanyProfile(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Cập nhật logo công ty (chỉ Admin/Manager)
     */
    @PutMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY', 'ADMIN_TAMABEE', 'MANAGER_TAMABEE')")
    public ResponseEntity<BaseResponse<CompanyProfileResponse>> updateLogo(
            @RequestParam("file") MultipartFile file) {
        // Upload file
        String logoUrl = uploadService.uploadFile(file, "logo", "company");
        // Cập nhật vào database
        CompanyProfileResponse response = companyProfileService.updateLogo(logoUrl);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
