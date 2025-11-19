package com.example.umc9th.global.validation.annotation;

import com.example.umc9th.global.validation.validator.LocationExistValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 지역 존재 여부 검증 어노테이션
 * Location ID가 DB에 실제로 존재하는지 확인
 */
@Documented
@Constraint(validatedBy = LocationExistValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExistLocation {
    String message() default "해당 지역이 존재하지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
