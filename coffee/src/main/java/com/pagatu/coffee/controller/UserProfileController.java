package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.UserAwardDto;
import com.pagatu.coffee.dto.UserProfileDto;
import com.pagatu.coffee.dto.UserProfileRequest;
import com.pagatu.coffee.dto.UserStatisticsDto;
import com.pagatu.coffee.service.AwardService;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.ProfileService;
import com.pagatu.coffee.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coffee/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final ProfileService profileService;
    private final UserStatisticsService userStatisticsService;
    private final AwardService awardService;
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

    @GetMapping("/statistics")
    public ResponseEntity<UserStatisticsDto> getStatistics(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(userStatisticsService.getStatistics(userId));
    }

    @GetMapping("/awards")
    public ResponseEntity<List<UserAwardDto>> getAwards(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(awardService.listAndSync(userId));
    }
}