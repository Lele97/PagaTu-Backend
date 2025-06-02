package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.event.InvitaionEvent;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UtenteRepository utenteRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private KafkaTemplate<String, InvitaionEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.invitation-caffe}")
    private String invitationTopic;

    public GroupService(GroupRepository groupRepository,
                        UtenteRepository utenteRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository, KafkaTemplate<String, InvitaionEvent> kafkaTemplate) {
        this.groupRepository = groupRepository;
        this.utenteRepository = utenteRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public GroupDto createGroup(NuovoGruppoRequest nuovoGruppoRequest, Long userId) {

        Utente utente = utenteRepository.findByAuthId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        groupRepository.getGroupByName(nuovoGruppoRequest.getName())
                .ifPresent(g -> {
                    throw new RuntimeException("Gruppo già esistente");
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
    public void addUserToGroup(AddUserToGroupRequest request) {

        try {

            Utente user = utenteRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Group group = groupRepository.getGroupByName(request.getGroupname())
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            UserGroupMembership membership = new UserGroupMembership();
            membership.setUtente(user);
            membership.setGroup(group);
            membership.setStatus(Status.NON_PAGATO);
            membership.setIsAdmin(false);
            membership.setJoinedAt(LocalDateTime.now());

            userGroupMembershipRepository.save(membership);

            log.info("Added user {} to group {}",
                    user.getUsername(), group.getName());

        } catch (RuntimeException ex) {
            log.error("Error adding user to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }


    public void sendInvitationToGroup(Long user_id, String groupName, String username) throws Exception {

        Group group = groupRepository.findGroupWithMembershipsByName(groupName).orElseThrow(() -> new Exception("Group not found"));

        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(membership ->
                        membership.getUtente() != null &&
                                membership.getUtente().getAuthId() != null &&  // Usa authId (String) invece di id (Long)?
                                membership.getUtente().getAuthId().equals(user_id) &&
                                Boolean.TRUE.equals(membership.getIsAdmin())
                );

        if (!isAdmin)
            throw new Exception("You are not an admin of this group!");


        Utente utente = utenteRepository.findByUsername(username).orElseThrow(() -> new Exception("The user you want to add does not exist"));

        InvitaionEvent event = new InvitaionEvent();
        event.setUsername(utente.getUsername());
        event.setGroupName(group.getName());

        kafkaTemplate.send(invitationTopic, event);

        log.info("Invitation event sent for user {} to group {}", username, groupName);

    }

    @Transactional
    public void updateMembershipStatus(UpdateMembershipStatusRequest request) {

        try {

            Utente user = utenteRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

            membership.setStatus(request.getNewStatus());
            userGroupMembershipRepository.save(membership);

            log.info("Updated membership status for user {} in group {} to {}",
                    user.getUsername(), group.getName(), request.getNewStatus());

        } catch (RuntimeException ex) {
            log.error("Error updating membership status: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {

        try {

            Utente user = utenteRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

            userGroupMembershipRepository.delete(membership);
            log.info("Removed user {} from group {}", user.getUsername(), group.getName());

        } catch (RuntimeException ex) {
            log.error("Error removing user from group: {}", ex.getMessage(), ex);
            throw ex;
        }
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
    public void deleteGroupByName(String groupName, Long userId) throws Exception {

        Group group = groupRepository.getGroupByName(groupName)
                .orElseThrow(() -> new Exception("The group does not exist."));

        boolean isMember = group.getUserMemberships().stream()
                .anyMatch(m -> m.getUtente().getAuthId().equals(userId));

        if (group.getUserMemberships().size() < 2 && isMember) {
            groupRepository.deleteGroupByName(groupName);
            log.info("Group '{}' deleted by user with ID {}", groupName, userId);
        } else {
            throw new Exception("has more than one user, or you are not a member.");
        }
    }
}