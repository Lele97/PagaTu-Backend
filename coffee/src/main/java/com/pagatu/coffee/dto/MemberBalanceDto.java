package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberBalanceDto {

    private String username;
    private double totalPaid;
    private double fairShare;
    private double netBalance;
    private String satispayLink;
    private String revolutLink;
}