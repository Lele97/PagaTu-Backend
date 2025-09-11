package com.pagatu.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProssimoPagatoreDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private Boolean attivo = true;
    private String name;
    private String lastname;
}
