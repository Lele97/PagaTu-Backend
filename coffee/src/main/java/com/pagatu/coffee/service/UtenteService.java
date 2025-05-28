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

            // Update group memberships if provided
            if (utenteDto.getGroupMemberships() != null) {
                updateUserGroupMemberships(existing, utenteDto.getGroupMemberships());
            }

            return mapToDto(existing);
        }

        // Check if user exists by username
        Optional<Utente> existingByUsername = utenteRepository.findByUsername(utenteDto.getUsername());
        if (existingByUsername.isPresent()) {
            Utente existing = existingByUsername.get();
            existing.setAuthId(utenteDto.getAuthId());
            existing.setEmail(utenteDto.getEmail());

            // Update group memberships
            if (utenteDto.getGroupMemberships() != null) {
                updateUserGroupMemberships(existing, utenteDto.getGroupMemberships());
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

        // Process group memberships with the saved user ID
        if (utenteDto.getGroupMemberships() != null) {
            updateUserGroupMemberships(savedUser, utenteDto.getGroupMemberships());
        }

        log.info("New user created: {}", savedUser.getUsername());
        return mapToDto(savedUser);
    }

    @Transactional
    public void addUserToGroup(AddUserToGroupRequest request) {
        Utente user = utenteRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if membership already exists
        if (membershipRepository.existsByUtenteAndGroup(user, group)) {
            throw new RuntimeException("User is already a member of this group");
        }

        // Create new membership
        UserGroupMembership membership = new UserGroupMembership(user, group, request.getStatus());
        membership.setIsAdmin(request.getIsAdmin());
        membershipRepository.save(membership);

        log.info("Added user {} to group {} with status {}", user.getUsername(), group.getName(), request.getStatus());
    }

    @Transactional
    public void updateMembershipStatus(UpdateMembershipStatusRequest request) {
        Utente user = utenteRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        UserGroupMembership membership = membershipRepository.findByUtenteAndGroup(user, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        membership.setStatus(request.getNewStatus());
        membershipRepository.save(membership);

        log.info("Updated membership status for user {} in group {} to {}",
                user.getUsername(), group.getName(), request.getNewStatus());
    }

    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {
        Utente user = utenteRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        UserGroupMembership membership = membershipRepository.findByUtenteAndGroup(user, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        membershipRepository.delete(membership);
        log.info("Removed user {} from group {}", user.getUsername(), group.getName());
    }

    private void updateUserGroupMemberships(Utente user, List<GroupMembershipDto> membershipDtos) {
        // Get current memberships
        List<UserGroupMembership> currentMemberships = membershipRepository.findByUtente(user);
        Map<String, UserGroupMembership> currentMembershipMap = currentMemberships.stream()
                .collect(Collectors.toMap(m -> m.getGroup().getName(), m -> m));

        // Process new memberships
        for (GroupMembershipDto membershipDto : membershipDtos) {
            Group group = findOrCreateGroup(membershipDto.getGroupName(), user.getId());

            UserGroupMembership existingMembership = currentMembershipMap.get(membershipDto.getGroupName());

            if (existingMembership != null) {
                // Update existing membership
                existingMembership.setStatus(membershipDto.getStatus());
                if (membershipDto.getIsAdmin() != null) {
                    existingMembership.setIsAdmin(membershipDto.getIsAdmin());
                }
                membershipRepository.save(existingMembership);
            } else {
                // Create new membership
                UserGroupMembership newMembership = new UserGroupMembership();
                newMembership.setUtente(user);
                newMembership.setGroup(group);
                newMembership.setStatus(membershipDto.getStatus() != null ? membershipDto.getStatus() : Status.NON_PAGATO);
                newMembership.setIsAdmin(membershipDto.getIsAdmin() != null ? membershipDto.getIsAdmin() : false);
                membershipRepository.save(newMembership);
            }
        }
    }

    private Group findOrCreateGroup(String groupName, Long userId) {
        return groupRepository.getGroupByName(groupName)
                .orElseGet(() -> {
                    log.info("Creating new group: {} for user ID: {}", groupName, userId);
                    try {
                        NuovoGruppoRequest nuovoGruppoRequest = new NuovoGruppoRequest();
                        nuovoGruppoRequest.setName(groupName);
                        groupService.createGroup(nuovoGruppoRequest, userId);
                        return groupRepository.getGroupByName(groupName)
                                .orElseThrow(() -> new RuntimeException("Failed to create group: " + groupName));
                    } catch (Exception e) {
                        log.error("Error creating group through GroupService: {}", e.getMessage());
                        Group newGroup = new Group();
                        newGroup.setName(groupName);
                        return groupRepository.save(newGroup);
                    }
                });
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

        // Convert memberships to DTOs
        if (utente.getGroupMemberships() != null) {
            List<GroupMembershipDto> membershipDtos = utente.getGroupMemberships().stream()
                    .map(this::mapMembershipToDto)
                    .toList();
            dto.setGroupMemberships(membershipDtos);
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