package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.*;
import com.pagatu.coffee.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffee")
@RequiredArgsConstructor
public class ExpansionController {

    private final GroupBalanceService groupBalanceService;
    private final GroupRulesService groupRulesService;
    private final GamificationService gamificationService;
    private final ProfileService profileService;
    private final JwtService jwtService;
    private final UserStatisticsService userStatisticsService;

    @PostMapping("/bilancio/gruppo")
    public ResponseEntity<GroupBalanceDto> getGroupBalance(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GroupBalanceRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(groupBalanceService.calculateBalance(userId, request));
    }

    @GetMapping("/regole/gruppo")
    public ResponseEntity<GroupRulesDto> getGroupRules(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("groupName") String groupName) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(groupRulesService.getRules(userId, groupName));
    }

    @PutMapping("/regole/gruppo")
    public ResponseEntity<GroupRulesDto> updateGroupRules(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GroupRulesRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(groupRulesService.updateRules(userId, request));
    }

    @PostMapping("/gamification/gruppo")
    public ResponseEntity<GroupGamificationDto> getGroupGamification(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GamificationRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(gamificationService.getGroupGamification(userId, request));
    }

    @GetMapping("/user/payment-links")
    public ResponseEntity<PaymentLinksDto> getPaymentLinks(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(profileService.getPaymentLinks(userId));
    }

    @PutMapping("/user/payment-links")
    public ResponseEntity<PaymentLinksDto> updatePaymentLinks(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PaymentLinksRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(profileService.updatePaymentLinks(userId, request));
    }

    @PutMapping("/user/coffeekarma")
    public ResponseEntity<String> coffe_karma_update(@RequestHeader("Authorization") String authHeader, @RequestBody CoffeeKarmaRequest request){
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        userStatisticsService.computeKarma(userId, request);
        return ResponseEntity.ok("Il valore coffee karma dell'utente è stato aggiornato");
    }
}