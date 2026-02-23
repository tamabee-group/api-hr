package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.payroll.PayrollConfigRequest;
import com.tamabee.api_hr.entity.company.PayrollSettingEntity;
import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.SalaryType;

/**
 * Mapper chuyển đổi giữa PayrollSettingEntity và PayrollConfig DTO.
 * Xử lý mapping giữa entity (string fields cho enum) và DTO (enum fields).
 */
@Component
public class PayrollSettingMapper {

    /**
     * Tạo entity mới với default values
     */
    public PayrollSettingEntity toEntity() {
        PayrollSettingEntity entity = new PayrollSettingEntity();
        entity.setDefaultSalaryType(SalaryType.MONTHLY.name());
        entity.setPayDay(25);
        entity.setCutoffDay(20);
        entity.setSalaryRounding(RoundingDirection.NEAREST.name());
        entity.setStandardWorkingDaysPerMonth(22);
        entity.setStandardWorkingHoursPerDay(8);
        return entity;
    }

    /**
     * Chuyển PayrollSettingEntity sang PayrollConfig DTO
     */
    public PayrollConfig toResponse(PayrollSettingEntity entity) {
        if (entity == null) {
            return PayrollConfig.builder().build();
        }

        return PayrollConfig.builder()
                .defaultSalaryType(parseSalaryType(entity.getDefaultSalaryType()))
                .payDay(entity.getPayDay())
                .cutoffDay(entity.getCutoffDay())
                .salaryRounding(parseRoundingDirection(entity.getSalaryRounding()))
                .standardWorkingDaysPerMonth(entity.getStandardWorkingDaysPerMonth())
                .standardWorkingHoursPerDay(entity.getStandardWorkingHoursPerDay())
                .build();
    }

    /**
     * Cập nhật entity từ request (chỉ cập nhật các field không null)
     */
    public void updateEntity(PayrollSettingEntity entity, PayrollConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getDefaultSalaryType() != null) {
            entity.setDefaultSalaryType(request.getDefaultSalaryType().name());
        }
        if (request.getPayDay() != null) {
            entity.setPayDay(request.getPayDay());
        }
        if (request.getCutoffDay() != null) {
            entity.setCutoffDay(request.getCutoffDay());
        }
        if (request.getSalaryRounding() != null) {
            entity.setSalaryRounding(request.getSalaryRounding().name());
        }
        if (request.getStandardWorkingDaysPerMonth() != null) {
            entity.setStandardWorkingDaysPerMonth(request.getStandardWorkingDaysPerMonth());
        }
        if (request.getStandardWorkingHoursPerDay() != null) {
            entity.setStandardWorkingHoursPerDay(request.getStandardWorkingHoursPerDay());
        }
    }

    // ==================== Helper methods ====================

    /**
     * Parse string sang SalaryType enum, trả về default nếu null/invalid
     */
    private SalaryType parseSalaryType(String value) {
        if (value == null) {
            return SalaryType.MONTHLY;
        }
        try {
            return SalaryType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return SalaryType.MONTHLY;
        }
    }

    /**
     * Parse string sang RoundingDirection enum, trả về default nếu null/invalid
     */
    private RoundingDirection parseRoundingDirection(String value) {
        if (value == null) {
            return RoundingDirection.NEAREST;
        }
        try {
            return RoundingDirection.valueOf(value);
        } catch (IllegalArgumentException e) {
            return RoundingDirection.NEAREST;
        }
    }
}
