package com.pmrodrigues.commons.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated string is a syntactically and algorithmically valid Brazilian CNPJ.
 * Accepts both formatted ({@code XX.XXX.XXX/XXXX-XX}) and unformatted (14-digit) values.
 * {@code null} and blank values are considered valid — combine with {@code @NotBlank} to reject them.
 */
@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CNPJ {

    String message() default "CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
