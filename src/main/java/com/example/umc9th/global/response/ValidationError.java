package com.example.umc9th.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationError {

    private final String field;
    private final Object rejectedValue;
    private final String reason;
}
