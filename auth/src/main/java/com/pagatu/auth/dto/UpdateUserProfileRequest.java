package com.pagatu.auth.dto;

import com.pagatu.auth.entity.Age;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]*$", message = "First name contains invalid characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]*$", message = "Last name contains invalid characters")
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    @Age(min = 13, message = "Must be at least 13 years old")
    private LocalDate dateOfBirth;
}