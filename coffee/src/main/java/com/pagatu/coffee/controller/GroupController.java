package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.InvitationRequest;
import com.pagatu.coffee.dto.NuovoGruppoRequest;
import com.pagatu.coffee.jwt.jwtUtil;
import com.pagatu.coffee.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/coffee/group")
public class GroupController {

    private final GroupService groupService;
    private final JwtService jwtService;

    public GroupController(GroupService groupService, JwtService jwtService) {
        this.groupService = groupService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody NuovoGruppoRequest nuovoGruppoRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        GroupDto group = groupService.createGroup(nuovoGruppoRequest, userId);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/delete/{groupName}")
    public ResponseEntity<String> deleteGroupByName(
            @PathVariable String groupName,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.deleteGroupByName(groupName, userId);
        return ResponseEntity.ok("Group '" + groupName + "' deleted successfully");
    }

    @PutMapping("/update/addtogroup")
    public ResponseEntity<String> addUserToGroup(
            @RequestParam("username") String username,
            @RequestParam("groupName") String groupName) {
        groupService.addUserToGroup(groupName, username);
        return ResponseEntity.ok("User '" + username + "' added to group '" + groupName + "' successfully");
    }

    @PostMapping("/update/invitation")
    public ResponseEntity<String> sendInvitationToGroup(
            @RequestBody InvitationRequest invitationRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        groupService.sendInvitationToGroup(userId, invitationRequest);
        return ResponseEntity.ok("Invitation sent to user '" + invitationRequest.getUsername() + "' successfully");
    }

    @GetMapping("/get/{username}")
    public ResponseEntity<String> getGroupsByUsername(@PathVariable("username") String username) {
        try{
            groupService.getGroupsByUsername(username);
            int size = groupService.getGroupsByUsername(username).size();
            return ResponseEntity.ok("Successfully retrieved "+ size +" groups by username: "+username);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unable to retrieve groups by username: "+username);
        }
    }
}
