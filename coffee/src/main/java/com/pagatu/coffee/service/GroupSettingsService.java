package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupSettingsDto;
import com.pagatu.coffee.dto.GroupSettingsRequest;
import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.InvitationUserToGroupInformation;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupSettingsService {

    private final BaseUserService baseUserService;
    private final GroupRepository groupRepository;
    private final InvitationUserToGroupInformationRepository invitationRepository;
    private final MembershipDtoFactory membershipDtoFactory;

    @Transactional(readOnly = true)
    public GroupSettingsDto getSettings(Long userId, String groupName) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        assertAdmin(group, userId, groupName);
        return toDto(group);
    }

    @Transactional
    public GroupSettingsDto updateSettings(Long userId, GroupSettingsRequest request) {
        String currentName = request.getCurrentGroupName().trim();
        Group group = baseUserService.findGroupWithMembershipsByName(currentName);
        assertAdmin(group, userId, currentName);

        boolean hasUpdate = request.getNewGroupName() != null
                || request.getDescription() != null
                || request.getMaxSkipPerRound() != null
                || request.getPayForEnabled() != null
                || request.getPayForAdminOnly() != null;

        if (!hasUpdate) {
            throw new BusinessException("Specificare almeno un campo da aggiornare");
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().isBlank() ? null : request.getDescription().trim());
        }

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

        if (request.getNewGroupName() != null) {
            renameGroup(group, currentName, request.getNewGroupName().trim());
        }

        Group saved = groupRepository.save(group);
        return toDto(saved);
    }

    private void renameGroup(Group group, String currentName, String newName) {
        if (newName.isBlank()) {
            throw new BusinessException("Il nuovo nome del gruppo non può essere vuoto");
        }
        if (newName.equals(currentName)) {
            return;
        }

        groupRepository.getGroupByName(newName).ifPresent(existing -> {
            throw new BusinessException("Esiste già un gruppo con nome '" + newName + "'");
        });

        group.setName(newName);
        updatePendingInvitations(currentName, newName);
    }

    private void updatePendingInvitations(String oldName, String newName) {
        List<InvitationUserToGroupInformation> pending = invitationRepository.findActiveByGroupName(oldName);
        for (InvitationUserToGroupInformation invitation : pending) {
            invitation.setGroupName(newName);
        }
        if (!pending.isEmpty()) {
            invitationRepository.saveAll(pending);
        }
    }

    private GroupSettingsDto toDto(Group group) {
        List<UserMembershipDto> members = group.getUserMemberships().stream()
                .sorted(Comparator
                        .comparing((UserGroupMembership m) -> !Boolean.TRUE.equals(m.getIsAdmin()))
                        .thenComparing(m -> m.getJoinedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> membershipDtoFactory.toDto(group, m))
                .toList();

        return new GroupSettingsDto(
                group.getName(),
                group.getDescription(),
                group.getMaxSkipPerRound(),
                group.getPayForEnabled(),
                group.getPayForAdminOnly(),
                members);
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