package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String username;
    private String email;
    private String name;
    private String lastname;
    private String avatarKey;
    private String satispayLink;
    private String revolutLink;
}