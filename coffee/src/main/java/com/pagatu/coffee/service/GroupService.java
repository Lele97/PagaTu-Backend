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
import com.pagatu.coffee.exception.UserNotInGroup;
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

/**
 * Service for managing coffee payment groups and group memberships.
 * <p>
 * This service handles all group-related operations including:
 * <ul>
 *   <li>Creating new coffee payment groups</li>
 *   <li>Adding users to groups</li>
 *   <li>Managing group memberships and permissions</li>
 *   <li>Sending group invitations via Kafka events</li>
 *   <li>Deleting groups with proper authorization checks</li>
 * </ul>
 * </p>
 * <p>
 * The service integrates with Kafka to send invitation events when users
 * are invited to join groups. All operations include proper authorization
 * checks to ensure users can only perform actions they're permitted to.
 * </p>
 */
@Service
@Slf4j
public class GroupService {

    @Value("${spring.kafka.topics.invitation-caffe}")
    private String invitationTopic;

    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final KafkaTemplate<String, InvitaionEvent> kafkaTemplate;
    private final BaseUserService baseUserService;

    public GroupService(GroupRepository groupRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository,
                        KafkaTemplate<String, InvitaionEvent> kafkaTemplate,
                        BaseUserService baseUserService) {
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.baseUserService = baseUserService;
    }

    /**
     * Creates a new coffee payment group with the user as admin.
     * <p>
     * This method creates a new group and automatically adds the creating user
     * as the first member with admin privileges. The user is set as the first
     * person to pay (myTurn = true) and has NON_PAGATO status.
     * </p>
     *
     * @param nuovoGruppoRequest the request containing group name and description
     * @param userId the ID of the user creating the group
     * @return GroupDto containing the created group information
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.BusinessException if group name already exists
     */
    @Transactional
    public GroupDto createGroup(NuovoGruppoRequest nuovoGruppoRequest, Long userId) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        groupRepository.getGroupByName(nuovoGruppoRequest.getName())
                .ifPresent(g -> {
                    throw new BusinessException("Group already exists: " + nuovoGruppoRequest.getName());
                });

        Group group = new Group();
        group.setName(nuovoGruppoRequest.getName());
        group.setDescription(nuovoGruppoRequest.getDescription());
        UserGroupMembership membership = new UserGroupMembership();
        membership.setGroup(group);
        membership.setUtente(utente);
        membership.setStatus(Status.NON_PAGATO);
        membership.setIsAdmin(true);
        membership.setMyTurn(true);
        membership.setJoinedAt(LocalDateTime.now());
        group.getUserMemberships().add(membership);

        Group savedGroup = groupRepository.save(group);

        return mapToDto(savedGroup);
    }

    /**
     * Adds a user to the specified group with default NON_PAGATO status.
     * <p>
     * This method adds a user to an existing coffee payment group with the following characteristics:
     * <ul>
     *   <li>Sets the user's payment status to NON_PAGATO (not paid)</li>
     *   <li>Assigns non-admin privileges by default</li>
     *   <li>Records the join timestamp</li>
     *   <li>Validates that the user is not already a member of the group</li>
     * </ul>
     * </p>
     * <p>
     * If the user is already a member of the group, a BusinessException is thrown.
     * The method handles database integrity violations and other runtime exceptions
     * with appropriate error logging and exception conversion.
     * </p>
     *
     * @param groupName the name of the group to which the user will be added
     * @param username the username of the user to be added to the group
     * @throws BusinessException if the user is already a member of the group
     *                           or if a data integrity violation occurs
     * @throws RuntimeException if any other unexpected error occurs during the operation
     */
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

    /**
     * Sends a group invitation to a user via Kafka event.
     * <p>
     * Validates that the requesting user is an admin of the group before sending
     * the invitation event. The invitation contains details about the group,
     * the invited user, and the user who sent the invitation.
     * </p>
     *
     * @param userId the ID of the user sending the invitation
     * @param invitationRequest contains the group name and username to invite
     * @throws BusinessException if the user is not an admin of the specified group
     */
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
                    membershipDto.setUserId(m.getUtente().getId());
                    membershipDto.setUsername(m.getUtente().getUsername());
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
     * <p>
     * Only allows deletion if the group has exactly one member and the requesting
     * user is that member. Prevents deletion of groups with multiple members.
     * </p>
     *
     * @param groupName the name of the group to delete
     * @param userId the ID of the user requesting deletion
     * @throws BusinessException if group has multiple members or user is not a member
     */
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

    /**
     * Retrieves all groups that a user is member of.
     *
     * @param username the username to search groups for
     * @return List of GroupDto containing the user's groups
     * @throws UserNotInGroup if the user is not a member of any groups
     */
    @Transactional
    public List<GroupDto> getGroupsByUsername(String username) {
        List<Group> groups = groupRepository.getGroupsByUsername(username);
        if (groups.isEmpty()) {
            throw new UserNotInGroup("l'utente non presente in nessun gruppo");
        }
        return groups.stream()
                .map(this::mapToDto)
                .toList();
    }
}