package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {

    private double totalPaid;
    private long totalCoffeesForOthers;
    private int timesKing;
    private int currentStreak;
    private int longestStreak;
    private int skippedCount;
    private int coffeeKarma;
    private String funTitle;
    private double monthlySavedForFriends;
    private double averagePayment;
    private double mostExpensive;
}
