package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserAward;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserAwardRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwardServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private UserAwardRepository userAwardRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserGroupMembershipRepository membershipRepository;

    @InjectMocks
    private AwardService awardService;

    private CoffeeUser user;
    private UserGroupMembership membership;

    @BeforeEach
    void setUp() {
        user = new CoffeeUser();
        user.setId(1L);
        user.setAuthId(10L);
        user.setUsername("mario");

        Group group = new Group();
        group.setName("ufficio");

        membership = new UserGroupMembership();
        membership.setCoffeeUser(user);
        membership.setGroup(group);
        membership.setIsAdmin(true);
        membership.setPaymentStreak(3);
        membership.setPaymentCount(3);
        membership.setSkipCount(0);
    }

    @Test
    void grantGroupCreated_persistsOneAwardPerGroup() {
        when(userAwardRepository.existsByCoffeeUserAndCodeAndOccurrenceKey(user, AwardService.GRUPPO_CREATO, "ufficio"))
                .thenReturn(false);

        awardService.grantGroupCreated(user, "ufficio");

        ArgumentCaptor<UserAward> captor = ArgumentCaptor.forClass(UserAward.class);
        verify(userAwardRepository).save(captor.capture());
        assertEquals(AwardService.GRUPPO_CREATO, captor.getValue().getCode());
        assertEquals("ufficio", captor.getValue().getOccurrenceKey());
    }

    @Test
    void grantGroupCreated_skipsIfAlreadyStored() {
        when(userAwardRepository.existsByCoffeeUserAndCodeAndOccurrenceKey(user, AwardService.GRUPPO_CREATO, "bar"))
                .thenReturn(true);

        awardService.grantGroupCreated(user, "bar");

        verify(userAwardRepository, never()).save(any());
    }

    @Test
    void listAndSync_grantsRepeatableKingPerMonth() {
        Payment payment = new Payment();
        payment.setAmount(4.0);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setUserGroupMembership(membership);

        when(baseUserService.findUserByAuthId(10L)).thenReturn(user);
        when(paymentRepository.findWithUserGroupMembershipByCoffeeUserOrderByPaymentDateDesc(user))
                .thenReturn(List.of(payment));
        when(membershipRepository.findByCoffeeUserWithGroup(user)).thenReturn(List.of(membership));
        when(userAwardRepository.existsByCoffeeUserAndCodeAndOccurrenceKey(eq(user), any(), any()))
                .thenReturn(false);
        when(paymentRepository.findAllByGroupName("ufficio")).thenReturn(List.of(payment));
        when(userAwardRepository.findByCoffeeUserOrderByEarnedAtDesc(user)).thenReturn(List.of());

        awardService.listAndSync(10L);

        verify(userAwardRepository, atLeastOnce()).save(any(UserAward.class));
    }
}
