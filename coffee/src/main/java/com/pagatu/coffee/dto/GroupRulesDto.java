package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRulesDto {

    private String groupName;
    private Integer maxSkipPerRound;
    private Boolean payForEnabled;
    private Boolean payForAdminOnly;
}