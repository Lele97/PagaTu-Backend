package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.GroupPaymentRankingDto;
import com.pagatu.coffee.dto.GroupPaymentRankingRequest;
import com.pagatu.coffee.dto.PaymentDto;
import com.pagatu.coffee.entity.NewPaymentRequest;
import com.pagatu.coffee.service.JwtService;
import com.pagatu.coffee.service.PaymentService;
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
 * <li>Registering new payments</li>
 * <li>Skipping payment turns</li>
 * <li>Getting payment statistics and rankings</li>
 * <li>Making payments on behalf of others</li>
 * <li>Retrieving payment history</li>
 * </ul>
 * </p>
 * All endpoints require JWT authentication via Authorization header.
 */
@RestController
@RequestMapping("/api/coffee")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    public PaymentController(PaymentService paymentService, JwtService jwtService) {
        this.paymentService = paymentService;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new coffee payment for the authenticated user.
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param request    the payment details including amount and description
     * @param groupName  the name of the group for which the payment is being made
     * @return ResponseEntity containing the created payment details
     */
    @PostMapping("/pagamento")
    public ResponseEntity<PaymentDto> registerPayment(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NewPaymentRequest request,
            @RequestParam("groupNme") String groupName) {
        log.info("Richiesta di pagamento ricevuta. Body: {}, GroupName: {}", request, groupName);
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(paymentService.registerPayment(userId, groupName, request));
    }

    /**
     * Allows a user to skip their turn to pay for coffee.
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param groupName  the name of the group where the user wants to skip payment
     * @return ResponseEntity with confirmation message
     */
    @PostMapping("/salta/pagamento")
    public ResponseEntity<String> skipPayment(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("groupNme") String groupName) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        paymentService.skipPayment(userId, groupName);
        return ResponseEntity.ok("User: " + userId + " ha saltato il pagamento");
    }

    /**
     * Retrieves payment statistics and rankings for a specific group.
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param request    the request containing group details for statistics
     * @return ResponseEntity containing the payment statistics and rankings for the
     *         group
     */
    @PostMapping("/pagamenti/classifica")
    public ResponseEntity<List<GroupPaymentRankingDto>> getGroupPaymentRanking(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody GroupPaymentRankingRequest request) {
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(paymentService.getGroupPaymentRanking(userId, request));
    }

    /**
     * Allows a user to make a payment on behalf of another group member.
     *
     * @param authHeader the JWT authorization header containing the user token
     * @param request    the payment details including amount and description
     * @param groupName  the name of the group for which the payment is being made
     * @return ResponseEntity containing the created payment details
     */
    @PostMapping("/pagamento/pagaPer")
    public ResponseEntity<PaymentDto> payFor(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody NewPaymentRequest request,
            @RequestParam("groupNme") String groupName) {
        log.info("Richiesta di pagamento 'pagaPer' ricevuta. Body: {}, GroupName: {}", request, groupName);
        Long userId = jwtService.extractUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(paymentService.payFor(userId, groupName, request));
    }

    /**
     * Retrieves the recent payment history for a specific user.
     *
     * @param username   the username for which to retrieve payment history
     * @param authHeader the JWT authorization header containing the user token
     * @return ResponseEntity containing the user's payment history or appropriate
     *         error response
     */
    @PostMapping("/ultimi/pagamenti/{username}")
    public ResponseEntity<Object> getLatestPayments(
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

            List<PaymentDto> payments = paymentService.getLatestPaymentsByUsername(username);

            if (payments.isEmpty()) {
                log.info("No payments found for user {}", username);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            return ResponseEntity.ok(payments);

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
