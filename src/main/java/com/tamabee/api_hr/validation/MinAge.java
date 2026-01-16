package com.tamabee.api_hr.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Annotation để validate tuổi tối thiểu
 */
@Documented
@Constraint(validatedBy = MinAgeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinAge {
    String message() default "Tuổi phải từ {value} trở lên";
    int value() default 15;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
