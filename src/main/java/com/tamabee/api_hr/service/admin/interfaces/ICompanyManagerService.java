package com.tamabee.api_hr.service.admin.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.request.company.UpdateCompanyRequest;
import com.tamabee.api_hr.dto.response.company.CompanyResponse;

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

    /**
     * Deactivate công ty - set status = INACTIVE và remove DataSource khỏi pool.
     * Database vẫn được giữ lại 90 ngày cho compliance.
     */
    CompanyResponse deactivateCompany(Long id);

    /**
     * Reactivate công ty - set status = ACTIVE và restore DataSource vào pool.
     * Chỉ có thể reactivate trong vòng 90 ngày sau khi deactivate.
     */
    CompanyResponse reactivateCompany(Long id);

    /**
     * Xóa hoàn toàn công ty - xóa company record và drop tenant database.
     * Yêu cầu xác nhận bằng tên công ty để tránh xóa nhầm.
     *
     * @param id ID của công ty
     * @param confirmName Tên công ty để xác nhận
     */
    void deleteCompany(Long id, String confirmName);
}
