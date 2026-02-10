package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NewGroupRequest;
import com.pagatu.coffee.exception.UserNotInGroup;
import com.pagatu.coffee.service.GroupService;
import com.pagatu.coffee.service.JwtService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing coffee payment groups.
 * <p>
 * This controller provides endpoints for group management operations including:
 * <ul>
 * <li>Creating new coffee payment groups</li>
 * <li>Deleting groups (with proper authorization)</li>
 * <li>Adding users to existing groups</li>
 * <li>Sending group invitations</li>
 * <li>Retrieving user's group memberships</li>
 * </ul>
 * </p>
 * <p>
 * All endpoints require proper authentication and authorization.
 * Group operations are restricted to authorized users only.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("api/coffee/group")
public class GroupController {

    private final GroupService groupService;
    private final JwtService jwtService;

    public GroupController(GroupService groupService, JwtService jwtService) {
        this.groupService = groupService;
        this.jwtService = jwtService;
    }

    /**
     * Creates a new coffee payment group.
     *
     * @param newGroupRequest the request containing group name and description
     * @param authHeader      JWT authorization header
     * @return ResponseEntity containing the created group information
     */
    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody NewGroupRequest newGroupRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        GroupDto group = groupService.createGroup(newGroupRequest, userId);
        return ResponseEntity.ok(group);
    }

    /**
     * Deletes a group by name.
     *
     * @param groupName  the name of the group to delete
     * @param authHeader JWT authorization header
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/delete/{groupName}")
    public ResponseEntity<String> deleteGroupByName(
            @PathVariable String groupName,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.deleteGroupByName(groupName, userId);
        return ResponseEntity.ok("Group '" + groupName + "' deleted successfully");
    }

    /**
     * Adds a user to an existing group.
     *
     * @param username  the username of the user to add
     * @param groupName the name of the group to add the user to
     * @return ResponseEntity with success message
     */
    @PutMapping("/update/addtogroup")
    public ResponseEntity<String> addUserToGroup(
            @RequestParam("username") String username,
            @RequestParam("groupName") String groupName) {
        groupService.addUserToGroup(groupName, username);
        return ResponseEntity.ok("User '" + username + "' added to group '" + groupName + "' successfully");
    }

    /**
     * Sends an invitation to a user to join a group.
     *
     * @param invitationRequest the invitation details including username and group
     * @param authHeader        JWT authorization header
     * @return ResponseEntity with success message
     */
    @PostMapping("/update/invitation")
    public ResponseEntity<String> sendInvitationToGroup(
            @RequestBody InvitationRequest invitationRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.sendInvitationToGroup(userId, invitationRequest);
        return ResponseEntity.ok("Invitation sent to user '" + invitationRequest.getUsername() + "' successfully");
    }

    /**
     * Retrieves all groups that a user is member of.
     *
     * @param username   the username of the user whose groups are being retrieved
     * @param authHeader the JWT authorization header containing the bearer token
     * @return ResponseEntity containing the list of groups or appropriate error
     *         response
     */
    @PostMapping("/get/{username}")
    public ResponseEntity<Object> getGroupsByUsernamePost(
            @PathVariable("username") String username,
            @RequestHeader("Authorization") String authHeader) {
        try {

            Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);

            log.info("Token valid for user: '{}'", userId);

            String tokenUsername = jwtService.extractUsernameFromAuthHeader(authHeader);
            if (!username.equals(tokenUsername)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Not authorized to access this user's groups");
            }

            List<GroupDto> groups = groupService.getGroupsByUsername(username);

            if (groups.isEmpty()) {
                throw new UserNotInGroup("l'utente non è presente in nessun gruppo");
            }
            return ResponseEntity.ok(groups);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization token");
        }
    }
}
