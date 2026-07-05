package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NewGroupRequest;
import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.InvitationEvent;
import com.pagatu.coffee.event.InvitationResponseEvent;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;

import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final PaymentService paymentService;
    private final MembershipDtoFactory membershipDtoFactory;

    private static final int INVITATION_VALIDITY_DAYS = 7;

    /**
     * @param outboxService                               transactional outbox for NATS events
     * @param groupRepository                             group persistence
     * @param userGroupMembershipRepository               membership management
     * @param invitationUserToGroupInformationRepository  invitation records
     * @param baseUserService                             shared user/group resolution
     * @param paymentService                              payment rotation logic
     */
    public GroupService(OutboxService outboxService,
                        GroupRepository groupRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository,
                        InvitationUserToGroupInformationRepository invitationUserToGroupInformationRepository,
                        BaseUserService baseUserService,
                        PaymentService paymentService,
                        MembershipDtoFactory membershipDtoFactory) {
        this.outboxService = outboxService;
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.invitationUserToGroupInformationRepository = invitationUserToGroupInformationRepository;
        this.baseUserService = baseUserService;
        this.paymentService = paymentService;
        this.membershipDtoFactory = membershipDtoFactory;
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
                    throw new BusinessException("Il gruppo esiste già: " + newGroupRequest.getName());
                });

        Group group = new Group();
        group.setName(newGroupRequest.getName());
        group.setDescription(newGroupRequest.getDescription());
        group.setCurrentRoundNumber(1);
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
    public void addUserToGroup(String groupName, String username, Long invitationId, Long requestingUserId) {

        InvitationUserToGroupInformation invitationUserToGroupInformation = validateInvitation(
                groupName, username, invitationId, requestingUserId);

        Long userWhoSentTheInvitation = invitationUserToGroupInformation.getUserWhoSentInvitation();
        invitationUserToGroupInformation.setUsedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.ACCEPTED);
        invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        CoffeeUser user = baseUserService.findUserByUsername(username);
        CoffeeUser userSendInvitation = baseUserService.findUserByAuthId(userWhoSentTheInvitation);
        Group group = baseUserService.findGroupByName(groupName);

        if (userGroupMembershipRepository.existsByCoffeeUserAndGroup(user, group))
            throw new BusinessException("L'utente '" + username + "' è già membro del gruppo '" + groupName + "'");

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
            throw new BusinessException("L'utente è già nel gruppo");
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

        if ((invitationRequest.getUsername() == null || invitationRequest.getUsername().isBlank())
                && (invitationRequest.getEmail() == null || invitationRequest.getEmail().isBlank())) {
            throw new BusinessException("Specificare username o email dell'utente da invitare");
        }

        Group group = baseUserService.findGroupWithMembershipsByName(invitationRequest.getGroupName());
        CoffeeUser coffeeUser = resolveInvitee(invitationRequest);
        CoffeeUser userWhoSentTheInvitation = baseUserService.findUserByAuthId(userId);

        assertAdmin(group, userId, invitationRequest.getGroupName());

        if (userGroupMembershipRepository.existsByCoffeeUserAndGroup(coffeeUser, group)) {
            throw new BusinessException("L'utente è già membro del gruppo '" + invitationRequest.getGroupName() + "'");
        }

        InvitationUserToGroupInformation invitationUserToGroupInformation = new InvitationUserToGroupInformation();
        invitationUserToGroupInformation.setUserWhoSentInvitation(userId);
        invitationUserToGroupInformation.setGroupName(invitationRequest.getGroupName());
        invitationUserToGroupInformation.setUsername(coffeeUser.getUsername());
        invitationUserToGroupInformation.setEmail(coffeeUser.getEmail());
        invitationUserToGroupInformation.setCreatedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setExpiredDate(LocalDateTime.now().plusDays(INVITATION_VALIDITY_DAYS));
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.ACTIVE);

        InvitationUserToGroupInformation savedInvitationUserToGroupInformation = invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        InvitationEvent event = new InvitationEvent();
        event.setUsername(coffeeUser.getUsername());
        event.setGroupName(group.getName());
        event.setEmail(coffeeUser.getEmail());
        event.setUserWhoSentTheInvitation(userWhoSentTheInvitation.getUsername());
        event.setInvitationId(savedInvitationUserToGroupInformation.getId());

        outboxService.saveEvent(natsSubject, event);

        log.info("Invito inviato a {} per il gruppo {}", coffeeUser.getUsername(),
                invitationRequest.getGroupName());
    }

    private CoffeeUser resolveInvitee(InvitationRequest invitationRequest) {
        if (invitationRequest.getEmail() != null && !invitationRequest.getEmail().isBlank()) {
            return baseUserService.findUserByEmail(invitationRequest.getEmail().trim());
        }
        return baseUserService.findUserByUsername(invitationRequest.getUsername().trim());
    }

    /**
     * Returns a rich group summary for any member of the group.
     */
    @Transactional
    public GroupDto getGroupSummary(String groupName, Long userId) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        assertMember(group, userId, groupName);
        return mapToDto(group);
    }

    private GroupDto mapToDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setId(group.getId());
        groupDto.setName(group.getName());
        groupDto.setDescription(group.getDescription());
        groupDto.setMaxSkipPerRound(group.getMaxSkipPerRound());
        groupDto.setPayForEnabled(group.getPayForEnabled());
        groupDto.setPayForAdminOnly(group.getPayForAdminOnly());

        List<UserMembershipDto> membershipDtos = group.getUserMemberships().stream()
                .sorted(Comparator
                        .comparing((UserGroupMembership m) -> !Boolean.TRUE.equals(m.getMyTurn()))
                        .thenComparing((UserGroupMembership m) -> !Boolean.TRUE.equals(m.getIsAdmin()))
                        .thenComparing(m -> m.getJoinedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> membershipDtoFactory.toDto(group, m))
                .toList();

        groupDto.setUserMembershipsdto(membershipDtos);
        groupDto.setMemberCount(membershipDtos.size());
        groupDto.setCurrentRoundNumber(group.getCurrentRoundNumber() != null ? group.getCurrentRoundNumber() : 1);
        groupDto.setMaxSkipPerMonth(MembershipDtoFactory.MAX_SKIP_PER_MONTH);
        groupDto.setMaxPayForPerMonth(MembershipDtoFactory.MAX_PAYFOR_PER_MONTH);

        membershipDtos.stream()
                .filter(m -> Boolean.TRUE.equals(m.getMyTurn()))
                .findFirst()
                .ifPresent(m -> groupDto.setCurrentTurnUsername(m.getUsername()));

        int paid = (int) membershipDtos.stream()
                .filter(m -> PaymentStatus.PAGATO.equals(m.getStatus()))
                .count();
        int pending = (int) membershipDtos.stream()
                .filter(m -> PaymentStatus.NON_PAGATO.equals(m.getStatus()))
                .count();
        groupDto.setRoundPaidCount(paid);
        groupDto.setRoundPendingCount(pending);

        return groupDto;
    }

    private void assertMember(Group group, Long userId, String groupName) {
        boolean isMember = group.getUserMemberships().stream()
                .anyMatch(m -> m.getCoffeeUser().getAuthId().equals(userId));
        if (!isMember) {
            throw new ForbiddenException("Non sei membro del gruppo '" + groupName + "'");
        }
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
                    "Impossibile eliminare il gruppo '" + groupName + "': ha più di un membro oppure non ne fai parte");
        }
    }

    @Transactional
    public void leaveGroup(String groupName, Long userId) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        CoffeeUser user = baseUserService.findUserByAuthId(userId);

        UserGroupMembership membership = userGroupMembershipRepository.findByCoffeeUserAndGroup(user, group)
                .orElseThrow(() -> new BusinessException("Non sei membro del gruppo '" + groupName + "'"));

        boolean hadTurn = Boolean.TRUE.equals(membership.getMyTurn());
        boolean wasAdmin = Boolean.TRUE.equals(membership.getIsAdmin());
        int remainingCount = group.getUserMemberships().size() - 1;

        if (remainingCount == 0) {
            groupRepository.deleteGroupByName(groupName);
            log.info("Gruppo '{}' eliminato: ultimo membro uscito", groupName);
            return;
        }

        userGroupMembershipRepository.delete(membership);

        if (wasAdmin) {
            promoteNewAdmin(group, userId);
        }

        if (hadTurn) {
            Group refreshedGroup = baseUserService.findGroupByName(groupName);
            paymentService.reassignTurnAfterMemberRemoval(refreshedGroup);
        }

        log.info("Utente {} ha lasciato il gruppo {}", user.getUsername(), groupName);
    }

    @Transactional
    public void removeMember(Long adminUserId, String groupName, String memberUsername) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        assertAdmin(group, adminUserId, groupName);

        CoffeeUser member = baseUserService.findUserByUsername(memberUsername);
        CoffeeUser admin = baseUserService.findUserByAuthId(adminUserId);

        if (member.getAuthId().equals(adminUserId)) {
            throw new BusinessException("Per uscire dal gruppo usa l'endpoint dedicato");
        }

        UserGroupMembership membership = userGroupMembershipRepository.findByCoffeeUserAndGroup(member, group)
                .orElseThrow(() -> new BusinessException("L'utente non è membro del gruppo"));

        boolean hadTurn = Boolean.TRUE.equals(membership.getMyTurn());
        boolean wasAdmin = Boolean.TRUE.equals(membership.getIsAdmin());

        userGroupMembershipRepository.delete(membership);

        if (wasAdmin) {
            promoteNewAdmin(group, member.getAuthId());
        }

        if (hadTurn) {
            Group refreshedGroup = baseUserService.findGroupByName(groupName);
            paymentService.reassignTurnAfterMemberRemoval(refreshedGroup);
        }

        log.info("Admin {} ha rimosso {} dal gruppo {}", admin.getUsername(), memberUsername, groupName);
    }

    @Transactional
    public void transferAdmin(Long currentAdminId, String groupName, String newAdminUsername) {
        Group group = baseUserService.findGroupWithMembershipsByName(groupName);
        assertAdmin(group, currentAdminId, groupName);

        CoffeeUser newAdmin = baseUserService.findUserByUsername(newAdminUsername);
        if (newAdmin.getAuthId().equals(currentAdminId)) {
            throw new BusinessException("Sei già admin del gruppo");
        }

        UserGroupMembership newAdminMembership = userGroupMembershipRepository
                .findByCoffeeUserAndGroup(newAdmin, group)
                .orElseThrow(() -> new BusinessException("Il nuovo admin deve essere membro del gruppo"));

        for (UserGroupMembership m : group.getUserMemberships()) {
            if (m.getCoffeeUser().getAuthId().equals(currentAdminId)) {
                m.setIsAdmin(false);
            }
            if (m.getCoffeeUser().getAuthId().equals(newAdmin.getAuthId())) {
                m.setIsAdmin(true);
            }
        }
        userGroupMembershipRepository.saveAll(group.getUserMemberships());
        log.info("Admin del gruppo {} trasferito a {}", groupName, newAdminUsername);
    }

    private void promoteNewAdmin(Group group, Long excludedAuthId) {
        group.getUserMemberships().stream()
                .filter(m -> !m.getCoffeeUser().getAuthId().equals(excludedAuthId))
                .min(Comparator.comparing(UserGroupMembership::getJoinedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .ifPresent(m -> {
                    m.setIsAdmin(true);
                    userGroupMembershipRepository.save(m);
                });
    }

    private void assertAdmin(Group group, Long userId, String groupName) {
        boolean isAdmin = group.getUserMemberships().stream()
                .anyMatch(membership -> membership.getCoffeeUser() != null &&
                        membership.getCoffeeUser().getAuthId() != null &&
                        membership.getCoffeeUser().getAuthId().equals(userId) &&
                        Boolean.TRUE.equals(membership.getIsAdmin()));

        if (!isAdmin) {
            throw new ForbiddenException("Non sei admin del gruppo '" + groupName + "'");
        }
    }

    private InvitationUserToGroupInformation validateInvitation(
            String groupName, String username, Long invitationId, Long requestingUserId) {

        InvitationUserToGroupInformation invitation =
                invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId)
                        .orElseThrow(() -> new BusinessException("Invito non trovato o non più attivo"));

        if (invitation.getExpiredDate() != null && invitation.getExpiredDate().isBefore(LocalDateTime.now())) {
            invitation.setInvitationStatus(InvitationStatus.EXPIRED);
            invitationUserToGroupInformationRepository.save(invitation);
            throw new BusinessException("Invito scaduto");
        }

        if (!groupName.equals(invitation.getGroupName())) {
            throw new BusinessException("Il gruppo non corrisponde all'invito");
        }

        CoffeeUser requestingUser = baseUserService.findUserByAuthId(requestingUserId);
        if (!username.equals(requestingUser.getUsername())) {
            throw new ForbiddenException("Non puoi accettare un invito per un altro utente");
        }

        if (!username.equals(invitation.getUsername())) {
            throw new BusinessException("L'invito non è destinato a questo utente");
        }

        return invitation;
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
    public void rejectInvitation(String groupName, String username, Long invitationId, Long requestingUserId) {

        InvitationUserToGroupInformation invitationUserToGroupInformation = validateInvitation(
                groupName, username, invitationId, requestingUserId);

        Long userWhoSentTheInvitation = invitationUserToGroupInformation.getUserWhoSentInvitation();
        invitationUserToGroupInformation.setUsedAt(LocalDateTime.now());
        invitationUserToGroupInformation.setInvitationStatus(InvitationStatus.REJECTED);
        invitationUserToGroupInformationRepository.save(invitationUserToGroupInformation);

        CoffeeUser user = baseUserService.findUserByUsername(username);
        CoffeeUser userSendInvitation = baseUserService.findUserByAuthId(userWhoSentTheInvitation);
        Group group = baseUserService.findGroupByName(groupName);

        if (userGroupMembershipRepository.existsByCoffeeUserAndGroup(user, group))
            throw new BusinessException("L'utente '" + username + "' è già membro del gruppo '" + groupName + "'");


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