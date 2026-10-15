package com.pagatu.auth.controller;

import com.pagatu.auth.dto.ChangePasswordRequest;
import com.pagatu.auth.dto.UpdateUserProfileRequest;
import com.pagatu.auth.dto.UserProfileDto;
import com.pagatu.auth.service.JwtService;
import com.pagatu.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        profileService.changePassword(userId, request);
        return ResponseEntity.ok("Password aggiornata con successo");
    }
}