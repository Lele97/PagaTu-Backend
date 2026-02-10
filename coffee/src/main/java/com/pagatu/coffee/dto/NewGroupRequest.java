package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.CoffeeUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewGroupRequest {

    private String name;
    private String description;
    private List<CoffeeUser> users;
}
