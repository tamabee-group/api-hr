package com.tamabee.api_hr.validation;

import java.time.LocalDate;
import java.time.Period;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator cho annotation MinAge
 * Kiểm tra tuổi tối thiểu theo luật lao động (mặc định 15 tuổi)
 */
public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {

    private int minAge;

    @Override
    public void initialize(MinAge constraintAnnotation) {
        this.minAge = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        // Cho phép null (không bắt buộc nhập ngày sinh)
        if (dateOfBirth == null) {
            return true;
        }

        int age = Period.between(dateOfBirth, LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()))).getYears();
        return age >= minAge;
    }
}
