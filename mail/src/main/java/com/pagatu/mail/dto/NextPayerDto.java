package com.pagatu.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the next payer in the coffee payment rotation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextPayerDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private Boolean active = true;
    private String name;
    private String lastname;
}
