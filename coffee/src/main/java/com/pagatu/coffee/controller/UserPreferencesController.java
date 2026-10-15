package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.UserPreferencesDto;
import com.pagatu.coffee.dto.UserPreferencesRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffee/user")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final PreferencesService preferencesService;
    private final JwtService jwtService;

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> getPreferences(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(preferencesService.getPreferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> updatePreferences(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserPreferencesRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(preferencesService.updatePreferences(userId, request));
    }
}
