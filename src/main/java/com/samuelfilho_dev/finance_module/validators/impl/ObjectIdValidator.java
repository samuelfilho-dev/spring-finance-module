package com.samuelfilho_dev.finance_module.validators.impl;

import com.samuelfilho_dev.finance_module.validators.ObjectId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ObjectIdValidator implements ConstraintValidator<ObjectId, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return org.bson.types.ObjectId.isValid(value);
    }
}
