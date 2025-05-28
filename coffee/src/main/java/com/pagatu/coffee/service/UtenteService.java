package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.*;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

        // Check if user exists by authId
        Optional<Utente> existingByAuthId = utenteRepository.findByAuthId(utenteDto.getAuthId());
        if (existingByAuthId.isPresent()) {
            Utente existing = existingByAuthId.get();
            log.info("Existing user found by authId: {}", utenteDto.getAuthId());


            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());


            // Update group memberships if provided
            if (userGroupMemberships != null) {
                updateUserGroupMemberships(existing, userGroupMemberships);
            }

            return mapToDto(existing);
        }

        // Check if user exists by username
        Optional<Utente> existingByUsername = utenteRepository.findByUsername(utenteDto.getUsername());
        if (existingByUsername.isPresent()) {
            Utente existing = existingByUsername.get();
            existing.setAuthId(utenteDto.getAuthId());
            existing.setEmail(utenteDto.getEmail());

            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());

            // Update group memberships
            if (userGroupMemberships != null) {
                updateUserGroupMemberships(existing, userGroupMemberships);
            }

            Utente updated = utenteRepository.save(existing);
            log.info("Updated existing user by username: {}", utenteDto.getUsername());
            return mapToDto(updated);
        }

        // Create new user
        Utente utente = new Utente();
        utente.setAuthId(utenteDto.getAuthId());
        utente.setUsername(utenteDto.getUsername());
        utente.setEmail(utenteDto.getEmail());
        utente.setName(utenteDto.getName());
        utente.setLastname(utenteDto.getLastname());

        // Save user first to generate ID
        Utente savedUser = utenteRepository.save(utente);

        System.out.println(utente.getGroupMemberships());

        List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(utenteDto.getGroups());

        if (userGroupMemberships != null && !userGroupMemberships.isEmpty()) {
            updateUserGroupMemberships(savedUser, userGroupMemberships);
        }

        log.info("New user created: {}", savedUser.getUsername());
        return mapToDto(savedUser);
    }

//    @Transactional
//    public void addUserToGroup(AddUserToGroupRequest request) {
//        // Delegate to GroupService for proper entity creation and relationship management
//        try {
//            log.info("Adding user {} to group {} with status {}",
//                    request.getUserId(), request.getGroupId(), request.getStatus());
//
//            // Delegate to GroupService - you need to implement this method there
//            groupService.addUserToGroup(request);
//
//        } catch (Exception e) {
//            log.error("Error adding user to group: {}", e.getMessage());
//            throw new RuntimeException("Failed to add user to group: " + e.getMessage());
//        }
//    }

    @Transactional
    public void updateMembershipStatus(UpdateMembershipStatusRequest request) {
        // Delegate to GroupService for proper membership management
        try {
            log.info("Updating membership status for user {} in group {} to {}",
                    request.getUserId(), request.getGroupId(), request.getNewStatus());

            // Delegate to GroupService - you need to implement this method there
            groupService.updateMembershipStatus(request);

        } catch (Exception e) {
            log.error("Error updating membership status: {}", e.getMessage());
            throw new RuntimeException("Failed to update membership status: " + e.getMessage());
        }
    }

    private List<GroupMembershipDto> convertGroupsToMemberships(List<String> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(groupName -> {
                    Optional<Group> groupOpt = groupRepository.getGroupByName(groupName);
                    GroupMembershipDto membership = new GroupMembershipDto();

                    membership.setGroupId(String.valueOf(groupOpt.map(Group::getId).orElse(null))); // qui prendiamo l'id
                    membership.setGroupName(groupName);
                    membership.setStatus(Status.NON_PAGATO);
                    membership.setIsAdmin(false);
                    membership.setJoinedAt(null);

                    return membership;
                })
                .toList();
    }

    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {
        // Delegate to GroupService for proper membership management
        try {
            log.info("Removing user {} from group {}", userId, groupId);

            // Delegate to GroupService - you need to implement this method there
            groupService.removeUserFromGroup(userId, groupId);

        } catch (Exception e) {
            log.error("Error removing user from group: {}", e.getMessage());
            throw new RuntimeException("Failed to remove user from group: " + e.getMessage());
        }
    }

    private void updateUserGroupMemberships(Utente user, List<GroupMembershipDto> membershipDtos) {
        for (GroupMembershipDto membershipDto : membershipDtos) {
            try {

                // Controlla se il gruppo esiste
                Optional<Group> existingGroup = groupRepository.getGroupByName(membershipDto.getGroupName());
                Long groupId;

                if (existingGroup.isEmpty()) {
                    // Se il gruppo non esiste, crealo
                    // here groupId is null !!!!!!
                    log.info("Creating new group: {} for user ID: {}", membershipDto.getGroupName(), user.getId());
                    NuovoGruppoRequest nuovoGruppoRequest = new NuovoGruppoRequest();
                    nuovoGruppoRequest.setName(membershipDto.getGroupName());
                    GroupDto createdGroup = groupService.createGroup(nuovoGruppoRequest, user.getId());
                    groupId = createdGroup.getId();
                } else {
                    groupId = existingGroup.get().getId();
                }

                System.out.println("AAAAA"+groupId);

                // Aggiungi o aggiorna la membership delegando al GroupService
                AddUserToGroupRequest addRequest = new AddUserToGroupRequest();
                addRequest.setUserId(user.getId());
                addRequest.setGroupId(groupId);
                addRequest.setStatus(membershipDto.getStatus() != null ? membershipDto.getStatus() : Status.NON_PAGATO);
                addRequest.setIsAdmin(membershipDto.getIsAdmin() != null ? membershipDto.getIsAdmin() : false);

                groupService.addUserToGroup(addRequest);

                log.info("Processed membership for user {} in group {}", user.getUsername(), membershipDto.getGroupName());

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

    private GroupMembershipDto mapMembershipToDto(UserGroupMembership membership) {
        GroupMembershipDto dto = new GroupMembershipDto();
        dto.setGroupName(membership.getGroup().getName());
        dto.setStatus(membership.getStatus());
        dto.setIsAdmin(membership.getIsAdmin());
        dto.setJoinedAt(membership.getJoinedAt());
        return dto;
    }
}