package com.pagatu.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing user information for inter-service communication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private String name;
    private String lastname;
    private List<String> groups;
}
