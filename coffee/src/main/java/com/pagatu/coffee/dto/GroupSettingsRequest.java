package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSettingsRequest {

    @NotBlank(message = "Il nome attuale del gruppo è obbligatorio")
    private String currentGroupName;

    @Size(max = 255, message = "Il nome del gruppo non può superare 255 caratteri")
    private String newGroupName;

    @Size(max = 1000, message = "La descrizione non può superare 1000 caratteri")
    private String description;

    private Integer maxSkipPerRound;
    private Boolean payForEnabled;
    private Boolean payForAdminOnly;
}