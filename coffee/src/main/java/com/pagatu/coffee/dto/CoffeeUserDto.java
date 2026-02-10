package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeUserDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private String name;
    private String lastname;
    private List<String> groups;
}
