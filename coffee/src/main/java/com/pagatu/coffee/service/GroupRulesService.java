package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupRulesDto;
import com.pagatu.coffee.dto.GroupRulesRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupRulesService {

    private final BaseUserService baseUserService;
    private final GroupRepository groupRepository;

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
    }

    public void validateSkipAllowed(Group group, UserGroupMembership membership) {
        Integer maxSkip = group.getMaxSkipPerRound();
        if (maxSkip == null) {
            return;
        }
        int currentRoundSkips = membership.getRoundSkipCount() != null ? membership.getRoundSkipCount() : 0;
        if (currentRoundSkips >= maxSkip) {
            throw new BusinessException("Hai raggiunto il limite di " + maxSkip + " skip per questo giro");
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