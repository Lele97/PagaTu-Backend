package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupBalanceDto {

    private String groupName;
    private double totalSpent;
    private int memberCount;
    private List<MemberBalanceDto> memberBalances;
    private List<PairwiseDebtDto> pairwiseDebts;
}