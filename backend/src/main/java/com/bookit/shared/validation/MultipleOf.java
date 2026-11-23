package com.bookit.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MultipleOf.Validator.class)
public @interface MultipleOf {

    int value();

    String message() default "must be a multiple of {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MultipleOf, Integer> {

        private int divisor;

        @Override
        public void initialize(MultipleOf annotation) {
            this.divisor = annotation.value();
        }

        @Override
        public boolean isValid(Integer value, ConstraintValidatorContext context) {
            return value == null || value % divisor == 0;
        }
    }
}
