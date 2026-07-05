package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.UserMembershipDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NewGroupRequest;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.InvitationEvent;
import com.pagatu.coffee.event.InvitationResponseEvent;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupMembershipRepository userGroupMembershipRepository;

    @Mock
    private InvitationUserToGroupInformationRepository invitationUserToGroupInformationRepository;

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private MembershipDtoFactory membershipDtoFactory;

    @InjectMocks
    private GroupService groupService;

    private CoffeeUser testUser;
    private Group testGroup;
    private UserGroupMembership testMembership;

    @BeforeEach
    void setUp() {
        testUser = new CoffeeUser();
        testUser.setId(1L);
        testUser.setAuthId(123L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setName("testgroup");
        testGroup.setDescription("Test group");
        testGroup.setUserMemberships(new ArrayList<>());

        testMembership = new UserGroupMembership();
        testMembership.setId(1L);
        testMembership.setCoffeeUser(testUser);
        testMembership.setGroup(testGroup);
        testMembership.setStatus(PaymentStatus.NON_PAGATO);
        testMembership.setIsAdmin(true);
        testMembership.setMyTurn(true);
        testMembership.setJoinedAt(LocalDateTime.now());

        ReflectionTestUtils.setField(groupService, "natsSubject", "invitation-subject");
        ReflectionTestUtils.setField(groupService, "natsSubjectsInvitationResponse", "invitation-response-subject");

        lenient().when(membershipDtoFactory.toDto(any(Group.class), any(UserGroupMembership.class)))
                .thenAnswer(inv -> {
                    UserGroupMembership m = inv.getArgument(1);
                    UserMembershipDto dto = new UserMembershipDto();
                    dto.setUsername(m.getCoffeeUser().getUsername());
                    dto.setStatus(m.getStatus());
                    dto.setMyTurn(m.getMyTurn());
                    dto.setIsAdmin(m.getIsAdmin());
                    dto.setRoundSkipCount(m.getRoundSkipCount());
                    if (m.getRoundSkipCount() != null && inv.getArgument(0, Group.class).getMaxSkipPerRound() != null) {
                        dto.setSkipsRemaining(inv.getArgument(0, Group.class).getMaxSkipPerRound() - m.getRoundSkipCount());
                    }
                    return dto;
                });
    }

    @Test
    void createGroup_WhenGroupDoesNotExist_ShouldCreateGroup() {
        // Given
        NewGroupRequest request = new NewGroupRequest();
        request.setName("newgroup");
        request.setDescription("New group description");
        
        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(groupRepository.getGroupByName("newgroup")).thenReturn(Optional.empty());
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            group.setId(2L);
            return group;
        });

        // When
        GroupDto result = groupService.createGroup(request, 123L);

        // Then
        assertNotNull(result);
        assertEquals("newgroup", result.getName());
        assertEquals("New group description", result.getDescription());
        assertEquals(1, result.getUserMembershipsdto().size());
        
        verify(baseUserService).findUserByAuthId(123L);
        verify(groupRepository).getGroupByName("newgroup");
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void createGroup_WhenGroupAlreadyExists_ShouldThrowBusinessException() {
        // Given
        NewGroupRequest request = new NewGroupRequest();
        request.setName("existinggroup");
        request.setDescription("Existing group");
        
        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(groupRepository.getGroupByName("existinggroup")).thenReturn(Optional.of(testGroup));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> groupService.createGroup(request, 123L));
        
        assertEquals("Il gruppo esiste già: existinggroup", exception.getMessage());
        verify(baseUserService).findUserByAuthId(123L);
        verify(groupRepository).getGroupByName("existinggroup");
        verify(groupRepository, never()).save(any(Group.class));
    }

    @Test
    void addUserToGroup_WhenUserNotMemberAndInvitationValid_ShouldAddUser() {
        // Given
        String groupName = "testgroup";
        String username = "newuser";
        Long invitationId = 1L;
        
        CoffeeUser newUser = new CoffeeUser();
        newUser.setId(2L);
        newUser.setAuthId(456L);
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        
        InvitationUserToGroupInformation invitation = new InvitationUserToGroupInformation();
        invitation.setId(invitationId);
        invitation.setUserWhoSentInvitation(123L);
        invitation.setGroupName(groupName);
        invitation.setUsername(username);
        invitation.setExpiredDate(LocalDateTime.now().plusDays(7));
        invitation.setInvitationStatus(InvitationStatus.ACTIVE);
        
        when(invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId))
            .thenReturn(Optional.of(invitation));
        when(baseUserService.findUserByUsername(username)).thenReturn(newUser);
        when(baseUserService.findUserByAuthId(456L)).thenReturn(newUser);
        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName(groupName)).thenReturn(testGroup);
        when(userGroupMembershipRepository.existsByCoffeeUserAndGroup(newUser, testGroup)).thenReturn(false);
        when(userGroupMembershipRepository.save(any(UserGroupMembership.class))).thenAnswer(invocation -> {
            UserGroupMembership membership = invocation.getArgument(0);
            membership.setId(2L);
            return membership;
        });

        // When
        groupService.addUserToGroup(groupName, username, invitationId, 456L);

        // Then
        verify(invitationUserToGroupInformationRepository).findByIdWithStatusActive(invitationId);
        verify(baseUserService).findUserByUsername(username);
        verify(baseUserService).findUserByAuthId(123L);
        verify(baseUserService).findGroupByName(groupName);
        verify(userGroupMembershipRepository).existsByCoffeeUserAndGroup(newUser, testGroup);
        verify(userGroupMembershipRepository).save(any(UserGroupMembership.class));
        verify(outboxService).saveEvent(anyString(), any(InvitationResponseEvent.class));
        
        // Verify invitation was updated
        assertEquals(InvitationStatus.ACCEPTED, invitation.getInvitationStatus());
        assertNotNull(invitation.getUsedAt());
    }

    @Test
    void addUserToGroup_WhenUserAlreadyMember_ShouldThrowBusinessException() {
        // Given
        String groupName = "testgroup";
        String username = "existinguser";
        Long invitationId = 1L;
        
        CoffeeUser existingUser = new CoffeeUser();
        existingUser.setId(2L);
        existingUser.setAuthId(123L);
        existingUser.setUsername("existinguser");
        
        InvitationUserToGroupInformation invitation = new InvitationUserToGroupInformation();
        invitation.setId(invitationId);
        invitation.setUserWhoSentInvitation(123L);
        invitation.setGroupName(groupName);
        invitation.setUsername(username);
        invitation.setExpiredDate(LocalDateTime.now().plusDays(7));
        invitation.setInvitationStatus(InvitationStatus.ACTIVE);
        
        when(invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId))
            .thenReturn(Optional.of(invitation));
        when(baseUserService.findUserByUsername(username)).thenReturn(existingUser);
        when(baseUserService.findUserByAuthId(123L)).thenReturn(existingUser);
        when(baseUserService.findGroupByName(groupName)).thenReturn(testGroup);
        when(userGroupMembershipRepository.existsByCoffeeUserAndGroup(existingUser, testGroup)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> groupService.addUserToGroup(groupName, username, invitationId, 123L));
        
        assertEquals("L'utente 'existinguser' è già membro del gruppo 'testgroup'", exception.getMessage());
        verify(userGroupMembershipRepository).existsByCoffeeUserAndGroup(existingUser, testGroup);
        verify(userGroupMembershipRepository, never()).save(any(UserGroupMembership.class));
    }

    @Test
    void sendInvitationToGroup_WhenUserIsAdmin_ShouldSendInvitation() {
        // Given
        Long userId = 123L;
        InvitationRequest request = new InvitationRequest();
        request.setGroupName("testgroup");
        request.setUsername("invitee");
        
        CoffeeUser inviteeUser = new CoffeeUser();
        inviteeUser.setId(2L);
        inviteeUser.setUsername("invitee");
        inviteeUser.setEmail("invitee@example.com");
        
        testGroup.getUserMemberships().add(testMembership); // User is admin
        
        InvitationUserToGroupInformation savedInvitation = new InvitationUserToGroupInformation();
        savedInvitation.setId(1L);
        savedInvitation.setUserWhoSentInvitation(userId);
        savedInvitation.setGroupName("testgroup");
        savedInvitation.setUsername("invitee");
        savedInvitation.setEmail("invitee@example.com");
        savedInvitation.setCreatedAt(LocalDateTime.now());
        savedInvitation.setExpiredDate(LocalDateTime.now().plusMinutes(60));
        savedInvitation.setInvitationStatus(InvitationStatus.ACTIVE);
        
        when(baseUserService.findGroupWithMembershipsByName("testgroup")).thenReturn(testGroup);
        when(baseUserService.findUserByUsername("invitee")).thenReturn(inviteeUser);
        when(baseUserService.findUserByAuthId(userId)).thenReturn(testUser);
        when(userGroupMembershipRepository.existsByCoffeeUserAndGroup(inviteeUser, testGroup)).thenReturn(false);
        when(invitationUserToGroupInformationRepository.save(any(InvitationUserToGroupInformation.class)))
            .thenReturn(savedInvitation);

        // When
        groupService.sendInvitationToGroup(userId, request);

        // Then
        verify(baseUserService).findGroupWithMembershipsByName("testgroup");
        verify(baseUserService).findUserByUsername("invitee");
        verify(baseUserService).findUserByAuthId(userId);
        verify(invitationUserToGroupInformationRepository).save(any(InvitationUserToGroupInformation.class));
        verify(outboxService).saveEvent(anyString(), any(InvitationEvent.class));
    }

    @Test
    void sendInvitationToGroup_WhenUserIsNotAdmin_ShouldThrowBusinessException() {
        // Given
        Long userId = 123L;
        InvitationRequest request = new InvitationRequest();
        request.setGroupName("testgroup");
        request.setUsername("invitee");
        
        CoffeeUser inviteeUser = new CoffeeUser();
        inviteeUser.setId(2L);
        inviteeUser.setUsername("invitee");
        inviteeUser.setEmail("invitee@example.com");
        
        // User is not admin
        testMembership.setIsAdmin(false);
        testGroup.getUserMemberships().add(testMembership);
        
        when(baseUserService.findGroupWithMembershipsByName("testgroup")).thenReturn(testGroup);
        when(baseUserService.findUserByUsername("invitee")).thenReturn(inviteeUser);
        when(baseUserService.findUserByAuthId(userId)).thenReturn(testUser);

        // When & Then
        ForbiddenException exception = assertThrows(ForbiddenException.class, 
            () -> groupService.sendInvitationToGroup(userId, request));
        
        assertEquals("Non sei admin del gruppo 'testgroup'", exception.getMessage());
        verify(invitationUserToGroupInformationRepository, never()).save(any(InvitationUserToGroupInformation.class));
        verify(outboxService, never()).saveEvent(anyString(), any(InvitationEvent.class));
    }

    @Test
    void deleteGroupByName_WhenUserIsOnlyMember_ShouldDeleteGroup() {
        // Given
        String groupName = "testgroup";
        Long userId = 123L;
        
        testGroup.getUserMemberships().add(testMembership);
        
        when(baseUserService.findGroupByName(groupName)).thenReturn(testGroup);

        // When
        groupService.deleteGroupByName(groupName, userId);

        // Then
        verify(baseUserService).findGroupByName(groupName);
        verify(groupRepository).deleteGroupByName(groupName);
    }

    @Test
    void deleteGroupByName_WhenGroupHasMultipleMembers_ShouldThrowBusinessException() {
        // Given
        String groupName = "testgroup";
        Long userId = 123L;
        
        CoffeeUser anotherUser = new CoffeeUser();
        anotherUser.setId(2L);
        anotherUser.setAuthId(456L);
        
        UserGroupMembership anotherMembership = new UserGroupMembership();
        anotherMembership.setId(2L);
        anotherMembership.setCoffeeUser(anotherUser);
        anotherMembership.setGroup(testGroup);
        anotherMembership.setStatus(PaymentStatus.NON_PAGATO);
        anotherMembership.setIsAdmin(false);
        anotherMembership.setMyTurn(false);
        
        testGroup.getUserMemberships().add(testMembership);
        testGroup.getUserMemberships().add(anotherMembership);
        
        when(baseUserService.findGroupByName(groupName)).thenReturn(testGroup);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> groupService.deleteGroupByName(groupName, userId));
        
        assertTrue(exception.getMessage().contains("Impossibile eliminare il gruppo"));
        verify(baseUserService).findGroupByName(groupName);
        verify(groupRepository, never()).deleteGroupByName(groupName);
    }

    @Test
    void getGroupsByUsername_WhenUserHasGroups_ShouldReturnGroupDtos() {
        // Given
        String username = "testuser";
        
        List<Group> groups = new ArrayList<>();
        groups.add(testGroup);
        
        when(groupRepository.getGroupsByUsername(username)).thenReturn(groups);

        // When
        List<GroupDto> result = groupService.getGroupsByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testgroup", result.get(0).getName());
        verify(groupRepository).getGroupsByUsername(username);
    }

    @Test
    void addUserToGroup_WhenInvitationNotFound_ShouldThrowBusinessException() {
        // Given
        when(invitationUserToGroupInformationRepository.findByIdWithStatusActive(99L))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> groupService.addUserToGroup("testgroup", "testuser", 99L, 123L));

        assertEquals("Invito non trovato o non più attivo", exception.getMessage());
        verify(userGroupMembershipRepository, never()).save(any());
    }

    @Test
    void rejectInvitation_WhenInvitationNotFound_ShouldThrowBusinessException() {
        // Given
        when(invitationUserToGroupInformationRepository.findByIdWithStatusActive(99L))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> groupService.rejectInvitation("testgroup", "testuser", 99L, 123L));

        assertEquals("Invito non trovato o non più attivo", exception.getMessage());
        verify(outboxService, never()).saveEvent(anyString(), any());
    }

    @Test
    void rejectInvitation_WhenInvitationValid_ShouldRejectAndSendEvent() {
        // Given
        String groupName = "testgroup";
        String username = "testuser";
        Long invitationId = 1L;
        
        InvitationUserToGroupInformation invitation = new InvitationUserToGroupInformation();
        invitation.setId(invitationId);
        invitation.setUserWhoSentInvitation(123L);
        invitation.setGroupName(groupName);
        invitation.setUsername(username);
        invitation.setExpiredDate(LocalDateTime.now().plusDays(7));
        invitation.setInvitationStatus(InvitationStatus.ACTIVE);
        
        when(invitationUserToGroupInformationRepository.findByIdWithStatusActive(invitationId))
            .thenReturn(Optional.of(invitation));
        when(baseUserService.findUserByUsername(username)).thenReturn(testUser);
        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName(groupName)).thenReturn(testGroup);
        when(userGroupMembershipRepository.existsByCoffeeUserAndGroup(testUser, testGroup)).thenReturn(false);

        // When
        groupService.rejectInvitation(groupName, username, invitationId, 123L);

        // Then
        verify(invitationUserToGroupInformationRepository).findByIdWithStatusActive(invitationId);
        verify(baseUserService).findUserByUsername(username);
        verify(baseUserService, atLeast(2)).findUserByAuthId(123L);
        verify(baseUserService).findGroupByName(groupName);
        verify(outboxService).saveEvent(anyString(), any(InvitationResponseEvent.class));
        
        // Verify invitation was updated
        assertEquals(InvitationStatus.REJECTED, invitation.getInvitationStatus());
        assertNotNull(invitation.getUsedAt());
    }

    @Test
    void getGroupSummary_ShouldReturnTurnAndSkipInfo() {
        testGroup.setMaxSkipPerRound(2);
        testGroup.setCurrentRoundNumber(4);
        testMembership.setRoundSkipCount(1);
        testMembership.getCoffeeUser().setName("Mario");
        testGroup.getUserMemberships().add(testMembership);

        when(baseUserService.findGroupWithMembershipsByName("testgroup")).thenReturn(testGroup);

        GroupDto result = groupService.getGroupSummary("testgroup", 123L);

        assertEquals("testgroup", result.getName());
        assertEquals(1, result.getMemberCount());
        assertEquals("testuser", result.getCurrentTurnUsername());
        assertEquals(2, result.getMaxSkipPerRound());
        assertEquals(1, result.getRoundPendingCount());
        assertEquals(4, result.getCurrentRoundNumber());
        assertEquals(4, result.getMaxSkipPerMonth());

        UserMembershipDto member = result.getUserMembershipsdto().get(0);
        assertEquals(1, member.getRoundSkipCount());
        assertEquals(1, member.getSkipsRemaining());
        assertTrue(member.getMyTurn());
    }
}