package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NewGroupRequest;
import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.InvitationEvent;
import com.pagatu.coffee.event.InvitationResponseEvent;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing coffee payment groups and group memberships.
 * <p>
 * This service handles all group-related operations including:
 * <ul>
 * <li>Creating new coffee payment groups</li>
 * <li>Adding users to groups</li>
 * <li>Managing group memberships and permissions</li>
 * <li>Sending group invitations via NATS events</li>
 * <li>Deleting groups with proper authorization checks</li>
 * </ul>
 * </p>
 * <p>
 * The service integrates with NATS to send invitation events when users
 * are invited to join groups. All operations include proper authorization
 * checks to ensure users can only perform actions they're permitted to.
 * </p>
 */
@Service
@Slf4j
public class GroupService {

    @Value("${spring.nats.subject.invitation-subject}")
    private String natsSubject;

    @Value("${spring.nats.subject.invitation-response-subject}")
    private String natsSubjectsInvitationResponse;

    private final OutboxService outboxService;
    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final InvitationUserToGroupInformationRepository invitationUserToGroupInformationRepository;
    private final BaseUserService baseUserService;

    /**
     * @param outboxService                               transactional outbox for NATS events
     * @param groupRepository                             group persistence
     * @param userGroupMembershipRepository               membership management
     * @param invitationUserToGroupInformationRepository  invitation records
     * @param baseUserService                             shared user/group resolution
     */
    public GroupService(OutboxService outboxService,
                        GroupRepository groupRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository, InvitationUserToGroupInformationRepository invitationUserToGroupInformationRepository,
                        BaseUserService baseUserService) {
        this.outboxService = outboxService;
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.invitationUserToGroupInformationRepository = invitationUserToGroupInformationRepository;
        this.baseUserService = baseUserService;
    }

    /**
     * Creates a new coffee payment group with the user as admin.
     *
     * @param newGroupRequest the request containing group name and description
     * @param userId          the ID of the user creating the group
     * @return GroupDto containing the created group information
     */
    @Transactional
    public GroupDto createGroup(NewGroupRequest newGroupRequest, Long userId) {

        CoffeeUser coffeeUser = baseUserService.findUserByAuthId(userId);

        groupRepository.getGroupByName(newGroupRequest.getName())
                .ifPresent(g -> {
                    throw new BusinessException("Group already exists: " + newGroupRequest.getName());
                });

        Group group = new Group();
        group.setName(newGroupRequest.getName());
        group.setDescription(newGroupRequest.getDescription());
        UserGroupMembership membership = new UserGroupMembership();
        membership.setGroup(group);
        membership.setCoffeeUser(coffeeUser);
        membership.setStatus(PaymentStatus.NON_PAGATO);
        membership.setIsAdmin(true);
        membership.setMyTurn(true);
        membership.setJoinedAt(LocalDateTime.now());
        group.getUserMemberships().add(membership);

        Group savedGroup = groupRepository.save(group);

        return mapToDto(savedGroup);
    }

    /**
     * Accepts a group invitation and adds the invited user to the group.
     * <p>
     * The invitation must be active and is marked as accepted before the user
     * is added as a non-admin member with {@link PaymentStatus#NON_PAGATO}.
     * A response event is published to notify the inviter.
     * </p>
     *
     * @param groupName    the name of the group to join
     * @param username     the username of the invited user
     * @param invitationId the identifier of the active invitation
     * @throws BusinessException if the invitation is missing, inactive, or the user is already a member
     */
    @Transactional
    public void addUserToGroup(String groupName, String username, Long invitationId) {

        InvitationUserToGroupInformation invitationUserToGroupInformation =
                invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId)
                        .orElseThrow(() -> new BusinessException("Invito non trovato o non più attivo"));
        Long userWhoSentTheInvitation = invitationUserToGroupInformation.getUserWhoSentInvitation();
        invitationUserToGroupInformation.setUsedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.ACCEPTED);
        invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        CoffeeUser user = baseUserService.findUserByUsername(username);
        CoffeeUser userSendInvitation = baseUserService.findUserByAuthId(userWhoSentTheInvitation);
        Group group = baseUserService.findGroupByName(groupName);

        if (userGroupMembershipRepository.existsByCoffeeUserAndGroup(user, group))
            throw new BusinessException("User '" + username + "' is already a member of group '" + groupName + "'");

        try {
            UserGroupMembership membership = new UserGroupMembership();
            membership.setCoffeeUser(user);
            membership.setGroup(group);
            membership.setStatus(PaymentStatus.NON_PAGATO);
            membership.setIsAdmin(false);
            membership.setJoinedAt(LocalDateTime.now());

            userGroupMembershipRepository.save(membership);

            log.info("Added user {} to group {}",
                    user.getUsername(), group.getName());


            InvitationResponseEvent invitationResponseEvent = new InvitationResponseEvent();
            invitationResponseEvent.setGroupName(groupName);
            invitationResponseEvent.setUsername(username);
            invitationResponseEvent.setUserWhoSentTheInvitation(userWhoSentTheInvitation);
            invitationResponseEvent.setAccepted(true);
            invitationResponseEvent.setEmail(userSendInvitation.getEmail());

            outboxService.saveEvent(natsSubjectsInvitationResponse, invitationResponseEvent);


        } catch (DataIntegrityViolationException ex) {
            log.warn("User {} already in group {}", username, groupName);
            throw new BusinessException("User is already in the group");
        } catch (RuntimeException ex) {
            log.error("Error adding user to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Sends a group invitation to a user via NATS event.
     *
     * @param userId            the ID of the user sending the invitation
     * @param invitationRequest contains the group name and username to invite
     * @throws BusinessException if the user is not an admin of the specified group
     */
    public void sendInvitationToGroup(Long userId, InvitationRequest invitationRequest) {

        Group group = baseUserService.findGroupWithMembershipsByName(invitationRequest.getGroupName());
        CoffeeUser coffeeUser = baseUserService.findUserByUsername(invitationRequest.getUsername());
        CoffeeUser userWhoSentTheInvitation = baseUserService.findUserByAuthId(userId);

        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(membership -> membership.getCoffeeUser() != null &&
                        membership.getCoffeeUser().getAuthId() != null &&
                        membership.getCoffeeUser().getAuthId().equals(userId) &&
                        Boolean.TRUE.equals(membership.getIsAdmin()));

        if (!isAdmin)
            throw new BusinessException("You are not an admin of group '" + invitationRequest.getGroupName() + "'");


        InvitationUserToGroupInformation invitationUserToGroupInformation = new InvitationUserToGroupInformation();
        invitationUserToGroupInformation.setUserWhoSentInvitation(userId);
        invitationUserToGroupInformation.setGroupName(invitationRequest.getGroupName());
        invitationUserToGroupInformation.setUsername(invitationRequest.getUsername());
        invitationUserToGroupInformation.setEmail(coffeeUser.getEmail());
        invitationUserToGroupInformation.setCreatedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setExpiredDate(LocalDateTime.now().plusMinutes(60));
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.ACTIVE);

        InvitationUserToGroupInformation savedInvitationUserToGroupInformation = invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        InvitationEvent event = new InvitationEvent();
        event.setUsername(coffeeUser.getUsername());
        event.setGroupName(group.getName());
        event.setEmail(coffeeUser.getEmail());
        event.setUserWhoSentTheInvitation(userWhoSentTheInvitation.getUsername());
        event.setInvitationId(savedInvitationUserToGroupInformation.getId());

        outboxService.saveEvent(natsSubject, event);

        log.info("Invitation event sent for user {} to group {}", invitationRequest.getUsername(),
                invitationRequest.getGroupName());
    }

    /**
     * Maps a Group entity to a GroupDto including user membership information.
     *
     * @param group the Group entity to convert
     * @return GroupDto with populated membership data
     */
    private GroupDto mapToDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setId(group.getId());
        groupDto.setName(group.getName());
        groupDto.setDescription(group.getDescription());

        List<UserMembershipDto> membershipDtos = group.getUserMemberships().stream()
                .map(m -> {
                    UserMembershipDto membershipDto = new UserMembershipDto();
                    membershipDto.setUserId(m.getCoffeeUser().getId());
                    membershipDto.setUsername(m.getCoffeeUser().getUsername());
                    membershipDto.setStatus(m.getStatus());
                    membershipDto.setMyTurn(m.getMyTurn());
                    membershipDto.setIsAdmin(m.getIsAdmin());
                    return membershipDto;
                }).toList();

        groupDto.setUserMembershipsdto(membershipDtos);
        return groupDto;
    }

    /**
     * Deletes a group by name if the user is the only member.
     *
     * @param groupName the name of the group to delete
     * @param userId    the ID of the user requesting deletion
     * @throws BusinessException if group has multiple members or user is not a
     *                           member
     */
    @Transactional
    public void deleteGroupByName(String groupName, Long userId) {

        Group group = baseUserService.findGroupByName(groupName);

        boolean isMember = group.getUserMemberships().stream()
                .anyMatch(m -> m.getCoffeeUser().getAuthId().equals(userId));

        if (group.getUserMemberships().size() < 2 && isMember) {
            groupRepository.deleteGroupByName(groupName);
            log.info("Group '{}' deleted by user with ID {}", groupName, userId);
        } else {
            throw new BusinessException(
                    "Cannot delete group '" + groupName + "': group has more than one member or you are not a member");
        }
    }

    /**
     * Retrieves all groups that a user is member of.
     *
     * @param username the username to search groups for
     * @return List of GroupDto containing the user's groups
     */
    @Transactional
    public List<GroupDto> getGroupsByUsername(String username) {
        List<Group> groups = groupRepository.getGroupsByUsername(username);
        return groups.stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Rejects a pending group invitation.
     * <p>
     * The invitation must be active and is marked as rejected. A response event
     * is published so the group admin receives a notification email.
     * </p>
     *
     * @param groupName    the name of the group referenced by the invitation
     * @param username     the username of the user rejecting the invitation
     * @param invitationId the identifier of the active invitation
     * @throws BusinessException if the invitation is missing, inactive, or the user is already a member
     */
    @Transactional
    public void rejectInvitation(String groupName, String username, Long invitationId) {

        InvitationUserToGroupInformation invitationUserToGroupInformation =
                invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId)
                        .orElseThrow(() -> new BusinessException("Invito non trovato o non più attivo"));
        Long userWhoSentTheInvitation = invitationUserToGroupInformation.getUserWhoSentInvitation();
        invitationUserToGroupInformation.setUsedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.REJECTED);
        invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        CoffeeUser user = baseUserService.findUserByUsername(username);
        CoffeeUser userSendInvitation = baseUserService.findUserByAuthId(userWhoSentTheInvitation);
        Group group = baseUserService.findGroupByName(groupName);

        if (userGroupMembershipRepository.existsByCoffeeUserAndGroup(user, group))
            throw new BusinessException("User '" + username + "' is already a member of group '" + groupName + "'");


        try {

            InvitationResponseEvent invitationResponseEvent = new InvitationResponseEvent();

            invitationResponseEvent.setGroupName(groupName);
            invitationResponseEvent.setUsername(username);
            invitationResponseEvent.setUserWhoSentTheInvitation(userWhoSentTheInvitation);
            invitationResponseEvent.setAccepted(false);
            invitationResponseEvent.setEmail(userSendInvitation.getEmail());

            outboxService.saveEvent(natsSubjectsInvitationResponse, invitationResponseEvent);
            

        } catch (RuntimeException ex) {
            log.error("Error rejecting invitazion to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}