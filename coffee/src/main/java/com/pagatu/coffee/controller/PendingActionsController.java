package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.PendingActionDto;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PendingActionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coffee/azioni-pendenti")
@RequiredArgsConstructor
public class PendingActionsController {

    private final PendingActionsService pendingActionsService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<PendingActionDto>> getPendingActions(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pendingActionsService.getPendingActions(userId));
    }
}