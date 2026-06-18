package com.pagatu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenRequest {

    @NotBlank(message = "Il token è obbligatorio")
    private String token;
}