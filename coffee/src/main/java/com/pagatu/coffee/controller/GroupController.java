package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NewGroupRequest;
import com.pagatu.coffee.dto.RemoveMemberRequest;
import com.pagatu.coffee.dto.TransferAdminRequest;
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

    /**
     * @param groupService group business logic
     * @param jwtService   JWT extraction from Authorization header
     */
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
        return ResponseEntity.ok("Gruppo '" + groupName + "' eliminato con successo");
    }

    /**
     * Accepts a group invitation and adds the user to the group.
     *
     * @param username     the username of the invited user
     * @param groupName    the name of the group to join
     * @param invitationId the identifier of the active invitation
     * @return ResponseEntity with success message
     */
    @PutMapping("/update/addtogroup")
    public ResponseEntity<String> addUserToGroup(
            @RequestParam("username") String username,
            @RequestParam("groupName") String groupName,
            @RequestParam("invitationId") Long invitationId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.addUserToGroup(groupName, username, invitationId, userId);
        return ResponseEntity.ok("Utente '" + username + "' aggiunto al gruppo '" + groupName + "' con successo");
    }

    /**
     * Rejects an invitation to join a group.
     *
     * @param username     the username of the user rejecting the invitation
     * @param groupName    the name of the group
     * @param invitationId the identifier of the active invitation
     * @return ResponseEntity with success message
     */
    @PutMapping("/update/rejectinvitation")
    public ResponseEntity<String> rejectInvitation(
            @RequestParam("username") String username,
            @RequestParam("groupName") String groupName,
            @RequestParam("invitationId") Long invitationId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.rejectInvitation(groupName, username, invitationId, userId);
        return ResponseEntity.ok("Invito al gruppo '" + groupName + "' rifiutato con successo");
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
            @Valid @RequestBody InvitationRequest invitationRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.sendInvitationToGroup(userId, invitationRequest);
        String target = invitationRequest.getEmail() != null ? invitationRequest.getEmail() : invitationRequest.getUsername();
        return ResponseEntity.ok("Invito inviato a '" + target + "' con successo");
    }

    @PutMapping("/update/leave")
    public ResponseEntity<String> leaveGroup(
            @RequestParam("groupName") String groupName,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.leaveGroup(groupName, userId);
        return ResponseEntity.ok("Hai lasciato il gruppo '" + groupName + "' con successo");
    }

    @DeleteMapping("/update/member")
    public ResponseEntity<String> removeMember(
            @Valid @RequestBody RemoveMemberRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.removeMember(userId, request.getGroupName(), request.getUsername());
        return ResponseEntity.ok("Membro '" + request.getUsername() + "' rimosso dal gruppo");
    }

    @PutMapping("/update/transfer-admin")
    public ResponseEntity<String> transferAdmin(
            @Valid @RequestBody TransferAdminRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.transferAdmin(userId, request.getGroupName(), request.getNewAdminUsername());
        return ResponseEntity.ok("Admin del gruppo trasferito a '" + request.getNewAdminUsername() + "'");
    }

    /**
     * Retrieves all groups that a user is member of.
     *
     * @param username   the username of the user whose groups are being retrieved
     * @param authHeader the JWT authorization header containing the bearer token
     * @return ResponseEntity containing the list of groups or appropriate error
     * response
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
                        .body("Non autorizzato ad accedere ai gruppi di questo utente");
            }

            List<GroupDto> groups = groupService.getGroupsByUsername(username);

            if (groups.isEmpty()) {
                throw new UserNotInGroup("l'utente non è presente in nessun gruppo");
            }
            return ResponseEntity.ok(groups);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token di autorizzazione non valido");
        }
    }
}
