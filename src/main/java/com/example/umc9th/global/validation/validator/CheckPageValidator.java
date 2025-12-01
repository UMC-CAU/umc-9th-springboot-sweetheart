package com.example.umc9th.global.validation.validator;

import com.example.umc9th.global.validation.annotation.CheckPage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class CheckPageValidator implements ConstraintValidator<CheckPage, Integer> {

  @Override
  public void initialize(CheckPage constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // null은 다른 어노테이션(@NotNull 등)으로 처리하거나 허용
    }
    return value >= 1;
  }
}
