package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.exception.GroupNotFoundException;
import com.pagatu.coffee.exception.UserNotFoundException;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Base service providing common user and group lookup operations.
 * <p>
 * This service centralizes user and group retrieval logic with proper
 * exception handling. It serves as a foundation for other services that
 * need to perform user or group lookups with consistent error handling.
 * </p>
 * <p>
 * All methods in this service throw appropriate exceptions when entities
 * are not found, ensuring consistent error handling across the application.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class BaseUserService {

    private final UtenteRepository utenteRepository;
    private final GroupRepository groupRepository;

    public Utente findUserByAuthId(Long authId) {
        return utenteRepository.findByAuthId(authId)
                .orElseThrow(() -> new UserNotFoundException("User not found with auth ID: " + authId));
    }

    /**
     * Find user by username with proper exception handling
     *
     * @param username Username
     * @return User entity
     * @throws UserNotFoundException if user not found
     */
    public Utente findUserByUsername(String username) {
        return utenteRepository.findByUsername(username)
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
