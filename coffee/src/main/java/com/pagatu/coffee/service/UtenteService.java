package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UtenteService {

    private final UtenteRepository utenteRepository;
    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository membershipRepository;
    private final GroupService groupService;

    public UtenteService(UtenteRepository utenteRepository,
                         GroupRepository groupRepository,
                         UserGroupMembershipRepository membershipRepository,
                         GroupService groupService) {
        this.utenteRepository = utenteRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.groupService = groupService;
    }

    @Transactional
    public UtenteDto createUtente(UtenteDto utenteDto) {

        log.info("Creating/updating user: {}", utenteDto);

        Optional<Utente> existingByAuthId = utenteRepository.findByAuthId(utenteDto.getAuthId());

        if (existingByAuthId.isPresent()) {
            Utente existing = existingByAuthId.get();
            log.info("Existing user found by authId: {}", utenteDto.getAuthId());
            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());
            if (userGroupMemberships != null)
                createGroupAndAddUserToTheGroup(existing, userGroupMemberships);
            return mapToDto(existing);
        }

        Optional<Utente> existingByUsername = utenteRepository.findByUsername(utenteDto.getUsername());

        if (existingByUsername.isPresent()) {
            Utente existing = existingByUsername.get();
            existing.setAuthId(utenteDto.getAuthId());
            existing.setEmail(utenteDto.getEmail());
            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());
            if (userGroupMemberships != null)
                createGroupAndAddUserToTheGroup(existing, userGroupMemberships);
            Utente updated = utenteRepository.save(existing);
            log.info("Updated existing user by username: {}", utenteDto.getUsername());
            return mapToDto(updated);
        }

        Utente utente = new Utente();
        utente.setAuthId(utenteDto.getAuthId());
        utente.setUsername(utenteDto.getUsername());
        utente.setEmail(utenteDto.getEmail());
        utente.setName(utenteDto.getName());
        utente.setLastname(utenteDto.getLastname());
        Utente savedUser = utenteRepository.save(utente);

        List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());

        if (userGroupMemberships != null && !userGroupMemberships.isEmpty())
            createGroupAndAddUserToTheGroup(savedUser, userGroupMemberships);

        log.info("New user created: {}", savedUser.getUsername());

        return mapToDto(savedUser);
    }

    private List<GroupMembershipDto> convertGroupsToMemberships(List<String> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(groupName -> {
                    Optional<Group> groupOpt = groupRepository.getGroupByName(groupName);
                    GroupMembershipDto membership = new GroupMembershipDto();

                    // Prendi l’id solo se esiste
                    Long groupId = groupOpt.map(Group::getId).orElse(null);
                    membership.setGroupId(groupId);

                    membership.setGroupName(groupName);
                    membership.setStatus(Status.NON_PAGATO);
                    membership.setIsAdmin(true);
                    membership.setJoinedAt(null);

                    return membership;
                })
                .toList();
    }

    private void createGroupAndAddUserToTheGroup(Utente user, List<GroupMembershipDto> membershipDtos) {

        for (GroupMembershipDto membershipDto : membershipDtos) {

            try {

                if (groupRepository.getGroupByName(membershipDto.getGroupName()).isEmpty()) {
                    NuovoGruppoRequest nuovoGruppoRequest = new NuovoGruppoRequest();
                    nuovoGruppoRequest.setName(membershipDto.getGroupName());
                    GroupDto createdGroup = groupService.createGroup(nuovoGruppoRequest, user.getAuthId());
                    log.info("Creating new group: {} with ID: {} for user ID: {}", membershipDto.getGroupName(), createdGroup.getId(), user.getId());
                    log.info("Processed membership for user {} in group {}", user.getUsername(), membershipDto.getGroupName());
                }
            } catch (Exception e) {
                log.error("Error processing membership for group {}: {}", membershipDto.getGroupName(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<Utente> findByUsername(String username) {
        return utenteRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<UtenteDetailDto> findUtenteByUsername(String username) {
        return utenteRepository.findUserByUsername(username);
    }

    @Transactional
    public Utente findByEmail(String email) {
        return utenteRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Utente> findAll() {
        return utenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Utente> findById(Long id) {
        return utenteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Utente> getUsersByGroupAndStatus(Long groupId, Status status) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return membershipRepository.findByGroupAndStatus(group, status)
                .stream()
                .map(UserGroupMembership::getUtente)
                .toList();
    }

    private UtenteDto mapToDto(Utente utente) {
        UtenteDto dto = new UtenteDto();
        dto.setId(utente.getId());
        dto.setAuthId(utente.getAuthId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setName(utente.getName());
        dto.setLastname(utente.getLastname());

        // 🌟 Converte solo i nomi dei gruppi
        if (utente.getGroupMemberships() != null) {
            List<String> groupNames = utente.getGroupMemberships().stream()
                    .map(membership -> membership.getGroup().getName())
                    .toList();
            dto.setGroups(groupNames);
        }

        return dto;
    }
}