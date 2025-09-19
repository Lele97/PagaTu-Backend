package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoDto;
import com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoRequest;
import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.dto.ProssimoPagamentoDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.event.SaltaPagamentoEvent;
import com.pagatu.coffee.exception.ActiveUserMemberNotInGroup;
import com.pagatu.coffee.exception.GroupNotFoundException;
import com.pagatu.coffee.exception.NoContentAvailableException;
import com.pagatu.coffee.exception.UserNotFoundException;
import com.pagatu.coffee.mapper.PagamentoMapper;
import com.pagatu.coffee.repository.PagamentoRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Service class for managing coffee payment operations and business logic.
 * <p>
 * This service handles all payment-related operations including:
 * <ul>
 *   <li>Payment registration and processing</li>
 *   <li>Determining next payer in rotation</li>
 *   <li>Managing payment status (PAGATO, NON_PAGATO, SALTATO)</li>
 *   <li>Handling payment on behalf of others</li>
 *   <li>Generating payment statistics and rankings</li>
 *   <li>Publishing payment events to Kafka topics</li>
 * </ul>
 * </p>
 * <p>
 * The service integrates with Kafka for event-driven architecture,
 * sending notifications when payments are made or skipped.
 * </p>
 */
@Service
@Slf4j
public class PagamentoService {

    @Value("${spring.kafka.topics.pagamenti-caffe}")
    private String pagamentiTopic;

    @Value("${spring.kafka.topics.saltaPagamento-caffe}")
    private String saltaPagamentoTopic;

    @Lazy
    private PagamentoService self;

    private static final SecureRandom RANDOM = new SecureRandom();
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final UtenteRepository utenteRepository;
    private final KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate;
    private final KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplateSaltaPagamento;
    private final BaseUserService baseUserService;

    public PagamentoService(PagamentoService self, PagamentoRepository pagamentoRepository,
                            PagamentoMapper pagamentoMapper, UserGroupMembershipRepository userGroupMembershipRepository, UtenteRepository utenteRepository, KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate, KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplateSaltaPagamento, BaseUserService baseUserService) {
        this.self = self;
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.utenteRepository = utenteRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateSaltaPagamento = kafkaTemplateSaltaPagamento;
        this.baseUserService = baseUserService;
    }

    /**
     * Registers a new coffee payment for a user in a specific group.
     * <p>
     * This method handles the complete payment registration process:
     * <ul>
     *   <li>Updates the user's status to PAGATO</li>
     *   <li>Resets any users with SALTATO status back to NON_PAGATO</li>
     *   <li>Determines who should pay next</li>
     *   <li>Publishes a payment event to Kafka</li>
     * </ul>
     * </p>
     *
     * @param userId the ID of the user making the payment
     * @param groupNme the name of the group for which the payment is made
     * @param request the payment details including amount and description
     * @return PagamentoDto containing the saved payment information
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.UserNotInGroup if the user is not a member of the group
     */
    @Transactional
    public PagamentoDto registraPagamento(Long userId, String groupNme, @Valid NuovoPagamentoRequest request) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupNme);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership userGroupMembership1 = userGroupMembership.stream().filter(p -> p.getUtente().equals(utente)).toList().get(0);

        userGroupMembership1.setStatus(Status.PAGATO);

        userGroupMembership1.setMyTurn(false);

        UserGroupMembership savedUserGroupMembership = userGroupMembershipRepository.save(userGroupMembership1);

        Pagamento pagamento = new Pagamento();
        pagamento.setUserGroupMembership(savedUserGroupMembership);
        pagamento.setImporto(request.getImporto());
        pagamento.setDescrizione(request.getDescrizione());
        pagamento.setDataPagamento(LocalDateTime.now());

        Pagamento savedPagamento = pagamentoRepository.save(pagamento);

        resetUtenteSaltatoinNonPagato(group);

        // Determina chi sarà il prossimo a pagare
        ProssimoPagamentoDto prossimoPagamento = self.determinaProssimoPagatore(group);

        ProssimoPagamentoEvent event = createPagamentoEvent(savedPagamento, utente, prossimoPagamento);

        kafkaTemplate.send(pagamentiTopic, event);

        log.info("Pagamento registrato: {} - Prossimo pagatore: {}", savedPagamento.getId(), prossimoPagamento.getUsername());

        return pagamentoMapper.toDto(savedPagamento);
    }

    /**
     * Allows a user to make a payment on behalf of another group member.
     * <p>
     * This method handles payments where one user pays for both themselves
     * and the person whose turn it currently is. Both users will be marked
     * as having paid (PAGATO status), and the system will determine the next payer.
     * </p>
     *
     * @param userId the ID of the user making the payment
     * @param groupNme the name of the group for which the payment is made
     * @param request the payment details including amount and description
     * @return PagamentoDto containing the saved payment information
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.UserNotInGroup if the user is not a member of the group
     */
    @Transactional
    public PagamentoDto pagaPer(Long userId, String groupNme, NuovoPagamentoRequest request) {

        Utente utentePagante = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupNme);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership userGroupMembership1 = userGroupMembership.stream().filter(p -> p.getUtente().equals(utentePagante)).toList().get(0);

        userGroupMembership1.setStatus(Status.PAGATO);

        userGroupMembership1.setMyTurn(false);

        UserGroupMembership savedUserGroupMembership = userGroupMembershipRepository.save(userGroupMembership1);

        UserGroupMembership userGroupMembership2 = userGroupMembershipRepository.findUserTurn(group.getName());

        Utente utenteAmico = baseUserService.findUserByAuthId(userGroupMembership2.getUtente().getAuthId());

        userGroupMembership2.setStatus(Status.PAGATO);
        userGroupMembership2.setMyTurn(false);

        userGroupMembershipRepository.save(userGroupMembership2);

        Pagamento pagamento = new Pagamento();
        pagamento.setUserGroupMembership(savedUserGroupMembership);
        pagamento.setImporto(request.getImporto());
        pagamento.setDescrizione(request.getDescrizione());
        pagamento.setDataPagamento(LocalDateTime.now());

        Pagamento savedPagamento = pagamentoRepository.save(pagamento);

        resetUtenteSaltatoinNonPagato(group);

        ProssimoPagamentoDto prossimoPagamento = determinaProssimoPagatore(group);

        ProssimoPagamentoEvent event = createPagamentoEvent(savedPagamento, utentePagante, prossimoPagamento);

        kafkaTemplate.send(pagamentiTopic, event);

        log.info("Pagato per: {} - Pagamento registrato: {} - Prossimo pagatore: {}", utenteAmico.getAuthId(), savedPagamento.getId(), prossimoPagamento.getUsername());

        return pagamentoMapper.toDto(savedPagamento);
    }

    /**
     * Determines who should be the next payer in the group rotation.
     * <p>
     * The algorithm works as follows:
     * <ol>
     *   <li>Look for users with NON_PAGATO status</li>
     *   <li>If found, randomly select one as the next payer</li>
     *   <li>If none found, reset all members to NON_PAGATO and start a new round</li>
     *   <li>Update the selected user's myTurn flag to true</li>
     * </ol>
     * </p>
     *
     * @param group the group for which to determine the next payer
     * @return ProssimoPagamentoDto containing details of the next payer
     * @throws com.pagatu.coffee.exception.ActiveUserMemberNotInGroup if no active members found in the group
     */
    private ProssimoPagamentoDto determinaProssimoPagatore(Group group) {
        log.info("Determining next payer for group: {}", group.getName());

        List<UserGroupMembership> nonPagatoMemberships = userGroupMembershipRepository
                .findByGroupAndStatus(group, Status.NON_PAGATO);

        log.info("Users with NON_PAGATO status in group {}: {}",
                group.getName(), nonPagatoMemberships.size());

        if (nonPagatoMemberships.isEmpty()) {
            List<UserGroupMembership> resetMemberships = resetGroupMembersToNonPagato(group);

            if (resetMemberships.isEmpty()) {
                throw new ActiveUserMemberNotInGroup("No active members found in group: " + group.getName());
            }

            UserGroupMembership nextPayer = resetMemberships.get(RANDOM.nextInt(resetMemberships.size()));
            nextPayer.setMyTurn(true);
            userGroupMembershipRepository.save(nextPayer);
            log.info("All members paid in group {}, starting new round. Next payer: {}",
                    group.getName(), nextPayer.getUtente().getUsername());

            return creaProssimoPagamentoDto(nextPayer.getUtente(), group);
        } else {
            UserGroupMembership nextPayer = nonPagatoMemberships.get(RANDOM.nextInt(nonPagatoMemberships.size()));
            nextPayer.setMyTurn(true);
            userGroupMembershipRepository.save(nextPayer);
            log.info("Next payer in group {}: {}", group.getName(), nextPayer.getUtente().getUsername());

            return creaProssimoPagamentoDto(nextPayer.getUtente(), group);
        }
    }

    /**
     * Resets all group members to NON_PAGATO status for a new payment round.
     * <p>
     * This method is called when all members have paid and a new round needs to start.
     * It sets all memberships in the group to NON_PAGATO status.
     * </p>
     *
     * @param group the group whose members should be reset
     * @return List of reset UserGroupMembership objects
     */
    private List<UserGroupMembership> resetGroupMembersToNonPagato(Group group) {
        log.info("Resetting all members in group {} to NON_PAGATO status", group.getName());

        List<UserGroupMembership> allMemberships = userGroupMembershipRepository.findByGroup(group);

        for (UserGroupMembership membership : allMemberships) {
            membership.setStatus(Status.NON_PAGATO);
        }

        List<UserGroupMembership> savedMemberships = userGroupMembershipRepository.saveAll(allMemberships);
        log.info("Reset {} members in group {} to NON_PAGATO", savedMemberships.size(), group.getName());

        return savedMemberships;
    }

    /**
     * Creates a DTO containing information about the next payer.
     * <p>
     * This helper method constructs a ProssimoPagamentoDto with user and group
     * information for the next person who should make a payment.
     * </p>
     *
     * @param utente the user who will be the next payer
     * @param group the group in which the payment will be made
     * @return ProssimoPagamentoDto with complete next payer information
     */
    private ProssimoPagamentoDto creaProssimoPagamentoDto(Utente utente, Group group) {
        ProssimoPagamentoDto dto = new ProssimoPagamentoDto();
        dto.setUserId(utente.getId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getName());
        return dto;
    }

    /**
     * Creates a ProssimoPagamentoEvent from payment details.
     *
     * @param pagamento the payment entity
     * @param pagatore the user who made the payment
     * @param prossimo details about the next payer
     * @return populated ProssimoPagamentoEvent
     */
    private ProssimoPagamentoEvent createPagamentoEvent(Pagamento pagamento, Utente pagatore, ProssimoPagamentoDto prossimo) {
        ProssimoPagamentoEvent event = new ProssimoPagamentoEvent();
        event.setUltimoPagamentoId(pagamento.getId());
        event.setUltimoPagatoreUsername(pagatore.getUsername());
        event.setUltimoPagatoreEmail(pagatore.getEmail());
        event.setProssimoUserId(prossimo.getUserId());
        event.setProssimoUsername(prossimo.getUsername());
        event.setProssimoEmail(prossimo.getEmail());
        event.setDataUltimoPagamento(pagamento.getDataPagamento());
        event.setImporto(pagamento.getImporto());
        event.setGroupName(prossimo.getGroupName());
        return event;
    }

    /**
     * Creates a SaltaPagamentoEvent for skipped payments.
     *
     * @param prossimo details about the next payer
     * @return populated SaltaPagamentoEvent
     */
    private SaltaPagamentoEvent saltaPagamentoEvent(ProssimoPagamentoDto prossimo) {
        SaltaPagamentoEvent event = new SaltaPagamentoEvent();
        event.setProssimoUserId(prossimo.getUserId());
        event.setProssimoUsername(prossimo.getUsername());
        event.setProssimoEmail(prossimo.getEmail());
        return event;
    }

    /**
     * Skips the payment for a user in a group and determines the next payer.
     * <p>
     * Marks the user's status as SALTATO and publishes a skip payment event.
     * </p>
     *
     * @param userId the ID of the user skipping payment
     * @param groupNme the name of the group
     */
    @Transactional
    public void saltaPagamento(Long userId, String groupNme) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupNme);

        resetUtenteSaltatoinNonPagato(group);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership userGroupMembership1 = userGroupMembership.stream()
                .filter(p -> p.getUtente().equals(utente))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Membership non trovata per utente nel gruppo"));

        userGroupMembership1.setStatus(Status.SALTATO);

        userGroupMembership1.setMyTurn(false);

        userGroupMembershipRepository.save(userGroupMembership1);

        ProssimoPagamentoDto prossimoPagamento = self.determinaProssimoPagatore(group);

        SaltaPagamentoEvent event = saltaPagamentoEvent(prossimoPagamento);

        kafkaTemplateSaltaPagamento.send(saltaPagamentoTopic, event);

        log.info("Utente: {} ha saltato il pagamento - Prossimo pagatore: {}", utente.getUsername(), prossimoPagamento.getUsername());

    }

    /**
     * Resets any group members with SALTATO status back to NON_PAGATO status.
     *
     * @param group the group to check for skipped payments
     */
    public void resetUtenteSaltatoinNonPagato(Group group) {

        List<UserGroupMembership> allMemberships = userGroupMembershipRepository.findByGroup(group);

        Optional<UserGroupMembership> maybeMembership = allMemberships.stream()
                .filter(p -> p.getStatus().equals(Status.SALTATO))
                .findFirst();

        if (maybeMembership.isPresent()) {
            UserGroupMembership userGroupMembership = maybeMembership.get();
            log.info("Reset member in status SALTATO in group {} to NON_PAGATO status", group.getName());
            userGroupMembership.setStatus(Status.NON_PAGATO);
            userGroupMembershipRepository.save(userGroupMembership);
        } else {
            log.info("Non c'è nessun membro del gruppo '{}' che ha saltato", group.getName());
        }
    }

    /**
     * Retrieves payment rankings for a specific group.
     *
     * @param userId the ID of the user requesting the ranking
     * @param classificaPagamentiPerGruppoRequest contains the group name
     * @return List of ClassificaPagamentiPerGruppoDto with payment rankings
     * @throws UserNotFoundException if the user is not found
     * @throws GroupNotFoundException if the group is not found
     * @throws NoContentAvailableException if no payments exist for the group
     */
    @Transactional(readOnly = true)
    public List<ClassificaPagamentiPerGruppoDto> getClassificaPagamentiPerGruppo(Long userId, ClassificaPagamentiPerGruppoRequest classificaPagamentiPerGruppoRequest) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        if (utente == null) {
            throw new UserNotFoundException("User not found");
        }

        Group group = baseUserService.findGroupByName(classificaPagamentiPerGruppoRequest.getGroupName());

        if (group == null) {
            throw new GroupNotFoundException("Group not found: " + classificaPagamentiPerGruppoRequest.getGroupName());
        }

        List<ClassificaPagamentiPerGruppoDto> classificaPagamentiPerGruppoDtos = pagamentoRepository.classificaPagamentiPerGruppo(classificaPagamentiPerGruppoRequest.getGroupName());

        if (classificaPagamentiPerGruppoDtos.isEmpty()) {
            throw new NoContentAvailableException("Non ci sono pagamenti");
        }

        return classificaPagamentiPerGruppoDtos;
    }

    /**
     * Retrieves the latest payments made by a specific user.
     *
     * @param username the username to search payments for
     * @return List of PagamentoDto with payment history
     */
    public List<PagamentoDto> getUltimiPagamentiByUsername(String username) {

        Utente utente = utenteRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Pagamento> pagamenti = pagamentoRepository.findByUtenteOrderByDataPagamentoDesc(utente);

        return pagamenti.stream()
                .map(this::convertToPagamentoDto)
                .toList();
    }

    /**
     * Converts a Pagamento entity to a PagamentoDto.
     *
     * @param pagamento the payment entity to convert
     * @return PagamentoDto with populated fields
     */
    private PagamentoDto convertToPagamentoDto(Pagamento pagamento) {
        PagamentoDto dto = new PagamentoDto();
        dto.setId(pagamento.getId());
        dto.setUserId(pagamento.getUserGroupMembership().getUtente().getId());
        dto.setUsername(pagamento.getUserGroupMembership().getUtente().getUsername());
        dto.setImporto(pagamento.getImporto());
        dto.setDescrizione(pagamento.getDescrizione());
        dto.setDataPagamento(pagamento.getDataPagamento());
        dto.setGroupId(pagamento.getUserGroupMembership().getGroup().getId());
        dto.setGroupName(pagamento.getUserGroupMembership().getGroup().getName());
        return dto;
    }
}