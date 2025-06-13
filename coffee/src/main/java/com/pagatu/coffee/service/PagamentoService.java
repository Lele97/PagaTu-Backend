package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.dto.ProssimoPagamentoDto;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.mapper.PagamentoMapper;
import com.pagatu.coffee.repository.GroupRepository;
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
import java.util.Random;

@Service
@Slf4j
public class PagamentoService {

    @Autowired
    @Lazy
    private PagamentoService self;

    private static final Random RANDOM = new Random();
    private final PagamentoRepository pagamentoRepository;
    private final UtenteRepository utenteRepository;
    private final PagamentoMapper pagamentoMapper;
    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.pagamenti-caffe}")
    private String pagamentiTopic;

    public PagamentoService(PagamentoRepository pagamentoRepository, UtenteRepository utenteRepository,
                            PagamentoMapper pagamentoMapper, GroupRepository groupRepository, UserGroupMembershipRepository userGroupMembershipRepository, KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate) {
        this.pagamentoRepository = pagamentoRepository;
        this.utenteRepository = utenteRepository;
        this.pagamentoMapper = pagamentoMapper;
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PagamentoDto registraPagamento(Long userId, String groupNme, @Valid NuovoPagamentoRequest request) {


        Utente utente = utenteRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.getGroupByName(groupNme).orElseThrow(() -> new RuntimeException("Group non trovato"));

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

    // Overloaded method for backward compatibility (if called without group)
//    @Deprecated
//    @Transactional
//    public ProssimoPagamentoDto determinaProssimoPagatore() {
//        log.warn("determinaProssimoPagatore() called without group parameter - this is deprecated");
//
//        // Find the most recently active group or default group
//        // This is a fallback - ideally all calls should specify a group
//        List<Group> allGroups = groupRepository.findAll();
//        if (allGroups.isEmpty()) {
//            throw new RuntimeException("No groups found - cannot determine next payer");
//        }
//
//        // Use the first group as default (you might want different logic here)
//        Group defaultGroup = allGroups.get(0);
//        log.info("Using default group {} for payment determination", defaultGroup.getName());
//
//        return determinaProssimoPagatore(defaultGroup);
//    }

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