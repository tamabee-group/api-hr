package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.UpdateCompanyRequest;
import com.tamabee.api_hr.dto.response.CompanyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service quản lý công ty cho admin Tamabee
 */
public interface ICompanyManagerService {

    /**
     * Lấy danh sách tất cả công ty (phân trang)
     */
    Page<CompanyResponse> getAllCompanies(Pageable pageable);

    /**
     * Lấy thông tin chi tiết công ty theo ID
     */
    CompanyResponse getCompanyById(Long id);

    /**
     * Cập nhật thông tin công ty
     */
    CompanyResponse updateCompany(Long id, UpdateCompanyRequest request);

    /**
     * Upload logo công ty
     */
    String uploadLogo(Long id, MultipartFile file);
}
