package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import com.tamabee.api_hr.dto.request.payroll.CreateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.response.payroll.SalaryItemTemplateResponse;
import com.tamabee.api_hr.enums.SalaryItemType;

/**
 * Service interface cho quản lý template phụ cấp/khấu trừ
 */
public interface ISalaryItemTemplateService {

    /**
     * Lấy tất cả templates
     *
     * @return Danh sách templates
     */
    List<SalaryItemTemplateResponse> getAllTemplates();

    /**
     * Lấy templates theo loại
     *
     * @param type Loại template (ALLOWANCE/DEDUCTION)
     * @return Danh sách templates theo loại
     */
    List<SalaryItemTemplateResponse> getTemplatesByType(SalaryItemType type);

    /**
     * Tạo template mới
     *
     * @param request Thông tin template
     * @return Template đã tạo
     */
    SalaryItemTemplateResponse createTemplate(CreateSalaryItemTemplateRequest request);

    /**
     * Cập nhật template
     *
     * @param id      ID template
     * @param request Thông tin cập nhật
     * @return Template đã cập nhật
     */
    SalaryItemTemplateResponse updateTemplate(Long id, UpdateSalaryItemTemplateRequest request);

    /**
     * Xóa template (soft delete)
     *
     * @param id ID template
     */
    void deleteTemplate(Long id);

    /**
     * Lấy số nhân viên đang sử dụng template
     *
     * @param id ID template
     * @return Số nhân viên đang sử dụng
     */
    long getEmployeeCountByTemplateId(Long id);
}
