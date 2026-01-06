package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.UpdateCompanyRequest;
import com.tamabee.api_hr.dto.response.CompanyResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.ICompanyManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller quản lý công ty cho admin Tamabee
 * Chỉ ADMIN_TAMABEE có quyền truy cập
 */
@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE)
public class CompanyManagerController {

    private final ICompanyManagerService companyManagerService;

    /**
     * Lấy danh sách tất cả công ty (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<CompanyResponse>>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CompanyResponse> companies = companyManagerService.getAllCompanies(pageable);
        return ResponseEntity.ok(BaseResponse.success(companies));
    }

    /**
     * Lấy thông tin chi tiết công ty theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        CompanyResponse company = companyManagerService.getCompanyById(id);
        return ResponseEntity.ok(BaseResponse.success(company));
    }

    /**
     * Cập nhật thông tin công ty
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<CompanyResponse>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse company = companyManagerService.updateCompany(id, request);
        return ResponseEntity.ok(BaseResponse.success(company, "Cập nhật công ty thành công"));
    }

    /**
     * Upload logo công ty
     */
    @PostMapping("/{id}/logo")
    public ResponseEntity<BaseResponse<String>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("logo") MultipartFile file) {
        String logoUrl = companyManagerService.uploadLogo(id, file);
        return ResponseEntity.ok(BaseResponse.success(logoUrl, "Tải logo thành công"));
    }

    /**
     * Deactivate công ty - set status = INACTIVE và remove DataSource khỏi pool.
     * Database vẫn được giữ lại 90 ngày cho compliance.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<BaseResponse<CompanyResponse>> deactivateCompany(@PathVariable Long id) {
        CompanyResponse company = companyManagerService.deactivateCompany(id);
        return ResponseEntity.ok(BaseResponse.success(company, "Đã deactivate công ty thành công"));
    }

    /**
     * Reactivate công ty - set status = ACTIVE và restore DataSource vào pool.
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<BaseResponse<CompanyResponse>> reactivateCompany(@PathVariable Long id) {
        CompanyResponse company = companyManagerService.reactivateCompany(id);
        return ResponseEntity.ok(BaseResponse.success(company, "Đã reactivate công ty thành công"));
    }
}
