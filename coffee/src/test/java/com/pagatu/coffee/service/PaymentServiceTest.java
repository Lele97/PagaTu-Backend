package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupPaymentRankingDto;
import com.pagatu.coffee.dto.GroupPaymentRankingRequest;
import com.pagatu.coffee.dto.PaymentDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.NextPaymentEvent;
import com.pagatu.coffee.event.PayForEvent;
import com.pagatu.coffee.event.SkipPaymentEvent;

import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.NoContentAvailableException;
import com.pagatu.coffee.mapper.PaymentMapper;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import com.pagatu.coffee.repository.PaymentRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private UserGroupMembershipRepository userGroupMembershipRepository;

    @Mock
    private CoffeeUserRepository coffeeUserRepository;

    @Mock
    private BaseUserService baseUserService;

    @InjectMocks
    private PaymentService paymentService;

    private CoffeeUser testUser;
    private Group testGroup;
    private UserGroupMembership testMembership;
    private Payment savedPayment;
    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "natsSubjectNextPayment", "next-payment");
        ReflectionTestUtils.setField(paymentService, "natsSubjectSkipPayment", "skip-payment");
        ReflectionTestUtils.setField(paymentService, "natsSubjectPayFor", "pay-for-topic");

        testUser = new CoffeeUser();
        testUser.setId(1L);
        testUser.setAuthId(123L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testGroup = new Group();
        testGroup.setId(10L);
        testGroup.setName("testgroup");

        testMembership = new UserGroupMembership();
        testMembership.setId(1L);
        testMembership.setCoffeeUser(testUser);
        testMembership.setGroup(testGroup);
        testMembership.setStatus(PaymentStatus.NON_PAGATO);
        testMembership.setMyTurn(true);

        savedPayment = new Payment();
        savedPayment.setId(100L);
        savedPayment.setUserGroupMembership(testMembership);
        savedPayment.setAmount(2.5);
        savedPayment.setDescription("Caffè");
        savedPayment.setPaymentDate(LocalDateTime.now());

        paymentDto = new PaymentDto();
        paymentDto.setId(100L);
        paymentDto.setAmount(2.5);
        paymentDto.setDescription("Caffè");
    }

    @Test
    void payFor_WhenUserPaysForFriend_ShouldMarkBothPaidAndPublishNextPaymentEvent() {
        // Given
        NewPaymentRequest request = new NewPaymentRequest(3.0, "Caffè per amico");

        CoffeeUser friendUser = new CoffeeUser();
        friendUser.setId(2L);
        friendUser.setAuthId(456L);
        friendUser.setUsername("frienduser");
        friendUser.setEmail("friend@example.com");

        UserGroupMembership friendMembership = new UserGroupMembership();
        friendMembership.setId(2L);
        friendMembership.setCoffeeUser(friendUser);
        friendMembership.setGroup(testGroup);
        friendMembership.setStatus(PaymentStatus.NON_PAGATO);
        friendMembership.setMyTurn(true);

        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findUserByAuthId(456L)).thenReturn(friendUser);
        when(baseUserService.findGroupByName("testgroup")).thenReturn(testGroup);
        when(userGroupMembershipRepository.findByGroup(testGroup))
                .thenReturn(List.of(testMembership, friendMembership));
        when(userGroupMembershipRepository.findUserTurn("testgroup")).thenReturn(friendMembership);
        when(userGroupMembershipRepository.save(any(UserGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(userGroupMembershipRepository.findByGroupAndStatus(testGroup, PaymentStatus.NON_PAGATO))
                .thenReturn(Collections.emptyList());
        when(userGroupMembershipRepository.saveAll(anyList()))
                .thenReturn(List.of(testMembership, friendMembership));
        when(paymentMapper.toDto(savedPayment)).thenReturn(paymentDto);

        // When
        PaymentDto result = paymentService.payFor(123L, "testgroup", request);

        // Then
        assertNotNull(result);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(3.0, paymentCaptor.getValue().getAmount());
        assertEquals("Caffè per amico", paymentCaptor.getValue().getDescription());

        verify(userGroupMembershipRepository).findUserTurn("testgroup");
        verify(userGroupMembershipRepository, atLeast(2)).save(any(UserGroupMembership.class));
        verify(outboxService).saveEvent(eq("next-payment"), any(NextPaymentEvent.class));

        ArgumentCaptor<PayForEvent> payForCaptor = ArgumentCaptor.forClass(PayForEvent.class);
        verify(outboxService).saveEvent(eq("pay-for-topic"), payForCaptor.capture());
        PayForEvent payForEvent = payForCaptor.getValue();
        assertEquals("testuser", payForEvent.getPayerUsername());
        assertEquals("test@example.com", payForEvent.getPayerEmail());
        assertEquals("frienduser", payForEvent.getFriendUsername());
        assertEquals("friend@example.com", payForEvent.getFriendEmail());
        assertEquals("testgroup", payForEvent.getGroupName());
        assertEquals(2.5, payForEvent.getAmount());

        // Dopo payFor, determineNextPayer avvia un nuovo giro resettando i membri
        assertEquals(PaymentStatus.NON_PAGATO, testMembership.getStatus());
        assertEquals(PaymentStatus.NON_PAGATO, friendMembership.getStatus());
        assertTrue(testMembership.getMyTurn() || friendMembership.getMyTurn());
    }

    @Test
    void registerPayment_WhenNotUserTurn_ShouldThrowBusinessException() {
        testMembership.setMyTurn(false);
        NewPaymentRequest request = new NewPaymentRequest(2.5, "Colazione");

        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName("testgroup")).thenReturn(testGroup);
        when(userGroupMembershipRepository.findByGroup(testGroup))
                .thenReturn(List.of(testMembership));

        assertThrows(BusinessException.class,
                () -> paymentService.registerPayment(123L, "testgroup", request));
    }

    @Test
    void registerPayment_WhenUserIsMember_ShouldMarkPaidAndPublishEvent() {
        // Given
        NewPaymentRequest request = new NewPaymentRequest(2.5, "Caffè");
        CoffeeUser otherUser = new CoffeeUser();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        UserGroupMembership otherMembership = new UserGroupMembership();
        otherMembership.setCoffeeUser(otherUser);
        otherMembership.setGroup(testGroup);
        otherMembership.setStatus(PaymentStatus.NON_PAGATO);
        otherMembership.setMyTurn(false);

        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName("testgroup")).thenReturn(testGroup);
        when(userGroupMembershipRepository.findByGroup(testGroup))
                .thenReturn(List.of(testMembership, otherMembership));
        when(userGroupMembershipRepository.save(any(UserGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(userGroupMembershipRepository.findByGroupAndStatus(testGroup, PaymentStatus.NON_PAGATO))
                .thenReturn(List.of(otherMembership));
        when(paymentMapper.toDto(savedPayment)).thenReturn(paymentDto);

        // When
        PaymentDto result = paymentService.registerPayment(123L, "testgroup", request);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(PaymentStatus.PAGATO, testMembership.getStatus());
        assertFalse(testMembership.getMyTurn());

        verify(paymentRepository).save(any(Payment.class));
        verify(outboxService).saveEvent(eq("next-payment"), any(NextPaymentEvent.class));
    }

    @Test
    void skipPayment_WhenUserIsMember_ShouldMarkSkippedAndPublishEvent() {
        // Given
        CoffeeUser otherUser = new CoffeeUser();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        UserGroupMembership otherMembership = new UserGroupMembership();
        otherMembership.setCoffeeUser(otherUser);
        otherMembership.setGroup(testGroup);
        otherMembership.setStatus(PaymentStatus.NON_PAGATO);

        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName("testgroup")).thenReturn(testGroup);
        when(userGroupMembershipRepository.findByGroup(testGroup))
                .thenReturn(List.of(testMembership, otherMembership));
        when(userGroupMembershipRepository.save(any(UserGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupMembershipRepository.findByGroupAndStatus(testGroup, PaymentStatus.NON_PAGATO))
                .thenReturn(List.of(otherMembership));

        // When
        paymentService.skipPayment(123L, "testgroup");

        // Then
        assertEquals(PaymentStatus.SALTATO, testMembership.getStatus());
        assertFalse(testMembership.getMyTurn());
        verify(outboxService).saveEvent(eq("skip-payment"), any(SkipPaymentEvent.class));
    }

    @Test
    void resetSkippedUsersToNotPaid_WhenSkippedMemberExists_ShouldResetStatus() {
        // Given
        testMembership.setStatus(PaymentStatus.SALTATO);
        when(userGroupMembershipRepository.findByGroup(testGroup)).thenReturn(List.of(testMembership));
        when(userGroupMembershipRepository.save(testMembership)).thenReturn(testMembership);

        // When
        paymentService.resetSkippedUsersToNotPaid(testGroup);

        // Then
        assertEquals(PaymentStatus.NON_PAGATO, testMembership.getStatus());
        verify(userGroupMembershipRepository).save(testMembership);
    }

    @Test
    void resetSkippedUsersToNotPaid_WhenNoSkippedMember_ShouldNotSave() {
        // Given
        when(userGroupMembershipRepository.findByGroup(testGroup)).thenReturn(List.of(testMembership));

        // When
        paymentService.resetSkippedUsersToNotPaid(testGroup);

        // Then
        assertEquals(PaymentStatus.NON_PAGATO, testMembership.getStatus());
        verify(userGroupMembershipRepository, never()).save(any());
    }

    @Test
    void determineNextPayer_WhenNotPaidMembersExist_ShouldAssignNextTurn() {
        // Given
        CoffeeUser otherUser = new CoffeeUser();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");

        UserGroupMembership otherMembership = new UserGroupMembership();
        otherMembership.setCoffeeUser(otherUser);
        otherMembership.setGroup(testGroup);
        otherMembership.setStatus(PaymentStatus.NON_PAGATO);

        when(userGroupMembershipRepository.findByGroupAndStatus(testGroup, PaymentStatus.NON_PAGATO))
                .thenReturn(List.of(otherMembership));
        when(userGroupMembershipRepository.save(otherMembership)).thenReturn(otherMembership);

        // When
        var result = paymentService.determineNextPayer(testGroup);

        // Then
        assertNotNull(result);
        assertEquals("otheruser", result.getUsername());
        assertEquals("testgroup", result.getGroupName());
        assertTrue(otherMembership.getMyTurn());
    }

    @Test
    void getGroupPaymentRanking_WhenNoPayments_ShouldThrowNoContentAvailableException() {
        // Given
        GroupPaymentRankingRequest request = new GroupPaymentRankingRequest();
        request.setGroupName("testgroup");

        when(baseUserService.findUserByAuthId(123L)).thenReturn(testUser);
        when(baseUserService.findGroupByName("testgroup")).thenReturn(testGroup);
        when(paymentRepository.getGroupPaymentRanking("testgroup")).thenReturn(Collections.emptyList());

        // When & Then
        assertThrows(NoContentAvailableException.class,
                () -> paymentService.getGroupPaymentRanking(123L, request));
    }

    @Test
    void getLatestPaymentsByUsername_WhenPaymentsExist_ShouldReturnDtos() {
        // Given
        when(coffeeUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findWithUserGroupMembershipByCoffeeUserOrderByPaymentDateDesc(testUser))
                .thenReturn(List.of(savedPayment));

        // When
        List<PaymentDto> result = paymentService.getLatestPaymentsByUsername("testuser");

        // Then
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("testgroup", result.get(0).getGroupName());
    }

    @Test
    void getLatestPaymentsByUsername_WhenUserNotFound_ShouldThrowRuntimeException() {
        // Given
        when(coffeeUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> paymentService.getLatestPaymentsByUsername("unknown"));
    }
}