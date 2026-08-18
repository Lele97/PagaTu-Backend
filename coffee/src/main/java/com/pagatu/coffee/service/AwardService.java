package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserAwardDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserAward;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserAwardRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AwardService {

    static final String PRIMO_PAGAMENTO = "primo-pagamento";
    static final String GRUPPO_CREATO = "gruppo-creato";
    static final String STREAK_3 = "streak-3";
    static final String STREAK_7 = "streak-7";
    static final String AFFIDABILE = "affidabile";
    static final String GENEROSO = "generoso";
    static final String RE_DEL_CAFFE = "re-del-caffe";

    private static final int[] GENEROSO_MILESTONES = {3, 5, 10, 20};
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BaseUserService baseUserService;
    private final UserAwardRepository userAwardRepository;
    private final PaymentRepository paymentRepository;
    private final UserGroupMembershipRepository membershipRepository;

    @Transactional
    public List<UserAwardDto> listAndSync(Long userId) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        syncAwards(user);
        return userAwardRepository.findByCoffeeUserOrderByEarnedAtDesc(user).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void evaluateForUser(CoffeeUser user) {
        if (user == null) {
            return;
        }
        syncAwards(user);
    }

    @Transactional
    public void grantGroupCreated(CoffeeUser user, String groupName) {
        grant(user, GRUPPO_CREATO, "Gruppo creato: " + groupName, "bronze", "user-group",
                groupName, groupName);
    }

    private void syncAwards(CoffeeUser user) {
        List<Payment> payments = paymentRepository.findWithUserGroupMembershipByCoffeeUserOrderByPaymentDateDesc(user);
        List<UserGroupMembership> memberships = membershipRepository.findByCoffeeUserWithGroup(user);

        if (!payments.isEmpty()) {
            grant(user, PRIMO_PAGAMENTO, "Prima colazione", "bronze", "mug-hot", "once", null);
        }

        for (UserGroupMembership membership : memberships) {
            if (Boolean.TRUE.equals(membership.getIsAdmin()) && membership.getGroup() != null) {
                String groupName = membership.getGroup().getName();
                grant(user, GRUPPO_CREATO, "Gruppo creato: " + groupName, "bronze", "user-group",
                        groupName, groupName);
            }

            int streak = membership.getPaymentStreak() != null ? membership.getPaymentStreak() : 0;
            int paymentCount = membership.getPaymentCount() != null ? membership.getPaymentCount() : 0;
            String groupName = membership.getGroup() != null ? membership.getGroup().getName() : "gruppo";
            if (streak >= 3) {
                int hitAt = paymentCount - streak + 3;
                grant(user, STREAK_3, "Streak 3 turni · " + groupName, "bronze", "fire",
                        groupName + ":3:" + hitAt, groupName);
            }
            if (streak >= 7) {
                int hitAt = paymentCount - streak + 7;
                grant(user, STREAK_7, "Streak 7 turni · " + groupName, "silver", "star",
                        groupName + ":7:" + hitAt, groupName);
            }
        }

        int totalPayments = payments.size();
        int skippedCount = memberships.stream()
                .mapToInt(m -> m.getSkipCount() != null ? m.getSkipCount() : 0)
                .sum();
        if (totalPayments >= 5 && skippedCount == 0) {
            grant(user, AFFIDABILE, "Sempre presente", "silver", "check", "once", null);
        }

        long payForCount = payments.stream().filter(p -> p.getBeneficiaryUsername() != null).count();
        for (int milestone : GENEROSO_MILESTONES) {
            if (payForCount >= milestone) {
                grant(user, GENEROSO, "Generoso (" + milestone + " paga-per)", "gold", "heart",
                        String.valueOf(milestone), null);
            }
        }

        grantHistoricalKings(user, memberships);
    }

    private void grantHistoricalKings(CoffeeUser user, List<UserGroupMembership> memberships) {
        for (UserGroupMembership membership : memberships) {
            if (membership.getGroup() == null) {
                continue;
            }
            String groupName = membership.getGroup().getName();
            List<Payment> groupPayments = paymentRepository.findAllByGroupName(groupName);
            Map<YearMonth, Map<String, Double>> totals = new HashMap<>();
            for (Payment payment : groupPayments) {
                if (payment.getPaymentDate() == null || payment.getAmount() == null) {
                    continue;
                }
                String username = payment.getUserGroupMembership() != null
                        && payment.getUserGroupMembership().getCoffeeUser() != null
                        ? payment.getUserGroupMembership().getCoffeeUser().getUsername()
                        : null;
                if (username == null) {
                    continue;
                }
                YearMonth month = YearMonth.from(payment.getPaymentDate());
                totals.computeIfAbsent(month, ignored -> new HashMap<>())
                        .merge(username, payment.getAmount(), Double::sum);
            }

            for (Map.Entry<YearMonth, Map<String, Double>> monthEntry : totals.entrySet()) {
                Map<String, Double> byUser = monthEntry.getValue();
                double mine = byUser.getOrDefault(user.getUsername(), 0.0);
                if (mine <= 0) {
                    continue;
                }
                boolean someonePaidMore = byUser.entrySet().stream()
                        .anyMatch(e -> !e.getKey().equals(user.getUsername()) && e.getValue() > mine);
                if (someonePaidMore) {
                    continue;
                }
                String period = monthEntry.getKey().format(MONTH_KEY);
                grant(user, RE_DEL_CAFFE,
                        "Caffè King " + period + " · " + groupName,
                        "gold", "mug-hot",
                        groupName + ":" + period,
                        groupName);
            }
        }
    }

    private void grant(CoffeeUser user, String code, String name, String level, String icon,
                       String occurrenceKey, String groupName) {
        if (userAwardRepository.existsByCoffeeUserAndCodeAndOccurrenceKey(user, code, occurrenceKey)) {
            return;
        }
        UserAward award = new UserAward();
        award.setCoffeeUser(user);
        award.setCode(code);
        award.setName(name);
        award.setLevel(level);
        award.setIcon(icon);
        award.setOccurrenceKey(occurrenceKey);
        award.setGroupName(groupName);
        userAwardRepository.save(award);
    }

    private UserAwardDto toDto(UserAward award) {
        return new UserAwardDto(
                String.valueOf(award.getId()),
                award.getName(),
                award.getLevel(),
                award.getIcon(),
                award.getCode(),
                award.getGroupName(),
                award.getEarnedAt()
        );
    }
}
