package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.exception.GroupNotFoundException;
import com.pagatu.coffee.exception.UserNotFoundException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Base service providing common user and group lookup operations with consistent exception handling.
 * <p>
 * This service centralizes user and group retrieval logic with proper exception handling.
 * It serves as a foundation for other services that need to perform user or group lookups
 * with consistent error handling across the application.
 * </p>
 * <p>
 * All methods in this service throw appropriate exceptions when entities are not found,
 * ensuring consistent error handling across the application. Each method follows the pattern
 * of returning the entity if found, or throwing a specific exception if not found.
 * </p>
 * 
 * @author PagaTu Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class BaseUserService {

    private final CoffeeUserRepository coffeeUserRepository;
    private final GroupRepository groupRepository;

    /**
     * Finds a user by authentication ID (JWT claim).
     *
     * @param authId the authentication identifier from the auth service
     * @return the matching {@link CoffeeUser}
     * @throws UserNotFoundException if no user exists for the given auth ID
     */
    public CoffeeUser findUserByAuthId(Long authId) {
        return coffeeUserRepository.findByAuthId(authId)
                .orElseThrow(() -> new UserNotFoundException("User not found with auth ID: " + authId));
    }

    /**
     * Find user by username with proper exception handling
     *
     * @param username Username
     * @return CoffeeUser entity
     * @throws UserNotFoundException if user not found
     */
    public CoffeeUser findUserByUsername(String username) {
        return coffeeUserRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    /**
     * Find group by name with proper exception handling
     *
     * @param groupName Group name
     * @return Group entity
     * @throws GroupNotFoundException if group not found
     */
    public Group findGroupByName(String groupName) {
        return groupRepository.getGroupByName(groupName)
                .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupName));
    }

    /**
     * Find group with memberships by name
     *
     * @param groupName Group name
     * @return Group entity with memberships loaded
     * @throws GroupNotFoundException if group not found
     */
    public Group findGroupWithMembershipsByName(String groupName) {
        return groupRepository.findGroupWithMembershipsByName(groupName)
                .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupName));
    }
}
