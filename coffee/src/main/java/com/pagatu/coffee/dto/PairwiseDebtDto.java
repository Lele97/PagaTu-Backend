package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairwiseDebtDto {

    private String debtorUsername;
    private String creditorUsername;
    private double amount;
    private String creditorSatispayLink;
    private String creditorRevolutLink;
}