package com.pagatu.coffee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {

    private String username;

    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "Il nome del gruppo è obbligatorio")
    private String groupName;
}