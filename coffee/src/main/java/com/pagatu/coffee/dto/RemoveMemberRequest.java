package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveMemberRequest {

    @NotBlank(message = "Il nome del gruppo è obbligatorio")
    private String groupName;

    @NotBlank(message = "Lo username del membro è obbligatorio")
    private String username;
}