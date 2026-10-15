package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberGamificationDto {

    private String username;
    private int paymentCount;
    private int skipCount;
    private int paymentStreak;
    private double totalPaidThisMonth;
    private boolean coffeeKingOfMonth;
    private List<BadgeDto> badges;
}