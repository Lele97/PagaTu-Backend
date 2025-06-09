package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserToGroupRequest {

    @NotBlank(message = "Group name is required")
    private String groupName;

    @NotBlank(message = "Username is required")
    private String username;
}
