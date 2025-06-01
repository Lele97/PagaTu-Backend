package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.NuovoGruppoRequest;
import com.pagatu.coffee.jwt.jwtUtil;
import com.pagatu.coffee.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/coffee/group")
public class GroupController {

    private final GroupService groupService;
    private final jwtUtil jwtUtil;

    public GroupController(GroupService groupService, jwtUtil jwtUtil) {
        this.groupService = groupService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody NuovoGruppoRequest nuovoGruppoRequest,
            @RequestHeader("Authorization") String authHeader) {
        Long user_id = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        return ResponseEntity.ok(groupService.createGroup(nuovoGruppoRequest, user_id));
    }

    @DeleteMapping("/delete/{groupName}")
    public ResponseEntity<String> deleteGroupByName(@PathVariable String groupName,@RequestHeader("Authorization") String authHeader) throws Exception {
        Long user_id = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        groupService.deleteGroupByName(groupName,user_id);
        return ResponseEntity.ok("Delete group: " + groupName);
    }
}