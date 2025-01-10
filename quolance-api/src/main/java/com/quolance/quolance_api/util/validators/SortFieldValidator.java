package com.quolance.quolance_api.util.validators;


import com.quolance.quolance_api.entities.enums.ProjectSortFields;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class SortFieldValidator implements ConstraintValidator<ValidSortField, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return Arrays.stream(value.split(","))
                .allMatch(ProjectSortFields::isValidField);
    }
}