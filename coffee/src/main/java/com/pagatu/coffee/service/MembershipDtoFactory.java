package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class MembershipDtoFactory {

    public static final int MAX_SKIP_PER_MONTH = 4;
    public static final int MAX_PAYFOR_PER_MONTH = 4;

    private final PaymentRepository paymentRepository;

    public UserMembershipDto toDto(Group group, UserGroupMembership membership) {
        UserMembershipDto dto = new UserMembershipDto();
        dto.setUserId(membership.getCoffeeUser().getId());
        dto.setUsername(membership.getCoffeeUser().getUsername());
        dto.setName(membership.getCoffeeUser().getName());
        dto.setLastname(membership.getCoffeeUser().getLastname());
        dto.setStatus(membership.getStatus());
        dto.setMyTurn(membership.getMyTurn());
        dto.setIsAdmin(membership.getIsAdmin());
        dto.setJoinedAt(membership.getJoinedAt());

        int roundSkips = membership.getRoundSkipCount() != null ? membership.getRoundSkipCount() : 0;
        dto.setRoundSkipCount(roundSkips);
        Integer maxSkipPerRound = group.getMaxSkipPerRound();
        dto.setSkipsRemaining(maxSkipPerRound == null ? null : Math.max(0, maxSkipPerRound - roundSkips));

        int monthlySkips = effectiveMonthlySkipCount(membership);
        dto.setMonthlySkipCount(monthlySkips);
        dto.setMonthlySkipsRemaining(Math.max(0, MAX_SKIP_PER_MONTH - monthlySkips));

        YearMonth now = YearMonth.now();
        long payForCount = paymentRepository.countPayForByUserInGroupForMonth(
                group.getName(),
                membership.getCoffeeUser().getUsername(),
                now.getYear(),
                now.getMonthValue());
        dto.setMonthlyPayForCount((int) payForCount);
        dto.setMonthlyPayForRemaining(Math.max(0, MAX_PAYFOR_PER_MONTH - (int) payForCount));

        return dto;
    }

    public static int effectiveMonthlySkipCount(UserGroupMembership membership) {
        String period = YearMonth.now().toString();
        if (!period.equals(membership.getMonthlySkipPeriod())) {
            return 0;
        }
        return membership.getMonthlySkipCount() != null ? membership.getMonthlySkipCount() : 0;
    }

    public void incrementMonthlySkip(UserGroupMembership membership) {
        String period = YearMonth.now().toString();
        if (!period.equals(membership.getMonthlySkipPeriod())) {
            membership.setMonthlySkipPeriod(period);
            membership.setMonthlySkipCount(1);
            return;
        }
        int count = membership.getMonthlySkipCount() != null ? membership.getMonthlySkipCount() : 0;
        membership.setMonthlySkipCount(count + 1);
    }
}