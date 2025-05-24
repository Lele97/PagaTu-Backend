package com.pagatu.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDto {
    private Long id;
    private Long authId;
    private String username;
    private String email;
    private String name;
    private String lastname;
}