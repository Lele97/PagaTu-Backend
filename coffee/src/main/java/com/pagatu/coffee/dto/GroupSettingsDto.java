package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSettingsDto {

    private String name;
    private String description;
    private Integer maxSkipPerRound;
    private Boolean payForEnabled;
    private Boolean payForAdminOnly;
    private List<UserMembershipDto> members;
}