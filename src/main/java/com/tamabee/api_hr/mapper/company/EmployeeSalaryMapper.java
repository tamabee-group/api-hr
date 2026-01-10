package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.payroll.SalaryConfigRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryConfigResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Mapper cho EmployeeSalary entities và DTOs.
 */
@Component
public class EmployeeSalaryMapper {

    /**
     * Chuyển đổi từ Request DTO sang Entity
     */
    public EmployeeSalaryEntity toEntity(SalaryConfigRequest request, Long employeeId) {
        if (request == null) {
            return null;
        }

        EmployeeSalaryEntity entity = new EmployeeSalaryEntity();
        entity.setEmployeeId(employeeId);
        entity.setSalaryType(request.getSalaryType());
        entity.setMonthlySalary(request.getMonthlySalary());
        entity.setDailyRate(request.getDailyRate());
        entity.setHourlyRate(request.getHourlyRate());
        entity.setShiftRate(request.getShiftRate());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setNote(request.getNote());

        return entity;
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO
     */
    public EmployeeSalaryConfigResponse toResponse(EmployeeSalaryEntity entity, UserEntity employee) {
        if (entity == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        boolean isActive = entity.getEffectiveFrom().isBefore(today.plusDays(1)) &&
                (entity.getEffectiveTo() == null || entity.getEffectiveTo().isAfter(today.minusDays(1)));

        String employeeName = null;
        if (employee != null && employee.getProfile() != null) {
            employeeName = employee.getProfile().getName();
        }

        return EmployeeSalaryConfigResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .salaryType(entity.getSalaryType())
                .monthlySalary(entity.getMonthlySalary())
                .dailyRate(entity.getDailyRate())
                .hourlyRate(entity.getHourlyRate())
                .shiftRate(entity.getShiftRate())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .isActive(isActive)
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .createdBy(null)
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(null)
                .build();
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO (không có thông tin employee)
     */
    public EmployeeSalaryConfigResponse toResponse(EmployeeSalaryEntity entity) {
        return toResponse(entity, null);
    }
}
