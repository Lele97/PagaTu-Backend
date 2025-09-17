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
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupMonitoringService {

    private final TokenForUserPasswordResetRepository tokenRepository;

    /**
     * Get statistics about tokens in the system using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public TokenStatistics getTokenStatistics() {
        try {

            log.debug("Getting token statistics");

            long totalTokens;
            long activeTokens;
            long expiredTokens;
            long expiredActiveTokens;

            LocalDateTime currentTime = LocalDateTime.now();

            totalTokens = tokenRepository.count();
            activeTokens = tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE).size();
            expiredTokens = tokenRepository.findAllByTokenStatus(TokenStatus.EXPIRED).size();
            expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();


            TokenStatistics stats = new TokenStatistics(totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            log.debug("Token statistics  Total={}, Active={}, Expired={}, ExpiredActive={}"
                    , totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            return stats;

        } catch (Exception e) {
            log.error("Error retrieving token statistics {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve token statistics", e);
        }
    }

    /**
     * Check if there are tokens that need cleanup using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public boolean hasTokensToCleanup() {
        try {

            log.debug("Checking for tokens to cleanup");

            LocalDateTime currentTime = LocalDateTime.now();
            long expiredActiveTokens;


            expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();


            boolean hasTokensToCleanup = expiredActiveTokens > 0;
            log.debug("has {} expired active tokens to cleanup", expiredActiveTokens);

            return hasTokensToCleanup;
        } catch (Exception e) {
            log.error("Error checking for tokens to cleanup for {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Perform batch cleanup of expired tokens using profile-specific repository.
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {

            log.debug("Starting token cleanup");

            LocalDateTime currentTime = LocalDateTime.now();
            int updatedCount;


            updatedCount = tokenRepository.updateExpiredTokensStatus(
                    TokenStatus.EXPIRED, currentTime, TokenStatus.ACTIVE);


            log.info("Token cleanup completed: {} tokens updated", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("Error during token cleanup for {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cleanup expired tokens", e);
        }
    }

    /**
     * Get count of tokens by status using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public long getTokenCountByStatus(TokenStatus status) {
        try {
            log.debug("Getting token count by status {}", status);

            long count = tokenRepository.findAllByTokenStatus(status).size();

            log.debug("has {} tokens with status {}", count, status);
            return count;

        } catch (Exception e) {
            log.error("Error getting token count by status {} {}",
                    status, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get count of recent tokens for a specific email using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public long getRecentTokenCount(String email, LocalDateTime since) {
        try {

            log.debug("Getting recent token count for email {} since {}", email, since);

            long count = tokenRepository.countRecnetTokensByEmail(email, since);

            log.debug("has {} recent tokens for email {} since {}", count, email, since);
            return count;

        } catch (Exception e) {
            log.error("Error getting recent token count for email {} {}",
                    email, e.getMessage(), e);
            return 0;
        }
    }
}
