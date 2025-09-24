package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDetailDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private String name;
    private String lastname;
}
