package com.pagatu.auth.batch;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.exception.TokenCleanupBatchException;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import com.pagatu.auth.service.TokenCleanupMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch job component responsible for cleaning up expired password reset tokens.
 * Runs every 15 minutes to find and update expired tokens from ACTIVE to EXPIRED status.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupBatchJob {

    private final TokenForUserPasswordResetRepository tokenRepository;
    private final TokenCleanupMonitoringService tokenCleanupMonitoringService;

    /**
     * Scheduled method that runs every 15 minutes (900000 milliseconds).
     * Uses fixedRate to ensure consistent execution intervals.
     */
    @Scheduled(fixedRate = 900000)
    public void cleanupExpiredTokens() {

        log.info("Starting token cleanup batch job at {}", LocalDateTime.now());

        long startTime = System.currentTimeMillis();
        int processedTokens = 0;
        int updatedTokens = 0;
        TokenStatistics initialStats;
        TokenStatistics finalStats;

        try {

            initialStats = tokenCleanupMonitoringService.getTokenStatistics();

            log.info("Initial token statistics: {}", initialStats);

            if (!tokenCleanupMonitoringService.hasTokensToCleanup()) {
                log.info("No expired active tokens found, skipping cleanup");
                return;
            }

            updatedTokens = performBatchTokenCleanup();

            if (updatedTokens == 0) {
                log.info("Batch update returned 0 results, falling back to individual token processing");
                var result = processTokensIndividually();
                processedTokens = result.processed;
                updatedTokens = result.updated;
            } else {
                processedTokens = updatedTokens;
            }

            finalStats = tokenCleanupMonitoringService.getTokenStatistics();

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Token cleanup batch job completed successfully. " +
                            "Processed: {}, Updated: {}, Execution time: {} ms",
                    processedTokens, updatedTokens, executionTime);

            log.info("Final token statistics: {}", finalStats);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Token cleanup batch job failed after {} ms. " +
                            "Processed: {}, Updated: {}, Error: {}",
                    executionTime, processedTokens, updatedTokens, e.getMessage(), e);
            throw new TokenCleanupBatchException(e.getMessage());
        }
    }

    /**
     * Performs batch update of expired tokens using a single query.
     * This is more efficient for large datasets.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    protected int performBatchTokenCleanup() {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            log.debug("Executing batch update for tokens expired before {}", currentTime);

            int updatedCount;

            updatedCount = tokenRepository.updateExpiredTokensStatus(
                    TokenStatus.EXPIRED,
                    currentTime,
                    TokenStatus.ACTIVE
            );


            log.info("Batch update completed: {} tokens updated from ACTIVE to EXPIRED", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("Error during batch token cleanup: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fallback method that processes tokens individually.
     * Used when batch update fails or returns unexpected results.
     */
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    protected TokenProcessingResult processTokensIndividually() {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            log.debug("Retrieving active tokens for individual processing at {}", currentTime);

            List<TokenForUserPasswordReset> activeTokens = tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE);
            log.info("Found {} active tokens to process", activeTokens.size());

            int updatedCount = 0;

            for (TokenForUserPasswordReset token : activeTokens) {
                if (processSingleToken(token, currentTime)) {
                    updatedCount++;
                }
            }

            log.info("Individual token processing completed: {} out of {} tokens updated",
                    updatedCount, activeTokens.size());

            return new TokenProcessingResult(activeTokens.size(), updatedCount);

        } catch (Exception e) {
            log.error("Error during individual token processing: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Processes a single token, checking if it's expired and updating its status if needed.
     *
     * @param token the token to process
     * @param currentTime the current time for expiration comparison
     * @return true if the token was updated to expired status, false otherwise
     */
    private boolean processSingleToken(TokenForUserPasswordReset token, LocalDateTime currentTime) {
        try {
            if (isTokenExpired(token, currentTime)) {
                updateTokenToExpired(token);
                log.debug("Updated token ID {} to EXPIRED status (expired at: {})",
                        token.getId(), token.getExpiredDate());
                return true;
            } else {
                log.debug("Token ID {} is still active (expires at: {})",
                        token.getId(), token.getExpiredDate());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to process token ID {}: {}", token.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a token has expired by comparing its expiration date with current time.
     */
    private boolean isTokenExpired(TokenForUserPasswordReset token, LocalDateTime currentTime) {
        return token.getExpiredDate() != null && token.getExpiredDate().isBefore(currentTime);
    }

    /**
     * Updates a single token to EXPIRED status with retry mechanism.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 1.5))
    protected void updateTokenToExpired(TokenForUserPasswordReset token) {
        try {

            token.setTokenStatus(TokenStatus.EXPIRED);

            tokenRepository.save(token);

            log.debug("Successfully updated token ID {} to EXPIRED status", token.getId());

        } catch (Exception e) {
            log.error("Failed to update token ID {} to EXPIRED status: {}", token.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Manual trigger method for testing purposes.
     * Can be called directly to test the batch job functionality.
     */
    public void manualTrigger() {
        log.info("Manual trigger initiated for token cleanup batch job");
        cleanupExpiredTokens();
    }
}
