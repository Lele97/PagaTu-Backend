package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.PaymentStatus;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user operations and user-group relationships.
 * <p>
 * This service handles user creation, updates, and management of user
 * memberships
 * in coffee payment groups. It provides functionality for:
 * <ul>
 * <li>Creating and updating user profiles</li>
 * <li>Managing user authentication information</li>
 * <li>Handling user group memberships</li>
 * <li>Automatic group creation when users specify new groups</li>
 * </ul>
 * </p>
 * <p>
 * The service implements smart user resolution logic, first checking by authId,
 * then by username, ensuring proper user identity management across the system.
 * </p>
 */
@Service
@Slf4j
public class CoffeeUserService {

    private final CoffeeUserRepository coffeeUserRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    public CoffeeUserService(CoffeeUserRepository coffeeUserRepository,
            GroupRepository groupRepository,
            GroupService groupService) {
        this.coffeeUserRepository = coffeeUserRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
    }

    /**
     * Creates or updates a user based on authentication information.
     * <p>
     * This method implements smart user resolution logic:
     * <ol>
     * <li>First checks if user exists by authId (from JWT)</li>
     * <li>If not found, checks by username</li>
     * <li>If found by username, updates with authId</li>
     * <li>If not found at all, creates new user</li>
     * </ol>
     * Additionally handles group memberships by creating groups if they don't exist
     * and adding the user to them.
     * </p>
     *
     * @param coffeeUserDto the user data transfer object containing user
     *                      information
     * @return CoffeeUserDto the created or updated user information
     */
    @Transactional
    public CoffeeUserDto createCoffeeUser(CoffeeUserDto coffeeUserDto) {

        log.info("Creating/updating user: {}", coffeeUserDto);

        Optional<CoffeeUser> existingByAuthId = coffeeUserRepository.findByAuthId(coffeeUserDto.getAuthId());

        if (existingByAuthId.isPresent()) {
            CoffeeUser existing = existingByAuthId.get();
            log.info("Existing user found by authId: {}", coffeeUserDto.getAuthId());
            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(coffeeUserDto.getGroups());
            if (userGroupMemberships != null)
                createGroupAndAddUserToTheGroup(existing, userGroupMemberships);
            return mapToDto(existing);
        }

        Optional<CoffeeUser> existingByUsername = coffeeUserRepository.findByUsername(coffeeUserDto.getUsername());

        if (existingByUsername.isPresent()) {
            CoffeeUser existing = existingByUsername.get();
            existing.setAuthId(coffeeUserDto.getAuthId());
            existing.setEmail(coffeeUserDto.getEmail());
            List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(coffeeUserDto.getGroups());
            if (userGroupMemberships != null)
                createGroupAndAddUserToTheGroup(existing, userGroupMemberships);
            CoffeeUser updated = coffeeUserRepository.save(existing);
            log.info("Updated existing user by username: {}", coffeeUserDto.getUsername());
            return mapToDto(updated);
        }

        CoffeeUser coffeeUser = new CoffeeUser();
        coffeeUser.setAuthId(coffeeUserDto.getAuthId());
        coffeeUser.setUsername(coffeeUserDto.getUsername());
        coffeeUser.setEmail(coffeeUserDto.getEmail());
        coffeeUser.setName(coffeeUserDto.getName());
        coffeeUser.setLastname(coffeeUserDto.getLastname());
        CoffeeUser savedUser = coffeeUserRepository.save(coffeeUser);

        List<GroupMembershipDto> userGroupMemberships = convertGroupsToMemberships(coffeeUserDto.getGroups());

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
     * New users are set as admin with NOT_PAID status by default.
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

                    // Get ID only if exists
                    Long groupId = groupOpt.map(Group::getId).orElse(null);
                    membership.setGroupId(groupId);

                    membership.setGroupName(groupName);
                    membership.setStatus(PaymentStatus.NON_PAGATO);
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
     * @param user           the user to add to the groups
     * @param membershipDtos list of group memberships to process
     */
    private void createGroupAndAddUserToTheGroup(CoffeeUser user, List<GroupMembershipDto> membershipDtos) {

        for (GroupMembershipDto membershipDto : membershipDtos) {

            try {

                if (groupRepository.getGroupByName(membershipDto.getGroupName()).isEmpty()) {
                    NewGroupRequest newGroupRequest = new NewGroupRequest();
                    newGroupRequest.setName(membershipDto.getGroupName());
                    GroupDto createdGroup = groupService.createGroup(newGroupRequest, user.getAuthId());
                    log.info("Creating new group: {} with ID: {} for user ID: {}", membershipDto.getGroupName(),
                            createdGroup.getId(), user.getId());
                    log.info("Processed membership for user {} in group {}", user.getUsername(),
                            membershipDto.getGroupName());
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
     * @return Optional containing CoffeeUserDetailDto if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<CoffeeUserDetailDto> findCoffeeUserByUsername(String username) {
        return coffeeUserRepository.findUserByUsername(username);
    }

    /**
     * Finds a user by email address.
     * <p>
     * This method retrieves user information based on email address,
     * typically used for user lookup operations.
     * </p>
     *
     * @param email the email address to search for
     * @return CoffeeUser entity if found, null otherwise
     */
    @Transactional
    public CoffeeUser findByEmail(String email) {
        return coffeeUserRepository.findByEmail(email);
    }

    /**
     * Converts a user entity to a user DTO.
     * <p>
     * This method maps user entity data to a DTO format suitable for
     * client consumption, including group membership information.
     * </p>
     *
     * @param coffeeUser the user entity to convert
     * @return CoffeeUserDto containing user information for client use
     */
    private CoffeeUserDto mapToDto(CoffeeUser coffeeUser) {

        CoffeeUserDto dto = new CoffeeUserDto();
        dto.setId(coffeeUser.getId());
        dto.setAuthId(coffeeUser.getAuthId());
        dto.setUsername(coffeeUser.getUsername());
        dto.setEmail(coffeeUser.getEmail());
        dto.setName(coffeeUser.getName());
        dto.setLastname(coffeeUser.getLastname());

        if (coffeeUser.getGroupMemberships() != null) {
            List<String> groupNames = coffeeUser.getGroupMemberships().stream()
                    .map(membership -> membership.getGroup().getName())
                    .toList();
            dto.setGroups(groupNames);
        }
        return dto;
    }
}
