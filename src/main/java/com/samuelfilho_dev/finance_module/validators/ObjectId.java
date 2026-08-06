package com.samuelfilho_dev.finance_module.validators;

import com.samuelfilho_dev.finance_module.validators.impl.ObjectIdValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ObjectIdValidator.class)
@Documented
public @interface ObjectId {

    String message() default "Invalid ObjectId";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
