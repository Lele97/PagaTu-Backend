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

/**
 * REST controller for managing coffee payment operations.
 * <p>
 * This controller provides endpoints for:
 * <ul>
 *   <li>Registering new payments</li>
 *   <li>Skipping payment turns</li>
 *   <li>Getting payment statistics and rankings</li>
 *   <li>Making payments on behalf of others</li>
 *   <li>Retrieving payment history</li>
 * </ul>
 * </p>
 * All endpoints require JWT authentication via Authorization header.
 */
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

    /**
     * Registers a new coffee payment for the authenticated user.
     * <p>
     * This endpoint allows a user to register that they have made a coffee payment
     * for their group. After registration, the system will automatically determine
     * who should pay next and send notification events via Kafka.
     * </p>
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param request the payment details including amount and description
     * @param groupNme the name of the group for which the payment is being made
     * @return ResponseEntity containing the created payment details
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.UserNotInGroup if the user is not a member of the group
     */
    @PostMapping("/pagamento")
    public ResponseEntity<PagamentoDto> registraPagamento(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NuovoPagamentoRequest request, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.registraPagamento(userId, groupNme, request));
    }

    /**
     * Allows a user to skip their turn to pay for coffee.
     * <p>
     * When a user skips their payment turn, the system will automatically
     * select the next person to pay and send appropriate notifications.
     * The user who skipped will be marked with SALTATO status.
     * </p>
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param groupNme the name of the group where the user wants to skip payment
     * @return ResponseEntity with confirmation message
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.UserNotInGroup if the user is not a member of the group
     */
    @PostMapping("/salta/pagamento")
    public ResponseEntity<String> saltaPagamentoDto(@RequestHeader("Authorization") String authHeader, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        pagamentoService.saltaPagamento(userId, groupNme);
        return ResponseEntity.ok("User: " + userId + " ha saltato il pagamento");
    }

    /**
     * Retrieves payment statistics and rankings for a specific group.
     * <p>
     * This endpoint provides a ranking/leaderboard of users within a group
     * based on their payment history, including total amounts paid,
     * number of payments, and other statistical information.
     * </p>
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param classificaPagamentiPerGruppoRequest the request containing group details for statistics
     * @return ResponseEntity containing the payment statistics and rankings for the group
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.NoContentAvailableException if no payments exist for the group
     */
    @PostMapping("/pagamenti/classifica")
    public ResponseEntity<List<ClassificaPagamentiPerGruppoDto>> classificaPagamentiPerGruppo(@RequestHeader("Authorization") String authHeader, @RequestBody ClassificaPagamentiPerGruppoRequest classificaPagamentiPerGruppoRequest) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.getClassificaPagamentiPerGruppo(userId, classificaPagamentiPerGruppoRequest));
    }

    /**
     * Allows a user to make a payment on behalf of another group member.
     * <p>
     * This endpoint enables a user to pay for coffee not only for themselves
     * but also cover the turn of the person who should currently pay.
     * Both the payer and the person they're paying for will be marked as paid.
     * </p>
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param request the payment details including amount and description
     * @param groupNme the name of the group for which the payment is being made
     * @return ResponseEntity containing the created payment details
     * @throws com.pagatu.coffee.exception.UserNotFoundException if the user is not found
     * @throws com.pagatu.coffee.exception.GroupNotFoundException if the group is not found
     * @throws com.pagatu.coffee.exception.UserNotInGroup if the user is not a member of the group
     */
    @PostMapping("/pagamento/pagaPer")
    public ResponseEntity<PagamentoDto> pagamentoPer(@RequestHeader("Authorization") String authHeader, @RequestBody NuovoPagamentoRequest request, @RequestParam("groupNme") String groupNme) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(pagamentoService.pagaPer(userId, groupNme, request));
    }

    /**
     * Retrieves the recent payment history for a specific user.
     * <p>
     * This endpoint returns the payment history for the specified user.
     * Users can only access their own payment history (username must match the token).
     * The response includes payment details like amount, description, date, and group information.
     * </p>
     *
     * @param username the username for which to retrieve payment history
     * @param authHeader the JWT authorization header containing the user token
     * @return ResponseEntity containing the user's payment history or appropriate error response
     * @throws IllegalArgumentException if the authorization token is invalid
     * @throws RuntimeException if the user is not found
     */
    @PostMapping("/ultimi/pagamenti/{username}")
    public ResponseEntity<Object> ultimiPagamentoDtoPost(
            @PathVariable("username") String username,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
            log.info("Fetching payments for user {} (ID: {})", username, userId);

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
