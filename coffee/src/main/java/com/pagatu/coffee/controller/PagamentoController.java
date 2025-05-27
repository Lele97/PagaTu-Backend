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

    @PostMapping("/pagamenti")
    public ResponseEntity<PagamentoDto> registraPagamento(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NuovoPagamentoRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        return ResponseEntity.ok(pagamentoService.registraPagamento(userId, request));
    }

//    @GetMapping("/pagamenti/ultimi")
//    public ResponseEntity<List<PagamentoDto>> getUltimiPagamenti() {
//        return ResponseEntity.ok(pagamentoService.getUltimiPagamenti());
//    }
//
//    @GetMapping("/pagamenti/prossimo")
//    public ResponseEntity<ProssimoPagamentoDto> getProssimoPagatore() {
//        return ResponseEntity.ok(pagamentoService.getProssimoPagatore());
//    }
}
