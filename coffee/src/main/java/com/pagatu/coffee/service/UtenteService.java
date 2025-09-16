package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user operations and user-group relationships.
 * <p>
 * This service handles user creation, updates, and management of user memberships
 * in coffee payment groups. It provides functionality for:
 * <ul>
 *   <li>Creating and updating user profiles</li>
 *   <li>Managing user authentication information</li>
 *   <li>Handling user group memberships</li>
 *   <li>Automatic group creation when users specify new groups</li>
 * </ul>
 * </p>
 * <p>
 * The service implements smart user resolution logic, first checking by authId,
 * then by username, ensuring proper user identity management across the system.
 * </p>
 */
@Service
@Slf4j
public class UtenteService {

    private final UtenteRepository utenteRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    public UtenteService(UtenteRepository utenteRepository,
                         GroupRepository groupRepository,
                         GroupService groupService) {
        this.utenteRepository = utenteRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
    }

    /**
     * Creates or updates a user based on authentication information.
     * <p>
     * This method implements smart user resolution logic:
     * <ol>
     *   <li>First checks if user exists by authId (from JWT)</li>
     *   <li>If not found, checks by username</li>
     *   <li>If found by username, updates with authId</li>
     *   <li>If not found at all, creates new user</li>
     * </ol>
     * Additionally handles group memberships by creating groups if they don't exist
     * and adding the user to them.
     * </p>
     *
     * @param utenteDto the user data transfer object containing user information
     * @return UtenteDto the created or updated user information
     */
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

    /**
     * Converts a list of group names to group membership DTOs.
     * <p>
     * This method creates membership information for each group name,
     * looking up existing groups and setting default membership properties.
     * New users are set as admin with NON_PAGATO status by default.
     * </p>
     *
     * @param groups list of group names to convert
     * @return List of GroupMembershipDto objects with default settings
     */
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

    /**
     * Creates groups that don't exist and adds the user to them.
     * <p>
     * This method processes group memberships, creating any groups that don't
     * already exist in the system. The user is automatically added as an admin
     * member to newly created groups.
     * </p>
     *
     * @param user the user to add to the groups
     * @param membershipDtos list of group memberships to process
     */
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

    /**
     * Finds detailed user information by username.
     * <p>
     * This method retrieves comprehensive user information including
     * group memberships and other details for display purposes.
     * </p>
     *
     * @param username the username to search for
     * @return Optional containing UtenteDetailDto if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<UtenteDetailDto> findUtenteByUsername(String username) {
        return utenteRepository.findUserByUsername(username);
    }

    /**
     * Finds a user by email address.
     * <p>
     * This method retrieves user information based on email address,
     * typically used for user lookup operations.
     * </p>
     *
     * @param email the email address to search for
     * @return Utente entity if found, null otherwise
     */
    @Transactional
    public Utente findByEmail(String email) {
        return utenteRepository.findByEmail(email);
    }

    /**
     * Converts a user entity to a user DTO.
     * <p>
     * This method maps user entity data to a DTO format suitable for
     * client consumption, including group membership information.
     * </p>
     *
     * @param utente the user entity to convert
     * @return UtenteDto containing user information for client use
     */
    private UtenteDto mapToDto(Utente utente) {

        UtenteDto dto = new UtenteDto();
        dto.setId(utente.getId());
        dto.setAuthId(utente.getAuthId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setName(utente.getName());
        dto.setLastname(utente.getLastname());

        if (utente.getGroupMemberships() != null) {
            List<String> groupNames = utente.getGroupMemberships().stream()
                    .map(membership -> membership.getGroup().getName())
                    .toList();
            dto.setGroups(groupNames);
        }
        return dto;
    }
}