package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.UserProfileDto;
import com.pagatu.coffee.dto.UserProfileRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffee/user")
@RequiredArgsConstructor
public class UserProfileController {

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
            @RequestBody UserProfileRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }
}