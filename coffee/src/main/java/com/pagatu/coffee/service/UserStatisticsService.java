package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.CoffeeKarmaRequest;
import com.pagatu.coffee.dto.UserStatisticsDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {

    private final BaseUserService baseUserService;
    private final CoffeeUserRepository coffeeUserRepository;
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

        int skippedCount = memberships.stream()
                .mapToInt(m -> m.getSkipCount() != null ? m.getSkipCount() : 0)
                .sum();

        int coffeeKarma = user.getCoffeeKarma() == null || user.getCoffeeKarma() <= 0 ? 50 : user.getCoffeeKarma();
        //String funTitle = computeTitle(payments.size(), skippedCount, currentStreak, payForCount, timesKing);

        return UserStatisticsDto.builder()
                .totalPaid(round2(totalPaid))
                .totalCoffeesForOthers(payForCount)
                .skippedCount(skippedCount)
                .coffeeKarma(coffeeKarma)
                .averagePayment(round2(averagePayment))
                .mostExpensive(round2(mostExpensive))
                .build();
    }

    @Transactional
    public void computeKarma(Long userId, CoffeeKarmaRequest request) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        int karma = user.getCoffeeKarma() == null || user.getCoffeeKarma() <= 0 ? 50 : user.getCoffeeKarma();
        String operation = request.getType_operation() != null ? request.getType_operation() : "";
        double amount = request.getAmount() != null ? request.getAmount() : 0;

        karma = switch (operation) {
            case "jump_turn" -> (int) Math.round(karma * 0.9);
            case "payment_for" -> (int) Math.round(karma * (1.05 + Math.round(amount) / 100.0));
            case "payment" -> (int) Math.round(karma * (1 + Math.round(amount) / 100.0));
            default -> karma;
        };

        user.setCoffeeKarma(Math.max(0, Math.min(100, karma)));
        coffeeUserRepository.save(user);
    }
    
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
