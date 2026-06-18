package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupPaymentHistoryRequest;
import com.pagatu.coffee.dto.GroupPaymentRankingDto;
import com.pagatu.coffee.dto.GroupPaymentRankingRequest;
import com.pagatu.coffee.dto.PaymentDto;
import com.pagatu.coffee.dto.NextPaymentDto;
import com.pagatu.coffee.exception.BusinessException;
import com.pagatu.coffee.exception.ForbiddenException;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.NextPaymentEvent;
import com.pagatu.coffee.event.PayForEvent;
import com.pagatu.coffee.event.SkipPaymentEvent;
import com.pagatu.coffee.exception.ActiveUserMemberNotInGroup;
import com.pagatu.coffee.exception.GroupNotFoundException;
import com.pagatu.coffee.exception.NoContentAvailableException;
import com.pagatu.coffee.exception.UserNotFoundException;
import com.pagatu.coffee.mapper.PaymentMapper;
import com.pagatu.coffee.repository.PaymentRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing coffee payment operations and business logic.
 * <p>
 * This service handles all payment-related operations including:
 * <ul>
 * <li>Payment registration and processing</li>
 * <li>Determining next payer in rotation</li>
 * <li>Managing payment status (PAID, NOT_PAID, SKIPPED)</li>
 * <li>Handling payment on behalf of others</li>
 * <li>Generating payment statistics and rankings</li>
 * <li>Publishing payment events to NATS subjects</li>
 * </ul>
 * </p>
 * <p>
 * The service integrates with NATS for event-driven architecture,
 * sending notifications when payments are made or skipped.
 * </p>
 */
@Service
@Slf4j
public class PaymentService {

    @Value("${spring.nats.subject.next-payment-subject}")
    private String natsSubjectNextPayment;

    @Value("${spring.nats.subject.skip-payment-subject}")
    private String natsSubjectSkipPayment;

    @Value("${spring.nats.subject.pay-for-subject}")
    private String natsSubjectPayFor;

    private final OutboxService outboxService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final CoffeeUserRepository coffeeUserRepository;
    private final BaseUserService baseUserService;

    /**
     * @param outboxService                 transactional outbox for NATS events
     * @param paymentRepository             payment persistence layer
     * @param paymentMapper                 entity/DTO mapper
     * @param userGroupMembershipRepository membership and rotation state
     * @param coffeeUserRepository          user lookups for payment history
     * @param baseUserService               shared user/group resolution
     */
    public PaymentService(OutboxService outboxService, PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            UserGroupMembershipRepository userGroupMembershipRepository,
            CoffeeUserRepository coffeeUserRepository,
            BaseUserService baseUserService) {
        this.outboxService = outboxService;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.coffeeUserRepository = coffeeUserRepository;
        this.baseUserService = baseUserService;
    }

    /**
     * Registers a new coffee payment for a user in a specific group.
     *
     * @param userId    the ID of the user making the payment
     * @param groupName the name of the group for which the payment is made
     * @param request   the payment details including amount and description
     * @return PaymentDto containing the saved payment information
     */
    @Transactional
    public PaymentDto registerPayment(Long userId, String groupName, @Valid NewPaymentRequest request) {

        CoffeeUser coffeeUser = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupName);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership membership = userGroupMembership.stream()
                .filter(p -> p.getCoffeeUser().equals(coffeeUser))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Non sei membro del gruppo '" + groupName + "'"));

        validateMyTurn(membership, groupName);

        membership.setStatus(PaymentStatus.PAGATO);
        membership.setMyTurn(false);

        UserGroupMembership savedMembership = userGroupMembershipRepository.save(membership);

        Payment payment = new Payment();
        payment.setUserGroupMembership(savedMembership);
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        resetSkippedUsersToNotPaid(group);

        // Determine who should pay next
        NextPaymentDto nextPayment = determineNextPayer(group);

        NextPaymentEvent event = createPaymentEvent(savedPayment, coffeeUser, nextPayment);

        outboxService.saveEvent(natsSubjectNextPayment, event);

        log.info("Pagamento registrato: {} - Prossimo pagatore: {}", savedPayment.getId(), nextPayment.getUsername());

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Allows a user to make a payment on behalf of the member whose turn it is.
     * <p>
     * Both the payer and the beneficiary are marked as paid, a payment record is created
     * for the payer, and the next payer is determined. A {@link PayForEvent} payload is
     * prepared and published via the transactional outbox so the mail service can
     * notify both the payer and the beneficiary.
     * </p>
     *
     * @param userId    the auth ID of the user making the payment
     * @param groupName the name of the group
     * @param request   the payment details including amount and description
     * @return PaymentDto containing the saved payment information
     */
    @Transactional
    public PaymentDto payFor(Long userId, String groupName, NewPaymentRequest request) {

        CoffeeUser payingUser = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupName);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership payerMembership = userGroupMembership.stream()
                .filter(p -> p.getCoffeeUser().equals(payingUser))
                .toList().get(0);

        payerMembership.setStatus(PaymentStatus.PAGATO);
        payerMembership.setMyTurn(false);

        UserGroupMembership savedPayerMembership = userGroupMembershipRepository.save(payerMembership);

        UserGroupMembership friendMembership = userGroupMembershipRepository.findUserTurn(group.getName());

        CoffeeUser friend = baseUserService.findUserByAuthId(friendMembership.getCoffeeUser().getAuthId());

        friendMembership.setStatus(PaymentStatus.PAGATO);
        friendMembership.setMyTurn(false);

        userGroupMembershipRepository.save(friendMembership);

        Payment payment = new Payment();
        payment.setUserGroupMembership(savedPayerMembership);
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        resetSkippedUsersToNotPaid(group);

        NextPaymentDto nextPayment = determineNextPayer(group);

        NextPaymentEvent event = createPaymentEvent(savedPayment, payingUser, nextPayment);

        outboxService.saveEvent(natsSubjectNextPayment, event);

        try {
            PayForEvent payForEvent = new PayForEvent();
            payForEvent.setPayerUsername(payingUser.getUsername());
            payForEvent.setPayerEmail(payingUser.getEmail());
            payForEvent.setFriendUsername(friend.getUsername());
            payForEvent.setFriendEmail(friend.getEmail());
            payForEvent.setGroupName(group.getName());
            payForEvent.setAmount(savedPayment.getAmount());
            payForEvent.setPaymentDate(savedPayment.getPaymentDate());

            outboxService.saveEvent(natsSubjectPayFor, payForEvent);
            log.info("PayFor event saved in outbox: {} paid for {}", payingUser.getUsername(), friend.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish payFor event", e);
        }

        log.info("Pagato per: {} - Pagamento registrato: {} - Prossimo pagatore: {}",
                friend.getAuthId(), savedPayment.getId(), nextPayment.getUsername());

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Determines who should be the next payer in the group rotation.
     *
     * @param group the group for which to determine the next payer
     * @return NextPaymentDto containing details of the next payer
     */
    @Transactional
    public NextPaymentDto determineNextPayer(Group group) {
        log.info("Determining next payer for group: {}", group.getName());

        List<UserGroupMembership> notPaidMemberships = userGroupMembershipRepository
                .findByGroupAndStatus(group, PaymentStatus.NON_PAGATO);

        log.info("Users with NOT_PAID status in group {}: {}",
                group.getName(), notPaidMemberships.size());

        if (notPaidMemberships.isEmpty()) {
            List<UserGroupMembership> resetMemberships = resetGroupMembersToNotPaid(group);

            if (resetMemberships.isEmpty()) {
                throw new ActiveUserMemberNotInGroup("No active members found in group: " + group.getName());
            }

            UserGroupMembership nextPayer = resetMemberships.get(RANDOM.nextInt(resetMemberships.size()));
            assignTurn(nextPayer);
            userGroupMembershipRepository.save(nextPayer);
            log.info("All members paid in group {}, starting new round. Next payer: {}",
                    group.getName(), nextPayer.getCoffeeUser().getUsername());

            return createNextPaymentDto(nextPayer.getCoffeeUser(), group);
        } else {
            UserGroupMembership nextPayer = notPaidMemberships.get(RANDOM.nextInt(notPaidMemberships.size()));
            assignTurn(nextPayer);
            userGroupMembershipRepository.save(nextPayer);
            log.info("Next payer in group {}: {}", group.getName(), nextPayer.getCoffeeUser().getUsername());

            return createNextPaymentDto(nextPayer.getCoffeeUser(), group);
        }
    }

    /**
     * Resets all group members to NOT_PAID status for a new payment round.
     *
     * @param group the group whose members should be reset
     * @return List of reset UserGroupMembership objects
     */
    private List<UserGroupMembership> resetGroupMembersToNotPaid(Group group) {
        log.info("Resetting all members in group {} to NOT_PAID status", group.getName());

        List<UserGroupMembership> allMemberships = userGroupMembershipRepository.findByGroup(group);

        for (UserGroupMembership membership : allMemberships) {
            membership.setStatus(PaymentStatus.NON_PAGATO);
        }

        List<UserGroupMembership> savedMemberships = userGroupMembershipRepository.saveAll(allMemberships);
        log.info("Reset {} members in group {} to NOT_PAID", savedMemberships.size(), group.getName());

        return savedMemberships;
    }

    /**
     * Creates a DTO containing information about the next payer.
     *
     * @param coffeeUser the user who will be the next payer
     * @param group      the group in which the payment will be made
     * @return NextPaymentDto with complete next payer information
     */
    private NextPaymentDto createNextPaymentDto(CoffeeUser coffeeUser, Group group) {
        NextPaymentDto dto = new NextPaymentDto();
        dto.setUserId(coffeeUser.getId());
        dto.setUsername(coffeeUser.getUsername());
        dto.setEmail(coffeeUser.getEmail());
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getName());
        return dto;
    }

    /**
     * Creates a NextPaymentEvent from payment details.
     */
    private NextPaymentEvent createPaymentEvent(Payment payment, CoffeeUser payer, NextPaymentDto next) {
        NextPaymentEvent event = new NextPaymentEvent();
        event.setLastPaymentId(payment.getId());
        event.setLastPayerUsername(payer.getUsername());
        event.setLastPayerEmail(payer.getEmail());
        event.setNextUserId(next.getUserId());
        event.setNextUsername(next.getUsername());
        event.setNextEmail(next.getEmail());
        event.setLastPaymentDate(payment.getPaymentDate());
        event.setAmount(payment.getAmount());
        event.setGroupName(next.getGroupName());
        return event;
    }

    /**
     * Creates a SkipPaymentEvent for skipped payments.
     */
    private SkipPaymentEvent createSkipPaymentEvent(NextPaymentDto next) {
        SkipPaymentEvent event = new SkipPaymentEvent();
        event.setNextUserId(next.getUserId());
        event.setNextUsername(next.getUsername());
        event.setNextEmail(next.getEmail());
        return event;
    }

    /**
     * Skips the payment for a user in a group and determines the next payer.
     *
     * @param userId    the ID of the user skipping payment
     * @param groupName the name of the group
     */
    @Transactional
    public void skipPayment(Long userId, String groupName) {

        CoffeeUser coffeeUser = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupName);

        resetSkippedUsersToNotPaid(group);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership membership = userGroupMembership.stream()
                .filter(p -> p.getCoffeeUser().equals(coffeeUser))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Non sei membro del gruppo '" + groupName + "'"));

        validateMyTurn(membership, groupName);

        membership.setStatus(PaymentStatus.SALTATO);
        membership.setMyTurn(false);

        userGroupMembershipRepository.save(membership);

        NextPaymentDto nextPayment = determineNextPayer(group);

        SkipPaymentEvent event = createSkipPaymentEvent(nextPayment);

        outboxService.saveEvent(natsSubjectSkipPayment, event);

        log.info("Utente: {} ha saltato il pagamento - Prossimo pagatore: {}",
                coffeeUser.getUsername(), nextPayment.getUsername());
    }

    /**
     * Resets any group members with SKIPPED status back to NOT_PAID status.
     *
     * @param group the group to check for skipped payments
     */
    public void resetSkippedUsersToNotPaid(Group group) {

        List<UserGroupMembership> allMemberships = userGroupMembershipRepository.findByGroup(group);

        Optional<UserGroupMembership> maybeMembership = allMemberships.stream()
                .filter(p -> p.getStatus().equals(PaymentStatus.SALTATO))
                .findFirst();

        if (maybeMembership.isPresent()) {
            UserGroupMembership membership = maybeMembership.get();
            log.info("Reset member in status SKIPPED in group {} to NOT_PAID status", group.getName());
            membership.setStatus(PaymentStatus.NON_PAGATO);
            userGroupMembershipRepository.save(membership);
        } else {
            log.info("Non c'è nessun membro del gruppo '{}' che ha saltato", group.getName());
        }
    }

    /**
     * Retrieves payment rankings for a specific group.
     *
     * @param userId  the ID of the user requesting the ranking
     * @param request contains the group name
     * @return List of GroupPaymentRankingDto with payment rankings
     */
    @Transactional(readOnly = true)
    public List<GroupPaymentRankingDto> getGroupPaymentRanking(Long userId, GroupPaymentRankingRequest request) {

        CoffeeUser coffeeUser = baseUserService.findUserByAuthId(userId);

        if (coffeeUser == null) {
            throw new UserNotFoundException("User not found");
        }

        Group group = baseUserService.findGroupByName(request.getGroupName());

        if (group == null) {
            throw new GroupNotFoundException("Group not found: " + request.getGroupName());
        }

        List<GroupPaymentRankingDto> rankings = paymentRepository.getGroupPaymentRanking(request.getGroupName());

        if (rankings.isEmpty()) {
            throw new NoContentAvailableException("Non ci sono pagamenti");
        }

        return rankings;
    }

    /**
     * Retrieves shared payment history for a group, optionally filtered by year/month.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getGroupPaymentHistory(Long userId, GroupPaymentHistoryRequest request) {
        CoffeeUser coffeeUser = baseUserService.findUserByAuthId(userId);
        Group group = baseUserService.findGroupByName(request.getGroupName());

        boolean isMember = userGroupMembershipRepository.existsByCoffeeUserAndGroup(coffeeUser, group);
        if (!isMember) {
            throw new ForbiddenException("Non sei autorizzato a visualizzare lo storico di questo gruppo");
        }

        List<Payment> payments = paymentRepository.findGroupPayments(
                request.getGroupName(), request.getYear(), request.getMonth());

        if (payments.isEmpty()) {
            throw new NoContentAvailableException("Non ci sono pagamenti per questo gruppo");
        }

        return payments.stream()
                .map(this::convertToPaymentDto)
                .toList();
    }

    /**
     * Reassigns the active turn to a random non-paid member when the current payer leaves.
     */
    @Transactional
    public void reassignTurnAfterMemberRemoval(Group group) {
        List<UserGroupMembership> memberships = userGroupMembershipRepository.findByGroup(group);
        memberships.forEach(m -> m.setMyTurn(false));
        userGroupMembershipRepository.saveAll(memberships);

        List<UserGroupMembership> notPaid = memberships.stream()
                .filter(m -> PaymentStatus.NON_PAGATO.equals(m.getStatus()))
                .toList();

        if (!notPaid.isEmpty()) {
            UserGroupMembership nextPayer = notPaid.get(RANDOM.nextInt(notPaid.size()));
            assignTurn(nextPayer);
            userGroupMembershipRepository.save(nextPayer);
        } else if (!memberships.isEmpty()) {
            determineNextPayer(group);
        }
    }

    private void validateMyTurn(UserGroupMembership membership, String groupName) {
        if (!Boolean.TRUE.equals(membership.getMyTurn())) {
            throw new BusinessException("Non è il tuo turno di pagare la colazione nel gruppo '" + groupName + "'");
        }
    }

    private void assignTurn(UserGroupMembership membership) {
        membership.setMyTurn(true);
        membership.setTurnAssignedAt(LocalDateTime.now());
        membership.setReminderLevel(0);
    }

    /**
     * Retrieves the latest payments made by a specific user.
     *
     * @param username the username to search payments for
     * @return List of PaymentDto with payment history
     */
    public List<PaymentDto> getLatestPaymentsByUsername(String username) {
        CoffeeUser coffeeUser = coffeeUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Payment> payments = paymentRepository
                .findWithUserGroupMembershipByCoffeeUserOrderByPaymentDateDesc(coffeeUser);

        return payments.stream()
                .map(this::convertToPaymentDto)
                .toList();
    }

    /**
     * Converts a Payment entity to a PaymentDto.
     *
     * @param payment the payment entity to convert
     * @return PaymentDto with populated fields
     */
    private PaymentDto convertToPaymentDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setUserId(payment.getUserGroupMembership().getCoffeeUser().getId());
        dto.setUsername(payment.getUserGroupMembership().getCoffeeUser().getUsername());
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setGroupId(payment.getUserGroupMembership().getGroup().getId());
        dto.setGroupName(payment.getUserGroupMembership().getGroup().getName());
        return dto;
    }
}
