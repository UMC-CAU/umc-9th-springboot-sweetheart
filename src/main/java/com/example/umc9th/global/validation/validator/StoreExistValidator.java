package com.example.umc9th.global.validation.validator;

import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.global.response.code.ErrorCode;
import com.example.umc9th.global.validation.annotation.ExistStore;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @ExistStore 어노테이션에 대한 검증 로직
 * Store ID가 DB에 실제로 존재하는지 확인
 */
@Component
@RequiredArgsConstructor
public class StoreExistValidator implements ConstraintValidator<ExistStore, Long> {

    private final StoreRepository storeRepository;

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        // null은 @NotNull 어노테이션이 처리하도록 위임
        if (value == null) {
            return true;
        }

        boolean isValid = storeRepository.existsById(value);

        if (!isValid) {
            // 기본 메시지를 비활성화하고 커스텀 메시지로 대체
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ErrorCode.STORE_NOT_FOUND.getMessage())
                    .addConstraintViolation();
        }

        return isValid;
    }
}
