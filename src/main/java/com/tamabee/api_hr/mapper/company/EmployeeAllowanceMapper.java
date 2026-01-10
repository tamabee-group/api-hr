package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.payroll.AllowanceAssignmentRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeAllowanceResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeAllowanceEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper cho Employee Allowance
 */
@Component
public class EmployeeAllowanceMapper {

    /**
     * Chuyển đổi từ request DTO sang entity
     *
     * @param request    Request DTO
     * @param employeeId ID nhân viên
     * @return Entity
     */
    public EmployeeAllowanceEntity toEntity(AllowanceAssignmentRequest request, Long employeeId) {
        if (request == null) {
            return null;
        }

        EmployeeAllowanceEntity entity = new EmployeeAllowanceEntity();
        entity.setEmployeeId(employeeId);
        entity.setAllowanceCode(request.getAllowanceCode());
        entity.setAllowanceName(request.getAllowanceName());
        entity.setAllowanceType(request.getAllowanceType());
        entity.setAmount(request.getAmount());
        entity.setTaxable(request.getTaxable());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setIsActive(true);

        return entity;
    }

    /**
     * Chuyển đổi từ entity sang response DTO
     *
     * @param entity   Entity
     * @param employee Thông tin nhân viên (optional)
     * @return Response DTO
     */
    public EmployeeAllowanceResponse toResponse(EmployeeAllowanceEntity entity, UserEntity employee) {
        if (entity == null) {
            return null;
        }

        EmployeeAllowanceResponse response = new EmployeeAllowanceResponse();
        response.setId(entity.getId());
        response.setEmployeeId(entity.getEmployeeId());
        response.setAllowanceCode(entity.getAllowanceCode());
        response.setAllowanceName(entity.getAllowanceName());
        response.setAllowanceType(entity.getAllowanceType());
        response.setAmount(entity.getAmount());
        response.setTaxable(entity.getTaxable());
        response.setEffectiveFrom(entity.getEffectiveFrom());
        response.setEffectiveTo(entity.getEffectiveTo());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Set employee name nếu có
        if (employee != null && employee.getProfile() != null) {
            response.setEmployeeName(employee.getProfile().getName());
        }

        return response;
    }

    /**
     * Cập nhật entity từ request DTO
     *
     * @param entity  Entity cần cập nhật
     * @param request Request DTO
     */
    public void updateEntity(EmployeeAllowanceEntity entity, AllowanceAssignmentRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setAllowanceCode(request.getAllowanceCode());
        entity.setAllowanceName(request.getAllowanceName());
        entity.setAllowanceType(request.getAllowanceType());
        entity.setAmount(request.getAmount());
        entity.setTaxable(request.getTaxable());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
    }
}
