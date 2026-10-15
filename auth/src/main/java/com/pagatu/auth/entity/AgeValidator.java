package com.pagatu.auth.entity;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

class AgeValidator implements ConstraintValidator<Age, LocalDate> {
    private int minAge;

    @Override
    public void initialize(Age age) {
        this.minAge = age.min();
    }

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) return true;
        
        LocalDate today = LocalDate.now();
        int age = Period.between(dateOfBirth, today).getYears();
        
        return age >= minAge;
    }
}
