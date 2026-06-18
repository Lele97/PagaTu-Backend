package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final BaseUserService baseUserService;
    private final UserGroupMembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public GroupGamificationDto getGroupGamification(Long userId, GamificationRequest request) {
        CoffeeUser requester = baseUserService.findUserByAuthId(userId);
        Group group = baseUserService.findGroupByName(request.getGroupName());

        if (!membershipRepository.existsByCoffeeUserAndGroup(requester, group)) {
            throw new ForbiddenException("Non sei autorizzato a visualizzare le statistiche di questo gruppo");
        }

        List<UserGroupMembership> memberships = membershipRepository.findByGroup(group);
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String coffeeKing = memberships.stream()
                .map(m -> {
                    Double total = paymentRepository.sumPaymentsByUserInGroupForMonth(
                            request.getGroupName(), m.getCoffeeUser().getUsername(), year, month);
                    return new Object[]{m.getCoffeeUser().getUsername(), total != null ? total : 0.0};
                })
                .max(Comparator.comparingDouble(a -> (Double) a[1]))
                .map(a -> (String) a[0])
                .orElse(null);

        List<MemberGamificationDto> members = new ArrayList<>();
        for (UserGroupMembership membership : memberships) {
            CoffeeUser user = membership.getCoffeeUser();
            Double monthTotal = paymentRepository.sumPaymentsByUserInGroupForMonth(
                    request.getGroupName(), user.getUsername(), year, month);
            double paidThisMonth = monthTotal != null ? monthTotal : 0.0;

            int paymentCount = membership.getPaymentCount() != null ? membership.getPaymentCount() : 0;
            int skipCount = membership.getSkipCount() != null ? membership.getSkipCount() : 0;
            int streak = membership.getPaymentStreak() != null ? membership.getPaymentStreak() : 0;
            boolean isKing = user.getUsername().equals(coffeeKing) && paidThisMonth > 0;

            members.add(new MemberGamificationDto(
                    user.getUsername(),
                    paymentCount,
                    skipCount,
                    streak,
                    paidThisMonth,
                    isKing,
                    computeBadges(request.getGroupName(), membership, paymentCount, skipCount, streak, paidThisMonth, isKing)
            ));
        }

        return new GroupGamificationDto(request.getGroupName(), coffeeKing, members);
    }

    private List<BadgeDto> computeBadges(String groupName, UserGroupMembership membership, int paymentCount,
                                         int skipCount, int streak, double paidThisMonth, boolean isKing) {
        List<BadgeDto> badges = new ArrayList<>();

        if (paymentCount >= 1) {
            badges.add(new BadgeDto("PRIMO_PAGAMENTO", "Prima colazione", "Hai pagato la prima colazione del gruppo"));
        }
        if (isKing && paidThisMonth > 0) {
            badges.add(new BadgeDto("RE_DEL_CAFFE", "Re del caffè del mese",
                    "Hai pagato più colazioni di tutti questo mese"));
        }
        if (paymentCount >= 5 && skipCount == 0) {
            badges.add(new BadgeDto("AFFIDABILE", "Sempre presente", "Almeno 5 pagamenti senza mai saltare"));
        }
        if (streak >= 3) {
            badges.add(new BadgeDto("STREAK_MASTER", "Streak master",
                    "Almeno 3 colazioni consecutive pagate"));
        }
        if (membership.getJoinedAt() != null
                && membership.getJoinedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            badges.add(new BadgeDto("VETERANO", "Veterano del gruppo", "Membro del gruppo da oltre 30 giorni"));
        }
        long payForCount = paymentRepository.findAllByGroupName(groupName).stream()
                .filter(p -> p.getBeneficiaryUsername() != null
                        && p.getUserGroupMembership().getCoffeeUser().getUsername()
                        .equals(membership.getCoffeeUser().getUsername()))
                .count();
        if (payForCount >= 3) {
            badges.add(new BadgeDto("GENEROSO", "Generoso", "Hai pagato per i colleghi almeno 3 volte"));
        }

        return badges;
    }
}