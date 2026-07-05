package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupDto {

    private Long id;
    private String name;
    private String description;
    private List<UserMembershipDto> userMembershipsdto;
    private Integer memberCount;
    private String currentTurnUsername;
    private Integer maxSkipPerRound;
    private Boolean payForEnabled;
    private Boolean payForAdminOnly;
    private Integer roundPaidCount;
    private Integer roundPendingCount;
    private Integer currentRoundNumber;
    private Integer maxSkipPerMonth;
    private Integer maxPayForPerMonth;
}
