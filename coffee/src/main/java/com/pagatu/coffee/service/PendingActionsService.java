package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PendingActionDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.InvitationUserToGroupInformation;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingActionsService {

    private final BaseUserService baseUserService;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final InvitationUserToGroupInformationRepository invitationRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<PendingActionDto> getPendingActions(Long userId) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        List<PendingActionDto> actions = new ArrayList<>();

        List<UserGroupMembership> activeTurns = userGroupMembershipRepository.findActiveTurnsByUser(user);
        for (UserGroupMembership membership : activeTurns) {
            PendingActionDto action = new PendingActionDto();
            action.setType("TURNO");
            action.setGroupName(membership.getGroup().getName());
            action.setGroupDescription(membership.getGroup().getDescription());
            action.setMessage("È il tuo turno di pagare la colazione!");
            actions.add(action);
        }

        List<InvitationUserToGroupInformation> invitations = invitationRepository
                .findActiveInvitationsForUser(user.getUsername(), user.getEmail(), LocalDateTime.now());

        for (InvitationUserToGroupInformation invitation : invitations) {
            PendingActionDto action = new PendingActionDto();
            action.setType("INVITO");
            action.setGroupName(invitation.getGroupName());
            action.setInvitationId(invitation.getId());
            groupRepository.getGroupByName(invitation.getGroupName())
                    .ifPresent(g -> action.setGroupDescription(g.getDescription()));
            action.setInvitedBy(baseUserService.findUserByAuthId(invitation.getUserWhoSentInvitation()).getUsername());
            action.setMessage("Hai un invito pendente per il gruppo " + invitation.getGroupName());
            actions.add(action);
        }

        return actions;
    }
}