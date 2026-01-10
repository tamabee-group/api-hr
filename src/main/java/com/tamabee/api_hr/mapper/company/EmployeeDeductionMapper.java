package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.DeductionAssignmentRequest;
import com.tamabee.api_hr.dto.response.EmployeeDeductionResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeDeductionEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper cho Employee Deduction
 */
@Component
public class EmployeeDeductionMapper {

    /**
     * Chuyển đổi từ request DTO sang entity
     *
     * @param request    Request DTO
     * @param employeeId ID nhân viên
     * @return Entity
     */
    public EmployeeDeductionEntity toEntity(DeductionAssignmentRequest request, Long employeeId) {
        if (request == null) {
            return null;
        }

        EmployeeDeductionEntity entity = new EmployeeDeductionEntity();
        entity.setEmployeeId(employeeId);
        entity.setDeductionCode(request.getDeductionCode());
        entity.setDeductionName(request.getDeductionName());
        entity.setDeductionType(request.getDeductionType());
        entity.setAmount(request.getAmount());
        entity.setPercentage(request.getPercentage());
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
    public EmployeeDeductionResponse toResponse(EmployeeDeductionEntity entity, UserEntity employee) {
        if (entity == null) {
            return null;
        }

        EmployeeDeductionResponse response = new EmployeeDeductionResponse();
        response.setId(entity.getId());
        response.setEmployeeId(entity.getEmployeeId());
        response.setDeductionCode(entity.getDeductionCode());
        response.setDeductionName(entity.getDeductionName());
        response.setDeductionType(entity.getDeductionType());
        response.setAmount(entity.getAmount());
        response.setPercentage(entity.getPercentage());
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
    public void updateEntity(EmployeeDeductionEntity entity, DeductionAssignmentRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setDeductionCode(request.getDeductionCode());
        entity.setDeductionName(request.getDeductionName());
        entity.setDeductionType(request.getDeductionType());
        entity.setAmount(request.getAmount());
        entity.setPercentage(request.getPercentage());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
    }
}
