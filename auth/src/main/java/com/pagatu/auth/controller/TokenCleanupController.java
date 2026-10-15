package com.pagatu.auth.controller;

import com.pagatu.auth.batch.TokenCleanupBatchJob;
import com.pagatu.auth.batch.TokenStatistics;
import com.pagatu.auth.service.TokenCleanupMonitoringService;
import com.pagatu.auth.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing and monitoring token cleanup operations.
 * Provides endpoints for manual triggering and statistics retrieval.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH)
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupController {

    private final TokenCleanupBatchJob batchJob;
    private final TokenCleanupMonitoringService tokenCleanupMonitoringService;

    /**
     * Get current token statistics.
     */
    @GetMapping(Constants.STATISTICS_ENDPOINT)
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            TokenStatistics stats = tokenCleanupMonitoringService.getTokenStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put(Constants.TOTAL_TOKENS, stats.getTotalTokens());
            response.put(Constants.ACTIVE_TOKENS, stats.getActiveTokens());
            response.put(Constants.EXPIRED_TOKENS, stats.getExpiredTokens());
            response.put(Constants.EXPIRED_ACTIVE_TOKENS, stats.getExpiredActiveTokens());
            response.put(Constants.NEEDS_CLEANUP, tokenCleanupMonitoringService.hasTokensToCleanup());
            response.put(Constants.TIMESTAMP, java.time.LocalDateTime.now());

            log.info("Token statistics requested: {}", stats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving token statistics: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constants.ERROR, Constants.RETRIEVE_STATS_FAILED_MSG);
            errorResponse.put(Constants.MESSAGE, e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger the token cleanup batch job.
     * This endpoint should be restricted to admin users only.
     */
    @PostMapping(Constants.TRIGGER_ENDPOINT)
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {

            log.info("Manual token cleanup triggered via REST endpoint");

            TokenStatistics beforeStats = tokenCleanupMonitoringService.getTokenStatistics();

            batchJob.manualTrigger();

            TokenStatistics afterStats = tokenCleanupMonitoringService.getTokenStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put(Constants.SUCCESS, true);
            response.put(Constants.MESSAGE, Constants.CLEANUP_SUCCESS_MSG);
            response.put(Constants.BEFORE_STATS, Map.of(
                    Constants.TOTAL_TOKENS, beforeStats.getTotalTokens(),
                    Constants.ACTIVE_TOKENS, beforeStats.getActiveTokens(),
                    Constants.EXPIRED_TOKENS, beforeStats.getExpiredTokens(),
                    Constants.EXPIRED_ACTIVE_TOKENS, beforeStats.getExpiredActiveTokens()
            ));
            response.put(Constants.AFTER_STATS, Map.of(
                    Constants.TOTAL_TOKENS, afterStats.getTotalTokens(),
                    Constants.ACTIVE_TOKENS, afterStats.getActiveTokens(),
                    Constants.EXPIRED_TOKENS, afterStats.getExpiredTokens(),
                    Constants.EXPIRED_ACTIVE_TOKENS, afterStats.getExpiredActiveTokens()
            ));
            response.put(Constants.TOKENS_PROCESSED, beforeStats.getExpiredActiveTokens());
            response.put(Constants.TIMESTAMP, java.time.LocalDateTime.now());

            log.info("Manual token cleanup completed. Before: {}, After: {}", beforeStats, afterStats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during manual token cleanup: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constants.SUCCESS, false);
            errorResponse.put(Constants.ERROR, Constants.CLEANUP_FAILED_MSG);
            errorResponse.put(Constants.MESSAGE, e.getMessage());
            errorResponse.put(Constants.TIMESTAMP, java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint for the token cleanup system.
     */
    @GetMapping(Constants.HEALTH_ENDPOINT)
    public ResponseEntity<Map<String, Object>> healthCheck() {

        try {
            boolean needsCleanup = tokenCleanupMonitoringService.hasTokensToCleanup();
            TokenStatistics stats = tokenCleanupMonitoringService.getTokenStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put(Constants.STATUS, Constants.STATUS_HEALTHY);
            response.put(Constants.NEEDS_CLEANUP, needsCleanup);
            response.put(Constants.EXPIRED_ACTIVE_TOKENS, stats.getExpiredActiveTokens());
            response.put(Constants.TIMESTAMP, java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constants.STATUS, Constants.STATUS_UNHEALTHY);
            errorResponse.put(Constants.ERROR, e.getMessage());
            errorResponse.put(Constants.TIMESTAMP, java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
