package com.web.motoridelab3api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordLengthValidator.class)
public @interface PasswordLength {
    int min() default 12;

    String message() default "Password is too short or exceeds BCrypt's 72-byte UTF-8 limit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
