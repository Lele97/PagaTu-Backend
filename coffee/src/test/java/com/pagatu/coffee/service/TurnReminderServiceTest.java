package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnReminderServiceTest {

    @Mock
    private UserGroupMembershipRepository userGroupMembershipRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TurnReminderService turnReminderService;

    private UserGroupMembership membership;
    private CoffeeUser user;
    private Group group;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(turnReminderService, "turnReminderSubject", "turn-reminder");

        user = new CoffeeUser();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        group = new Group();
        group.setName("Caffe Team");

        membership = new UserGroupMembership();
        membership.setCoffeeUser(user);
        membership.setGroup(group);
        membership.setMyTurn(true);
        membership.setReminderLevel(0);
        membership.setTurnAssignedAt(LocalDateTime.now().minusHours(25));
    }

    @Test
    void processTurnReminders_skipsWhenEmailRemindersDisabled() {
        user.setEmailTurnReminders(false);
        when(userGroupMembershipRepository.findAllWithActiveTurn()).thenReturn(List.of(membership));
        when(userGroupMembershipRepository.save(any())).thenReturn(membership);

        turnReminderService.processTurnReminders();

        verify(outboxService, never()).saveEvent(anyString(), any());
    }

    @Test
    void processTurnReminders_sendsWhenEmailRemindersEnabled() {
        user.setEmailTurnReminders(true);
        when(userGroupMembershipRepository.findAllWithActiveTurn()).thenReturn(List.of(membership));
        when(userGroupMembershipRepository.save(any())).thenReturn(membership);

        turnReminderService.processTurnReminders();

        verify(outboxService).saveEvent(eq("turn-reminder"), any());
    }
}