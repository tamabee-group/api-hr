package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.PlanCreateRequest;
import com.tamabee.api_hr.dto.request.PlanUpdateRequest;
import com.tamabee.api_hr.dto.response.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service quản lý gói dịch vụ (Plan)
 * Hỗ trợ CRUD operations và quản lý features
 */
public interface IPlanService {

    /**
     * Tạo plan mới với features
     * 
     * @param request thông tin plan và features
     * @return plan đã tạo
     */
    PlanResponse create(PlanCreateRequest request);

    /**
     * Cập nhật plan và features
     * 
     * @param id      ID của plan
     * @param request thông tin cập nhật
     * @return plan đã cập nhật
     */
    PlanResponse update(Long id, PlanUpdateRequest request);

    /**
     * Xóa plan (soft delete)
     * Kiểm tra plan có đang được sử dụng không trước khi xóa
     * 
     * @param id ID của plan
     */
    void delete(Long id);

    /**
     * Lấy thông tin plan theo ID
     * 
     * @param id ID của plan
     * @return thông tin plan với features
     */
    PlanResponse getById(Long id);

    /**
     * Lấy danh sách tất cả plans (phân trang)
     * 
     * @param pageable thông tin phân trang
     * @return danh sách plans
     */
    Page<PlanResponse> getAll(Pageable pageable);

    /**
     * Lấy danh sách plans đang active
     * Dùng cho public API
     * 
     * @return danh sách plans active, sắp xếp theo giá tăng dần
     */
    List<PlanResponse> getActivePlans();
}
