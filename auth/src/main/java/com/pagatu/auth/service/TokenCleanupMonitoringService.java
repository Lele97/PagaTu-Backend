package com.pagatu.auth.service;

import com.pagatu.auth.batch.TokenStatistics;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for monitoring and managing token cleanup operations related to password reset tokens.
 * Provides statistical information about token statuses and identifies tokens requiring cleanup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupMonitoringService {

    private final TokenForUserPasswordResetRepository tokenRepository;

    /**
     * Retrieves comprehensive statistics about password reset tokens in the system.
     * Counts total tokens, active tokens, expired tokens, and expired-active tokens (active tokens past their expiration).
     *
     * @return TokenStatistics object containing counts of different token statuses
     * @throws RuntimeException if an error occurs during data retrieval
     * @see TokenStatistics
     */
    @Transactional(readOnly = true)
    public TokenStatistics getTokenStatistics() {
        try {
            log.debug("Getting token statistics");

            LocalDateTime currentTime = LocalDateTime.now();
            long totalTokens = tokenRepository.count();
            long activeTokens = tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE).size();
            long expiredTokens = tokenRepository.findAllByTokenStatus(TokenStatus.EXPIRED).size();
            long expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();

            TokenStatistics stats = new TokenStatistics(totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            log.debug("Token statistics - Total={}, Active={}, Expired={}, ExpiredActive={}",
                    totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            return stats;

        } catch (Exception e) {
            log.error("Error retrieving token statistics {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve token statistics", e);
        }
    }

    /**
     * Checks whether there are expired-active tokens that require cleanup.
     * An expired-active token is one marked as ACTIVE but past its expiration timestamp.
     *
     * @return true if there are expired-active tokens requiring cleanup, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasTokensToCleanup() {
        try {
            log.debug("Checking for tokens to cleanup");

            LocalDateTime currentTime = LocalDateTime.now();
            long expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();

            boolean hasTokensToCleanup = expiredActiveTokens > 0;
            log.debug("Found {} expired active tokens to cleanup", expiredActiveTokens);

            return hasTokensToCleanup;
        } catch (Exception e) {
            log.error("Error checking for tokens to cleanup: {}", e.getMessage(), e);
            return false;
        }
    }
}