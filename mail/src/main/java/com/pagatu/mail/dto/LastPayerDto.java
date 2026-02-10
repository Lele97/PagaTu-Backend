package com.pagatu.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the last payer in the coffee payment rotation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LastPayerDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private Boolean active = true;
    private String name;
    private String lastname;
}
