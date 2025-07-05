package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.dto.ProssimoPagamentoDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.event.SaltaPagamentoEvent;
import com.pagatu.coffee.mapper.PagamentoMapper;
import com.pagatu.coffee.repository.PagamentoRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class PagamentoService {

    @Autowired
    @Lazy
    private PagamentoService self;

    private static final Random RANDOM = new Random();
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final UtenteRepository utenteRepository;
    private final KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate;
    private final KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplate_saltaPagamento;
    private final BaseUserService baseUserService;

    @Value("${spring.kafka.topics.pagamenti-caffe}")
    private String pagamentiTopic;

    @Value("saltaPagamento-caffe")
    private String saltaPagamentoTopic;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            PagamentoMapper pagamentoMapper, UserGroupMembershipRepository userGroupMembershipRepository, UtenteRepository utenteRepository, KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate, KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplateSaltaPagamento, BaseUserService baseUserService) {
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.utenteRepository = utenteRepository;
        this.kafkaTemplate = kafkaTemplate;
        kafkaTemplate_saltaPagamento = kafkaTemplateSaltaPagamento;
        this.baseUserService = baseUserService;
    }

    @Transactional
    public PagamentoDto registraPagamento(Long userId, String groupNme, @Valid NuovoPagamentoRequest request) {

        Utente utente = baseUserService.findUserByAuthId(userId);

        Group group = baseUserService.findGroupByName(groupNme);

        List<UserGroupMembership> userGroupMembership = userGroupMembershipRepository.findByGroup(group);

        UserGroupMembership userGroupMembership1 = userGroupMembership.stream().filter(p -> p.getUtente().equals(utente)).toList().get(0);

        userGroupMembership1.setStatus(Status.PAGATO);

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

    @Transactional
    public ProssimoPagamentoDto determinaProssimoPagatore(Group group) {
        log.info("Determining next payer for group: {}", group.getName());

        // Get all memberships for this group with NON_PAGATO status
        List<UserGroupMembership> nonPagatoMemberships = userGroupMembershipRepository
                .findByGroupAndStatus(group, Status.NON_PAGATO);

        log.info("Users with NON_PAGATO status in group {}: {}",
                group.getName(), nonPagatoMemberships.size());

        if (nonPagatoMemberships.isEmpty()) {
            // All users in this group have paid, reset the group and start new round
            List<UserGroupMembership> resetMemberships = resetGroupMembersToNonPagato(group);

            if (resetMemberships.isEmpty()) {
                throw new RuntimeException("No active members found in group: " + group.getName());
            }

            UserGroupMembership nextPayer = resetMemberships.get(RANDOM.nextInt(resetMemberships.size()));
            log.info("All members paid in group {}, starting new round. Next payer: {}",
                    group.getName(), nextPayer.getUtente().getUsername());

            return creaProssimoPagamentoDto(nextPayer.getUtente(), group);
        } else {
            // Choose random user from those who haven't paid
            UserGroupMembership nextPayer = nonPagatoMemberships.get(RANDOM.nextInt(nonPagatoMemberships.size()));
            log.info("Next payer in group {}: {}", group.getName(), nextPayer.getUtente().getUsername());

            return creaProssimoPagamentoDto(nextPayer.getUtente(), group);
        }
    }

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

    private ProssimoPagamentoDto creaProssimoPagamentoDto(Utente utente, Group group) {
        ProssimoPagamentoDto dto = new ProssimoPagamentoDto();
        dto.setUserId(utente.getId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getName());
        return dto;
    }

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

    private SaltaPagamentoEvent saltaPagamentoEvent(Utente pagatore, ProssimoPagamentoDto prossimo) {
        SaltaPagamentoEvent event = new SaltaPagamentoEvent();
        event.setProssimoUserId(prossimo.getUserId());
        event.setProssimoUsername(prossimo.getUsername());
        event.setProssimoEmail(prossimo.getEmail());
        return event;
    }

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

        userGroupMembershipRepository.save(userGroupMembership1);

        // Determina chi sarà il prossimo a pagare
        ProssimoPagamentoDto prossimoPagamento = self.determinaProssimoPagatore(group);

        SaltaPagamentoEvent event = saltaPagamentoEvent(utente, prossimoPagamento);

        kafkaTemplate_saltaPagamento.send(saltaPagamentoTopic, event);

        log.info("Utente: {} ha saltato il pagamento - Prossimo pagatore: {}", utente.getUsername(), prossimoPagamento.getUsername());

    }

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

    public List<PagamentoDto> getUltimiPagamentiByUsername(String username) {

        // First, find the user by username
        Utente utente = utenteRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Get all payments for this user across all groups
        List<Pagamento> pagamenti = pagamentoRepository.findByUtenteOrderByDataPagamentoDesc(utente);

        // Convert to DTOs
        return pagamenti.stream()
                .map(this::convertToPagamentoDto)
                .toList();
    }

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

    // Additional utility methods for group-specific queries
//    @Transactional(readOnly = true)
//    public List<UserGroupMembership> getMembersWithStatus(String groupName, Status status) {
//        Group group = groupRepository.getGroupByName(groupName)
//                .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
//
//        return userGroupMembershipRepository.findByGroupAndStatus(group, status);
//    }

//    @Transactional(readOnly = true)
//    public long countMembersWithStatus(String groupName, Status status) {
//        Group group = groupRepository.getGroupByName(groupName)
//                .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
//
//        return userGroupMembershipRepository.countByGroupAndStatus(group, status);
//    }

//    @Transactional(readOnly = true)
//    public List<Pagamento> getGroupPaymentHistory(String groupName) {
//        Group group = groupRepository.getGroupByName(groupName)
//                .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
//
//        List<UserGroupMembership> groupMemberships = userGroupMembershipRepository.findByGroup(group);
//
//        return groupMemberships.stream()
//                .flatMap(membership -> pagamentoRepository.findByUserGroupMembershipOrderByDataPagamentoDesc(membership).stream())
//                .sorted((p1, p2) -> p2.getDataPagamento().compareTo(p1.getDataPagamento()))
//                .toList();
//    }
}