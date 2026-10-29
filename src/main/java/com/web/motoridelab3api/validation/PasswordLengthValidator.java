package com.web.motoridelab3api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {
    private int min;

    @Override
    public void initialize(PasswordLength constraint) {
        min = constraint.min();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null
                || (value.codePointCount(0, value.length()) >= min
                        && value.getBytes(StandardCharsets.UTF_8).length <= 72);
    }
}
