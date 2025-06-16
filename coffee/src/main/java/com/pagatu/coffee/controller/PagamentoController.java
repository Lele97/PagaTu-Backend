package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.entity.NuovoPagamentoRequest;
import com.pagatu.coffee.jwt.jwtUtil;
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
    private final jwtUtil jwtUtil;

    public PagamentoController(PagamentoService pagamentoService, jwtUtil jwtUtil) {
        this.pagamentoService = pagamentoService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/pagamento")
    public ResponseEntity<PagamentoDto> registraPagamento(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NuovoPagamentoRequest request, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        return ResponseEntity.ok(pagamentoService.registraPagamento(userId, groupNme, request));
    }

    @PostMapping("/salta/pagamento")
    public ResponseEntity<String> saltaPagamentoDto(@RequestHeader("Authorization") String authHeader, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        pagamentoService.saltaPagamento(userId,groupNme);
        return ResponseEntity.ok("User: "+ userId+ " ha saltato il pagamento");
    }
}
