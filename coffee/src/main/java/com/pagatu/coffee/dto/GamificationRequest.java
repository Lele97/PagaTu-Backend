package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamificationRequest {

    @NotBlank(message = "Il nome del gruppo è obbligatorio")
    private String groupName;
}