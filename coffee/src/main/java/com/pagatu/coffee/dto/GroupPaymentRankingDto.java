package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupPaymentRankingDto {

    private String username;
    private Double totalAmount;
    private Long totalPayments;
}
