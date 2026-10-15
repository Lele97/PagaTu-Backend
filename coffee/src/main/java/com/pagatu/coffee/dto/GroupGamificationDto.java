package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupGamificationDto {

    private String groupName;
    private String coffeeKingOfMonth;
    private List<MemberGamificationDto> members;
}