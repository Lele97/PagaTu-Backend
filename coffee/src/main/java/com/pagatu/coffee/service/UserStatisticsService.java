package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserStatisticsDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {

    private final BaseUserService baseUserService;
    private final PaymentRepository paymentRepository;
    private final UserGroupMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public UserStatisticsDto getStatistics(Long userId) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        List<Payment> payments = paymentRepository.findByCoffeeUserOrderByPaymentDateDesc(user);
        List<UserGroupMembership> memberships = membershipRepository.findByCoffeeUserWithGroup(user);

        double totalPaid = payments.stream().mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0).sum();
        long payForCount = payments.stream().filter(p -> p.getBeneficiaryUsername() != null).count();
        double mostExpensive = payments.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0)
                .max()
                .orElse(0);
        double averagePayment = payments.isEmpty() ? 0 : totalPaid / payments.size();

        YearMonth now = YearMonth.now();
        double monthlySavedForFriends = payments.stream()
                .filter(p -> p.getBeneficiaryUsername() != null && p.getPaymentDate() != null)
                .filter(p -> YearMonth.from(p.getPaymentDate()).equals(now))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0)
                .sum();

        int skippedCount = memberships.stream()
                .mapToInt(m -> m.getSkipCount() != null ? m.getSkipCount() : 0)
                .sum();
        int currentStreak = memberships.stream()
                .mapToInt(m -> m.getPaymentStreak() != null ? m.getPaymentStreak() : 0)
                .max()
                .orElse(0);
        int timesKing = countTimesKingThisMonth(user, memberships);

        int coffeeKarma = computeKarma(payments.size(), skippedCount, payForCount);
        String funTitle = computeTitle(payments.size(), skippedCount, currentStreak, payForCount, timesKing);

        return UserStatisticsDto.builder()
                .totalPaid(round2(totalPaid))
                .totalCoffeesForOthers(payForCount)
                .timesKing(timesKing)
                .currentStreak(currentStreak)
                .longestStreak(currentStreak)
                .skippedCount(skippedCount)
                .coffeeKarma(coffeeKarma)
                .funTitle(funTitle)
                .monthlySavedForFriends(round2(monthlySavedForFriends))
                .averagePayment(round2(averagePayment))
                .mostExpensive(round2(mostExpensive))
                .build();
    }

    private int countTimesKingThisMonth(CoffeeUser user, List<UserGroupMembership> memberships) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int kings = 0;
        for (UserGroupMembership membership : memberships) {
            String groupName = membership.getGroup().getName();
            Double mine = paymentRepository.sumPaymentsByUserInGroupForMonth(
                    groupName, user.getUsername(), year, month);
            double myTotal = mine != null ? mine : 0;
            if (myTotal <= 0) {
                continue;
            }
            boolean someonePaidMore = membershipRepository.findByGroup(membership.getGroup()).stream()
                    .map(m -> m.getCoffeeUser().getUsername())
                    .filter(name -> !name.equals(user.getUsername()))
                    .anyMatch(name -> {
                        Double other = paymentRepository.sumPaymentsByUserInGroupForMonth(
                                groupName, name, year, month);
                        return (other != null ? other : 0) > myTotal;
                    });
            if (!someonePaidMore) {
                kings++;
            }
        }
        return kings;
    }

    static int computeKarma(int paymentCount, int skippedCount, long payForCount) {
        if (paymentCount == 0 && skippedCount == 0) {
            return 50;
        }
        int penalty = (int) Math.min(60, skippedCount * 12.0);
        int bonus = (int) Math.min(20, payForCount * 4);
        int base = 70 + bonus - penalty + Math.min(20, paymentCount);
        return Math.max(0, Math.min(100, base));
    }

    static String computeTitle(int paymentCount, int skippedCount, int streak, long payForCount, int timesKing) {
        if (paymentCount == 0) {
            return "Coffee Newbie";
        }
        if (skippedCount > paymentCount) {
            return "Skip Master";
        }
        if (timesKing >= 1 || payForCount >= 5 || streak >= 7) {
            return "Coffee Legend";
        }
        if (skippedCount == 0 && paymentCount >= 3) {
            return "The Reliable One";
        }
        return "Coffee Newbie";
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
