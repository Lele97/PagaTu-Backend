package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NuovoGruppoRequest;
import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.event.InvitaionEvent;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final KafkaTemplate<String, InvitaionEvent> kafkaTemplate;
    private final BaseUserService baseUserService;

    @Value("${spring.kafka.topics.invitation-caffe}")
    private String invitationTopic;

    public GroupService(GroupRepository groupRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository,
                        KafkaTemplate<String, InvitaionEvent> kafkaTemplate,
                        BaseUserService baseUserService) {
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.baseUserService = baseUserService;
    }

    @Transactional
    public GroupDto createGroup(NuovoGruppoRequest nuovoGruppoRequest, Long userId) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        groupRepository.getGroupByName(nuovoGruppoRequest.getName())
                .ifPresent(g -> {
                    throw new BusinessException("Group already exists: " + nuovoGruppoRequest.getName());
                });

        Group group = new Group();
        group.setName(nuovoGruppoRequest.getName());
        UserGroupMembership membership = new UserGroupMembership();
        membership.setGroup(group);
        membership.setUtente(utente);
        membership.setStatus(Status.NON_PAGATO);
        membership.setIsAdmin(true);
        membership.setJoinedAt(LocalDateTime.now());
        group.getUserMemberships().add(membership);

        Group savedGroup = groupRepository.save(group);

        return mapToDto(savedGroup);
    }

    @Transactional
    public void addUserToGroup(String groupName, String username) {

        Utente user = baseUserService.findUserByUsername(username);
        Group group = baseUserService.findGroupByName(groupName);

        if (userGroupMembershipRepository.existsByUtenteAndGroup(user, group))
            throw new BusinessException("User '" + username + "' is already a member of group '" + groupName + "'");


        try {
            UserGroupMembership membership = new UserGroupMembership();
            membership.setUtente(user);
            membership.setGroup(group);
            membership.setStatus(Status.NON_PAGATO);
            membership.setIsAdmin(false);
            membership.setJoinedAt(LocalDateTime.now());

            userGroupMembershipRepository.save(membership);

            log.info("Added user {} to group {}",
                    user.getUsername(), group.getName());

        } catch (DataIntegrityViolationException ex) {
            log.warn("User {} already in group {}", username, groupName);
            throw new BusinessException("User is already in the group");
        } catch (RuntimeException ex) {
            log.error("Error adding user to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public void sendInvitationToGroup(Long userId, InvitationRequest invitationRequest) {

        Group group = baseUserService.findGroupWithMembershipsByName(invitationRequest.getGroupName());
        Utente utente = baseUserService.findUserByUsername(invitationRequest.getUsername());
        Utente userWhoSentTheInvitation = baseUserService.findUserByAuthId(userId);

        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(membership ->
                        membership.getUtente() != null &&
                                membership.getUtente().getAuthId() != null &&  // Usa authId (String) invece di id (Long)?
                                membership.getUtente().getAuthId().equals(userId) &&
                                Boolean.TRUE.equals(membership.getIsAdmin())
                );

        if (!isAdmin)
            throw new BusinessException("You are not an admin of group '" + invitationRequest.getGroupName() + "'");

        InvitaionEvent event = new InvitaionEvent();
        event.setUsername(utente.getUsername());
        event.setGroupName(group.getName());
        event.setEmail(utente.getEmail());
        event.setUserWhoSentTheInvitation(userWhoSentTheInvitation.getUsername());

        kafkaTemplate.send(invitationTopic, event);

        log.info("Invitation event sent for user {} to group {}", invitationRequest.getUsername(), invitationRequest.getGroupName());
    }

    private GroupDto mapToDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setId(group.getId());
        groupDto.setName(group.getName());

        List<UserMembershipDto> membershipDtos = group.getUserMemberships().stream()
                .map(m -> {
                    UserMembershipDto membershipDto = new UserMembershipDto();
                    membershipDto.setUserId(m.getUtente().getId());
                    membershipDto.setUsername(m.getUtente().getUsername());
                    membershipDto.setStatus(m.getStatus());
                    membershipDto.setIsAdmin(m.getIsAdmin());
                    return membershipDto;
                }).toList();

        groupDto.setUserMembershipsdto(membershipDtos);
        return groupDto;
    }

    @Transactional
    public void deleteGroupByName(String groupName, Long userId) {

        Group group = baseUserService.findGroupByName(groupName);

        boolean isMember = group.getUserMemberships().stream()
                .anyMatch(m -> m.getUtente().getAuthId().equals(userId));

        if (group.getUserMemberships().size() < 2 && isMember) {
            groupRepository.deleteGroupByName(groupName);
            log.info("Group '{}' deleted by user with ID {}", groupName, userId);
        } else {
            throw new BusinessException("Cannot delete group '" + groupName + "': group has more than one member or you are not a member");
        }
    }
}