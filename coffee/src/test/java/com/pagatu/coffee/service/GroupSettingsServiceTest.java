package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupSettingsDto;
import com.pagatu.coffee.dto.GroupSettingsRequest;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.InvitationUserToGroupInformationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupSettingsServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private InvitationUserToGroupInformationRepository invitationRepository;

    @InjectMocks
    private GroupSettingsService groupSettingsService;

    private static final Long ADMIN_ID = 100L;
    private static final Long MEMBER_ID = 200L;

    private Group group;
    private CoffeeUser admin;
    private CoffeeUser member;

    @BeforeEach
    void setUp() {
        admin = user(ADMIN_ID, "admin");
        member = user(MEMBER_ID, "member");

        group = new Group();
        group.setId(1L);
        group.setName("Caffe Team");
        group.setDescription("Gruppo ufficio");
        group.setPayForEnabled(true);
        group.setPayForAdminOnly(false);
        group.setMaxSkipPerRound(2);
        group.setUserMemberships(new ArrayList<>(List.of(
                membership(admin, true, true),
                membership(member, false, false))));
    }

    @Test
    void getSettings_asAdmin_returnsFullSettings() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);

        GroupSettingsDto result = groupSettingsService.getSettings(ADMIN_ID, "Caffe Team");

        assertEquals("Caffe Team", result.getName());
        assertEquals("Gruppo ufficio", result.getDescription());
        assertEquals(2, result.getMaxSkipPerRound());
        assertTrue(result.getPayForEnabled());
        assertEquals(2, result.getMembers().size());
        assertTrue(result.getMembers().get(0).getIsAdmin());
    }

    @Test
    void getSettings_asNonAdmin_throwsForbidden() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);

        assertThrows(ForbiddenException.class,
                () -> groupSettingsService.getSettings(MEMBER_ID, "Caffe Team"));
    }

    @Test
    void updateSettings_renameGroup_updatesPendingInvitations() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);
        when(groupRepository.getGroupByName("Nuovo Caffe")).thenReturn(Optional.empty());
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationUserToGroupInformation invitation = new InvitationUserToGroupInformation();
        invitation.setId(10L);
        invitation.setGroupName("Caffe Team");
        invitation.setInvitationStatus(InvitationStatus.ACTIVE);
        when(invitationRepository.findActiveByGroupName("Caffe Team")).thenReturn(List.of(invitation));

        GroupSettingsRequest request = new GroupSettingsRequest();
        request.setCurrentGroupName("Caffe Team");
        request.setNewGroupName("Nuovo Caffe");

        GroupSettingsDto result = groupSettingsService.updateSettings(ADMIN_ID, request);

        assertEquals("Nuovo Caffe", result.getName());
        assertEquals("Nuovo Caffe", invitation.getGroupName());
        verify(invitationRepository).saveAll(List.of(invitation));
    }

    @Test
    void updateSettings_duplicateName_throwsBusinessException() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);
        when(groupRepository.getGroupByName("Esistente")).thenReturn(Optional.of(new Group()));

        GroupSettingsRequest request = new GroupSettingsRequest();
        request.setCurrentGroupName("Caffe Team");
        request.setNewGroupName("Esistente");

        assertThrows(BusinessException.class,
                () -> groupSettingsService.updateSettings(ADMIN_ID, request));
    }

    @Test
    void updateSettings_rulesAndDescription() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupSettingsRequest request = new GroupSettingsRequest();
        request.setCurrentGroupName("Caffe Team");
        request.setDescription("Nuova descrizione");
        request.setMaxSkipPerRound(5);
        request.setPayForEnabled(false);
        request.setPayForAdminOnly(true);

        GroupSettingsDto result = groupSettingsService.updateSettings(ADMIN_ID, request);

        assertEquals("Nuova descrizione", result.getDescription());
        assertEquals(5, result.getMaxSkipPerRound());
        assertFalse(result.getPayForEnabled());
        assertTrue(result.getPayForAdminOnly());
    }

    @Test
    void updateSettings_asNonAdmin_throwsForbidden() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);

        GroupSettingsRequest request = new GroupSettingsRequest();
        request.setCurrentGroupName("Caffe Team");
        request.setDescription("Nuova descrizione");

        assertThrows(ForbiddenException.class,
                () -> groupSettingsService.updateSettings(MEMBER_ID, request));
    }

    private CoffeeUser user(Long authId, String username) {
        CoffeeUser user = new CoffeeUser();
        user.setId(authId);
        user.setAuthId(authId);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        return user;
    }

    private UserGroupMembership membership(CoffeeUser user, boolean isAdmin, boolean myTurn) {
        UserGroupMembership m = new UserGroupMembership();
        m.setCoffeeUser(user);
        m.setGroup(group);
        m.setIsAdmin(isAdmin);
        m.setMyTurn(myTurn);
        m.setStatus(PaymentStatus.NON_PAGATO);
        m.setJoinedAt(LocalDateTime.now());
        return m;
    }
}