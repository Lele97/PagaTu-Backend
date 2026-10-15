package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupRulesDto;
import com.pagatu.coffee.dto.GroupRulesRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.PaymentStatus;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class GroupRulesService {

    private final BaseUserService baseUserService;
    private final GroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final MembershipDtoFactory membershipDtoFactory;

    @Transactional(readOnly = true)
    public GroupRulesDto getRules(Long userId, String groupName) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        assertMember(group, userId, groupName);
        return toDto(group);
    }

    @Transactional
    public GroupRulesDto updateRules(Long userId, GroupRulesRequest request) {
        Group group = baseUserService.findGroupWithMembershipsByName(request.getGroupName());
        assertAdmin(group, userId, request.getGroupName());

        if (request.getMaxSkipPerRound() != null) {
            if (request.getMaxSkipPerRound() < 0) {
                throw new BusinessException("Il numero massimo di skip non può essere negativo");
            }
            group.setMaxSkipPerRound(request.getMaxSkipPerRound());
        }
        if (request.getPayForEnabled() != null) {
            group.setPayForEnabled(request.getPayForEnabled());
        }
        if (request.getPayForAdminOnly() != null) {
            group.setPayForAdminOnly(request.getPayForAdminOnly());
        }

        Group saved = groupRepository.save(group);
        return toDto(saved);
    }

    public void validatePayForAllowed(Group group, CoffeeUser payingUser) {
        if (Boolean.FALSE.equals(group.getPayForEnabled())) {
            throw new BusinessException("La funzione 'paga per' è disabilitata in questo gruppo");
        }
        if (Boolean.TRUE.equals(group.getPayForAdminOnly())) {
            boolean isAdmin = group.getUserMemberships().stream()
                    .anyMatch(m -> m.getCoffeeUser().getAuthId().equals(payingUser.getAuthId())
                            && Boolean.TRUE.equals(m.getIsAdmin()));
            if (!isAdmin) {
                throw new ForbiddenException("Solo l'admin può usare 'paga per' in questo gruppo");
            }
        }
        validatePayForMonthlyLimit(group, payingUser);
    }

    public void validateSkipAllowed(Group group, UserGroupMembership membership) {
        int monthlySkips = MembershipDtoFactory.effectiveMonthlySkipCount(membership);
        if (monthlySkips >= MembershipDtoFactory.MAX_SKIP_PER_MONTH) {
            throw new BusinessException("Hai raggiunto il limite di "
                    + MembershipDtoFactory.MAX_SKIP_PER_MONTH + " skip al mese");
        }

        Integer maxSkip = group.getMaxSkipPerRound();
        if (maxSkip != null) {
            int currentRoundSkips = membership.getRoundSkipCount() != null ? membership.getRoundSkipCount() : 0;
            if (currentRoundSkips >= maxSkip) {
                throw new BusinessException("Hai raggiunto il limite di " + maxSkip + " skip per questo giro");
            }
        }
    }

    public void validateNotLastUnpaidMember(Group group, UserGroupMembership membership) {
        long unpaidCount = group.getUserMemberships().stream()
                .filter(m -> PaymentStatus.NON_PAGATO.equals(m.getStatus()))
                .count();
        if (unpaidCount <= 1 && Boolean.TRUE.equals(membership.getMyTurn())) {
            throw new BusinessException("In questo giro qualcuno deve pagare: non puoi saltare");
        }
    }

    public void incrementMonthlySkip(UserGroupMembership membership) {
        membershipDtoFactory.incrementMonthlySkip(membership);
    }

    private void validatePayForMonthlyLimit(Group group, CoffeeUser payingUser) {
        YearMonth now = YearMonth.now();
        long count = paymentRepository.countPayForByUserInGroupForMonth(
                group.getName(),
                payingUser.getUsername(),
                now.getYear(),
                now.getMonthValue());
        if (count >= MembershipDtoFactory.MAX_PAYFOR_PER_MONTH) {
            throw new BusinessException("Hai raggiunto il limite di "
                    + MembershipDtoFactory.MAX_PAYFOR_PER_MONTH + " 'paga per' al mese");
        }
    }

    private GroupRulesDto toDto(Group group) {
        return new GroupRulesDto(
                group.getName(),
                group.getMaxSkipPerRound(),
                group.getPayForEnabled(),
                group.getPayForAdminOnly()
        );
    }

    private void assertMember(Group group, Long userId, String groupName) {
        boolean isMember = group.getUserMemberships().stream()
                .anyMatch(m -> m.getCoffeeUser().getAuthId().equals(userId));
        if (!isMember) {
            throw new ForbiddenException("Non sei membro del gruppo '" + groupName + "'");
        }
    }

    private void assertAdmin(Group group, Long userId, String groupName) {
        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(m -> m.getCoffeeUser().getAuthId().equals(userId)
                        && Boolean.TRUE.equals(m.getIsAdmin()));
        if (!isAdmin) {
            throw new ForbiddenException("Non sei admin del gruppo '" + groupName + "'");
        }
    }
}