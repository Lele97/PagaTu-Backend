package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserStatisticsDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStatisticsServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserGroupMembershipRepository membershipRepository;

    @InjectMocks
    private UserStatisticsService userStatisticsService;

    private CoffeeUser user;
    private UserGroupMembership membership;

    @BeforeEach
    void setUp() {
        user = new CoffeeUser();
        user.setAuthId(10L);
        user.setUsername("mario");

        Group group = new Group();
        group.setName("ufficio");

        membership = new UserGroupMembership();
        membership.setCoffeeUser(user);
        membership.setGroup(group);
        membership.setIsAdmin(true);
        membership.setSkipCount(1);
        membership.setPaymentStreak(4);
    }

    @Test
    void getStatistics_aggregatesPaymentsAndMembership() {
        Payment own = payment(3.5, null);
        Payment payFor = payment(4.0, "luigi");

        when(baseUserService.findUserByAuthId(10L)).thenReturn(user);
        when(paymentRepository.findByCoffeeUserOrderByPaymentDateDesc(user)).thenReturn(List.of(own, payFor));
        when(membershipRepository.findByCoffeeUserWithGroup(user)).thenReturn(List.of(membership));
        when(paymentRepository.sumPaymentsByUserInGroupForMonth("ufficio", "mario",
                LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue())).thenReturn(7.5);
        when(membershipRepository.findByGroup(membership.getGroup())).thenReturn(List.of(membership));

        UserStatisticsDto stats = userStatisticsService.getStatistics(10L);

        assertEquals(7.5, stats.getTotalPaid());
        assertEquals(1, stats.getTotalCoffeesForOthers());
        assertEquals(1, stats.getSkippedCount());
        assertEquals(4, stats.getCurrentStreak());
        assertEquals(4, stats.getLongestStreak());
        assertEquals(4.0, stats.getMostExpensive());
        assertEquals(3.75, stats.getAveragePayment());
        assertEquals(1, stats.getTimesKing());
        assertEquals("Coffee Legend", stats.getFunTitle());
        assertTrue(stats.getCoffeeKarma() >= 0 && stats.getCoffeeKarma() <= 100);
    }

    @Test
    void computeTitle_skipMasterWhenMoreSkipsThanPayments() {
        assertEquals("Skip Master", UserStatisticsService.computeTitle(1, 3, 0, 0, 0));
        assertEquals("Coffee Newbie", UserStatisticsService.computeTitle(0, 0, 0, 0, 0));
        assertEquals("Coffee Legend", UserStatisticsService.computeTitle(8, 0, 7, 0, 0));
    }

    private static Payment payment(double amount, String beneficiary) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setBeneficiaryUsername(beneficiary);
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }
}
