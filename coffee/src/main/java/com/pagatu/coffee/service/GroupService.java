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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final KafkaTemplate<String, InvitaionEvent> kafkaTemplate;

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
    public void addUserToGroup(String groupName, String username) throws Exception {

        Utente user = utenteRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.getGroupByName(groupName)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (userGroupMembershipRepository.existsByUtenteAndGroup(user, group))
            throw new Exception("User already in group");


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
            throw new Exception("User is already in the group");
        } catch (RuntimeException ex) {
            log.error("Error adding user to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public void sendInvitationToGroup(Long userId, InvitationRequest invitationRequest) throws Exception {

        Group group = groupRepository.findGroupWithMembershipsByName(invitationRequest.getGroupName()).orElseThrow(() -> new Exception("Group not found"));
        Utente utente = utenteRepository.findByUsername(invitationRequest.getUsername()).orElseThrow(() -> new Exception("The user you want to add does not exist"));
        Utente userWhoSentTheInvitation = utenteRepository.findByAuthId(userId).orElseThrow(() -> new Exception("User not found"));

        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(membership ->
                        membership.getUtente() != null &&
                                membership.getUtente().getAuthId() != null &&  // Usa authId (String) invece di id (Long)?
                                membership.getUtente().getAuthId().equals(userId) &&
                                Boolean.TRUE.equals(membership.getIsAdmin())
                );

        if (!isAdmin)
            throw new Exception("You are not an admin of this group!");

        InvitaionEvent event = new InvitaionEvent();
        event.setUsername(utente.getUsername());
        event.setGroupName(group.getName());
        event.setEmail(utente.getEmail());
        event.setUserWhoSentTheInvitation(userWhoSentTheInvitation.getUsername());

        kafkaTemplate.send(invitationTopic, event);

        log.info("Invitation event sent for user {} to group {}", invitationRequest.getUsername(), invitationRequest.getGroupName());
    }

//    @Transactional
//    public void updateMembershipStatus(UpdateMembershipStatusRequest request) {
//
//        try {
//
//            Utente user = utenteRepository.findById(request.getUserId())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            Group group = groupRepository.findById(request.getGroupId())
//                    .orElseThrow(() -> new RuntimeException("Group not found"));
//
//            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
//                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
//
//            membership.setStatus(request.getNewStatus());
//            userGroupMembershipRepository.save(membership);
//
//            log.info("Updated membership status for user {} in group {} to {}",
//                    user.getUsername(), group.getName(), request.getNewStatus());
//
//        } catch (RuntimeException ex) {
//            log.error("Error updating membership status: {}", ex.getMessage(), ex);
//            throw ex;
//        }
//    }

//    @Transactional
//    public void removeUserFromGroup(Long userId, Long groupId) {
//
//        try {
//
//            Utente user = utenteRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            Group group = groupRepository.findById(groupId)
//                    .orElseThrow(() -> new RuntimeException("Group not found"));
//
//            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
//                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
//
//            userGroupMembershipRepository.delete(membership);
//            log.info("Removed user {} from group {}", user.getUsername(), group.getName());
//
//        } catch (RuntimeException ex) {
//            log.error("Error removing user from group: {}", ex.getMessage(), ex);
//            throw ex;
//        }
//    }

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