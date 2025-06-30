package com.pagatu.auth.controller;

import com.pagatu.auth.batch.TokenCleanupBatchJob;
import com.pagatu.auth.batch.TokenStatistics;
import com.pagatu.auth.service.TokenCleanupMonitoringService;
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
@RequestMapping("/api/admin/token-cleanup")
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupController {

    private final TokenCleanupBatchJob batchJob;
    private final TokenCleanupMonitoringService monitoringService;

    /**
     * Get current token statistics.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            TokenStatistics stats = monitoringService.getTokenStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalTokens", stats.getTotalTokens());
            response.put("activeTokens", stats.getActiveTokens());
            response.put("expiredTokens", stats.getExpiredTokens());
            response.put("expiredActiveTokens", stats.getExpiredActiveTokens());
            response.put("needsCleanup", monitoringService.hasTokensToCleanup());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("Token statistics requested: {}", stats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving token statistics: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger the token cleanup batch job.
     * This endpoint should be restricted to admin users only.
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {
            log.info("Manual token cleanup triggered via REST endpoint");
            
            // Get statistics before cleanup
            TokenStatistics beforeStats = monitoringService.getTokenStatistics();
            
            // Trigger the cleanup
            batchJob.manualTrigger();
            
            // Get statistics after cleanup
            TokenStatistics afterStats = monitoringService.getTokenStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token cleanup completed successfully");
            response.put("beforeStats", Map.of(
                "totalTokens", beforeStats.getTotalTokens(),
                "activeTokens", beforeStats.getActiveTokens(),
                "expiredTokens", beforeStats.getExpiredTokens(),
                "expiredActiveTokens", beforeStats.getExpiredActiveTokens()
            ));
            response.put("afterStats", Map.of(
                "totalTokens", afterStats.getTotalTokens(),
                "activeTokens", afterStats.getActiveTokens(),
                "expiredTokens", afterStats.getExpiredTokens(),
                "expiredActiveTokens", afterStats.getExpiredActiveTokens()
            ));
            response.put("tokensProcessed", beforeStats.getExpiredActiveTokens());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("Manual token cleanup completed. Before: {}, After: {}", beforeStats, afterStats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during manual token cleanup: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Token cleanup failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint for the token cleanup system.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        try {
            boolean needsCleanup = monitoringService.hasTokensToCleanup();
            TokenStatistics stats = monitoringService.getTokenStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("needsCleanup", needsCleanup);
            response.put("expiredActiveTokens", stats.getExpiredActiveTokens());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "unhealthy");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
