package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupSettingsDto;
import com.pagatu.coffee.dto.GroupSettingsRequest;
import com.pagatu.coffee.service.GroupSettingsService;
import com.pagatu.coffee.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffee/group")
@RequiredArgsConstructor
public class GroupSettingsController {

    private final GroupSettingsService groupSettingsService;
    private final JwtService jwtService;

    @GetMapping("/settings")
    public ResponseEntity<GroupSettingsDto> getSettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("groupName") String groupName) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(groupSettingsService.getSettings(userId, groupName));
    }

    @PutMapping("/settings")
    public ResponseEntity<GroupSettingsDto> updateSettings(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GroupSettingsRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(groupSettingsService.updateSettings(userId, request));
    }
}