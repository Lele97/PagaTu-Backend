package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupRulesDto;
import com.pagatu.coffee.dto.GroupRulesRequest;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRulesServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MembershipDtoFactory membershipDtoFactory;

    @InjectMocks
    private GroupRulesService groupRulesService;

    private static final Long ADMIN_ID = 100L;
    private static final Long MEMBER_ID = 200L;

    private Group group;

    @BeforeEach
    void setUp() {
        CoffeeUser admin = user(ADMIN_ID, "admin");
        CoffeeUser member = user(MEMBER_ID, "member");

        group = new Group();
        group.setName("Caffe Team");
        group.setPayForEnabled(true);
        group.setPayForAdminOnly(false);
        group.setUserMemberships(new ArrayList<>(List.of(
                membership(admin, true),
                membership(member, false))));
    }

    @Test
    void getRules_asMember_succeeds() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);

        GroupRulesDto result = groupRulesService.getRules(MEMBER_ID, "Caffe Team");

        assertEquals("Caffe Team", result.getGroupName());
        assertTrue(result.getPayForEnabled());
    }

    @Test
    void updateRules_asNonAdmin_throwsForbidden() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);

        GroupRulesRequest request = new GroupRulesRequest();
        request.setGroupName("Caffe Team");
        request.setPayForAdminOnly(true);

        assertThrows(ForbiddenException.class,
                () -> groupRulesService.updateRules(MEMBER_ID, request));
    }

    @Test
    void validateSkipAllowed_monthlyLimitReached_throws() {
        UserGroupMembership member = group.getUserMemberships().get(1);
        member.setMonthlySkipPeriod(java.time.YearMonth.now().toString());
        member.setMonthlySkipCount(4);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupRulesService.validateSkipAllowed(group, member));

        assertTrue(ex.getMessage().contains("4 skip al mese"));
    }

    @Test
    void validateNotLastUnpaidMember_onlyOneLeft_throws() {
        UserGroupMembership admin = group.getUserMemberships().get(0);
        UserGroupMembership member = group.getUserMemberships().get(1);
        admin.setStatus(PaymentStatus.PAGATO);
        member.setMyTurn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupRulesService.validateNotLastUnpaidMember(group, member));

        assertTrue(ex.getMessage().contains("qualcuno deve pagare"));
    }

    @Test
    void validatePayForAllowed_monthlyLimitReached_throws() {
        when(paymentRepository.countPayForByUserInGroupForMonth(
                eq("Caffe Team"), eq("admin"), anyInt(), anyInt())).thenReturn(4L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> groupRulesService.validatePayForAllowed(group, group.getUserMemberships().get(0).getCoffeeUser()));

        assertTrue(ex.getMessage().contains("paga per"));
    }

    @Test
    void updateRules_asAdmin_updatesRules() {
        when(baseUserService.findGroupWithMembershipsByName("Caffe Team")).thenReturn(group);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupRulesRequest request = new GroupRulesRequest();
        request.setGroupName("Caffe Team");
        request.setMaxSkipPerRound(3);
        request.setPayForAdminOnly(true);

        GroupRulesDto result = groupRulesService.updateRules(ADMIN_ID, request);

        assertEquals(3, result.getMaxSkipPerRound());
        assertTrue(result.getPayForAdminOnly());
    }

    private CoffeeUser user(Long authId, String username) {
        CoffeeUser user = new CoffeeUser();
        user.setAuthId(authId);
        user.setUsername(username);
        return user;
    }

    private UserGroupMembership membership(CoffeeUser user, boolean isAdmin) {
        UserGroupMembership m = new UserGroupMembership();
        m.setCoffeeUser(user);
        m.setGroup(group);
        m.setIsAdmin(isAdmin);
        m.setStatus(PaymentStatus.NON_PAGATO);
        return m;
    }
}