package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.ThemePresetDto;
import com.pagatu.coffee.dto.UserPreferencesDto;
import com.pagatu.coffee.dto.UserPreferencesRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PreferencesService;
import com.pagatu.coffee.service.ThemePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coffee/user")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final PreferencesService preferencesService;
    private final ThemePresetService themePresetService;
    private final JwtService jwtService;

    @GetMapping("/theme-presets")
    public ResponseEntity<List<ThemePresetDto>> getThemePresets(
            @RequestHeader("Authorization") String authHeader) {
        jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(themePresetService.getPresets());
    }

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