package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.ContractRequest;
import com.tamabee.api_hr.dto.response.ContractResponse;
import com.tamabee.api_hr.entity.contract.EmploymentContractEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Mapper cho EmploymentContract entities và DTOs.
 */
@Component
public class EmploymentContractMapper {

    /**
     * Chuyển đổi từ Request DTO sang Entity
     */
    public EmploymentContractEntity toEntity(ContractRequest request, Long employeeId, Long companyId) {
        if (request == null) {
            return null;
        }

        EmploymentContractEntity entity = new EmploymentContractEntity();
        entity.setEmployeeId(employeeId);
        entity.setCompanyId(companyId);
        entity.setContractType(request.getContractType());
        entity.setContractNumber(request.getContractNumber());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setSalaryConfigId(request.getSalaryConfigId());
        entity.setStatus(ContractStatus.ACTIVE);
        entity.setNotes(request.getNotes());

        return entity;
    }

    /**
     * Cập nhật Entity từ Request DTO
     */
    public void updateEntity(EmploymentContractEntity entity, ContractRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setContractType(request.getContractType());
        entity.setContractNumber(request.getContractNumber());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setSalaryConfigId(request.getSalaryConfigId());
        entity.setNotes(request.getNotes());
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO
     */
    public ContractResponse toResponse(EmploymentContractEntity entity, UserEntity employee) {
        if (entity == null) {
            return null;
        }

        String employeeName = null;
        String employeeCode = null;
        if (employee != null) {
            employeeCode = employee.getEmployeeCode();
            if (employee.getProfile() != null) {
                employeeName = employee.getProfile().getName();
            }
        }

        // Tính số ngày còn lại đến khi hết hạn
        Integer daysUntilExpiry = null;
        if (entity.getEndDate() != null && entity.getStatus() == ContractStatus.ACTIVE) {
            LocalDate today = LocalDate.now();
            if (!entity.getEndDate().isBefore(today)) {
                daysUntilExpiry = (int) ChronoUnit.DAYS.between(today, entity.getEndDate());
            }
        }

        return ContractResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .companyId(entity.getCompanyId())
                .contractType(entity.getContractType())
                .contractNumber(entity.getContractNumber())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .salaryConfigId(entity.getSalaryConfigId())
                .status(entity.getStatus())
                .terminationReason(entity.getTerminationReason())
                .terminatedAt(entity.getTerminatedAt())
                .notes(entity.getNotes())
                .daysUntilExpiry(daysUntilExpiry)
                .createdAt(entity.getCreatedAt())
                .createdBy(null)
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(null)
                .build();
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO (không có thông tin employee)
     */
    public ContractResponse toResponse(EmploymentContractEntity entity) {
        return toResponse(entity, null);
    }
}
