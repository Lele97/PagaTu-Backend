package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoDto;
import com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoRequest;
import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.entity.NuovoPagamentoRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/pagamenti/classifica")
    public ResponseEntity<List<ClassificaPagamentiPerGruppoDto>> classificaPagamentiPerGruppo(@RequestHeader("Authorization") String authHeader, @RequestBody ClassificaPagamentiPerGruppoRequest classificaPagamentiPerGruppoRequest) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.getClassificaPagamentiPerGruppo(userId, classificaPagamentiPerGruppoRequest));
    }

    @PostMapping("/pagamento/pagaPer")
    public ResponseEntity<PagamentoDto> pagamentoPer(@RequestHeader("Authorization") String authHeader, @RequestBody NuovoPagamentoRequest request, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.pagaPer(userId, groupNme, request));
    }

    @PostMapping("/ultimi/pagamenti/{username}")
    public ResponseEntity<Object> ultimiPagamentoDtoPost(
            @PathVariable("username") String username,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Verify token is valid by extracting user ID (throws exception if invalid)
            Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
            log.info("Fetching payments for user {} (ID: {})", username, userId);

            // Verify the requested username matches the token's username
            String tokenUsername = jwtService.extractUsernameFromAuthHeader(authHeader);
            if (!username.equals(tokenUsername)) {
                log.warn("Authorization failed: User {} attempted to access payments for {}",
                        tokenUsername, username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Not authorized to access this user's payment history");
            }

            List<PagamentoDto> pagamenti = pagamentoService.getUltimiPagamentiByUsername(username);

            if (pagamenti.isEmpty()) {
                log.info("No payments found for user {}", username);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            return ResponseEntity.ok(pagamenti);

        } catch (IllegalArgumentException e) {
            log.error("Invalid authorization token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization token");
        } catch (Exception e) {
            log.error("Error retrieving payments for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to retrieve payment history");
        }
    }
}
