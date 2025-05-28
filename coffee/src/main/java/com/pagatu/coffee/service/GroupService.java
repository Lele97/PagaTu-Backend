package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UtenteRepository utenteRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;

    public GroupService(GroupRepository groupRepository,
                        UtenteRepository utenteRepository,
                        UserGroupMembershipRepository userGroupMembershipRepository) {
        this.groupRepository = groupRepository;
        this.utenteRepository = utenteRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
    }

    @Transactional
    public GroupDto createGroup(NuovoGruppoRequest nuovoGruppoRequest, Long userId) {

        Utente utente = utenteRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        groupRepository.getGroupByName(nuovoGruppoRequest.getName())
                .ifPresent(g -> {
                    throw new RuntimeException("Gruppo già esistente");
                });

        Group group = new Group();
        group.setName(nuovoGruppoRequest.getName());

        // Il cuore del gruppo pulsante
        UserGroupMembership membership = new UserGroupMembership();
        membership.setGroup(group);
        membership.setUtente(utente);
        membership.setStatus(Status.NON_PAGATO);
        membership.setIsAdmin(true);
        membership.setJoinedAt(LocalDateTime.now());

        // **IMPORTANTE** – Mantieni la relazione viva
        group.getUserMemberships().add(membership);

        // **Magia del cascade**: salva solo il gruppo, e Hibernate salverà anche la membership
        Group savedGroup = groupRepository.save(group);

        return mapToDto(savedGroup);
    }

    @Transactional
    public void addUserToGroup(AddUserToGroupRequest request) {
        try {

            System.out.println(request.getUserId());
            System.out.println(request.getGroupId());

            // Cerca l'utente tra le stelle del database, altrimenti getta un grido nell’oscurità
            Utente user = utenteRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Trova il gruppo, come un faro nell’oceano dei dati
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            // Se l'utente è già nell'orbita del gruppo, nessun bisogno di duplicare la costellazione
//            if (userGroupMembershipRepository.existsByUtenteAndGroup(user, group)) {
//                throw new RuntimeException("User is already a member of this group");
//            }

            // Forgia un nuovo legame, come un'alleanza di anime
            UserGroupMembership membership = new UserGroupMembership();
            membership.setUtente(user);
            membership.setGroup(group);

            // Lo status, come un presagio: se non esiste, lascialo dormire in uno stato primordiale
            membership.setStatus(
                    request.getStatus() != null ? request.getStatus() : Status.NON_PAGATO
            );

            // L’amministratore – custode del fuoco sacro – se non esiste, è un semplice viaggiatore
            membership.setIsAdmin(
                    request.getIsAdmin() != null ? request.getIsAdmin() : false
            );

            // Segna il tempo dell’unione, come un’ora segreta nella notte
            membership.setJoinedAt(LocalDateTime.now());

            // Scolpisci questo legame nel marmo del database
            userGroupMembershipRepository.save(membership);

            // Un’ode al log – canta la nascita di un nuovo membro
            log.info("Added user {} to group {} with status {}",
                    user.getUsername(), group.getName(), membership.getStatus());

        } catch (RuntimeException ex) {
            // Se l’incanto si spezza, raccontalo in un sussurro al log
            log.error("Error adding user to group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }


    @Transactional
    public void updateMembershipStatus(UpdateMembershipStatusRequest request) {
        try {
            Utente user = utenteRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

            membership.setStatus(request.getNewStatus());
            userGroupMembershipRepository.save(membership);

            log.info("Updated membership status for user {} in group {} to {}",
                    user.getUsername(), group.getName(), request.getNewStatus());

        } catch (RuntimeException ex) {
            log.error("Error updating membership status: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {
        try {
            Utente user = utenteRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            UserGroupMembership membership = userGroupMembershipRepository.findByUtenteAndGroup(user, group)
                    .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

            userGroupMembershipRepository.delete(membership);
            log.info("Removed user {} from group {}", user.getUsername(), group.getName());

        } catch (RuntimeException ex) {
            log.error("Error removing user from group: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private GroupDto mapToDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setId(group.getId()); // Aggiungi questo!
        groupDto.setName(group.getName());
        groupDto.setUserMemberships(group.getUserMemberships());
        return groupDto;
    }
}