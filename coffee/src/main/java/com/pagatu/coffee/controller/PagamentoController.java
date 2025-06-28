package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.entity.NuovoPagamentoRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffee")
@Slf4j
public class PagamentoController {
    private final PagamentoService pagamentoService;
    private final JwtService jwtService;

    public PagamentoController(PagamentoService pagamentoService, JwtService jwtService) {
        this.pagamentoService = pagamentoService;
        this.jwtService = jwtService;
    }

    @PostMapping("/pagamento")
    public ResponseEntity<PagamentoDto> registraPagamento(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NuovoPagamentoRequest request, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.registraPagamento(userId, groupNme, request));
    }

    @PostMapping("/salta/pagamento")
    public ResponseEntity<String> saltaPagamentoDto(@RequestHeader("Authorization") String authHeader, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        pagamentoService.saltaPagamento(userId, groupNme);
        return ResponseEntity.ok("User: " + userId + " ha saltato il pagamento");
    }
}
