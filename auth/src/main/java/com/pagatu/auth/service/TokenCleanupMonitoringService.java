package com.pagatu.auth.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class providing monitoring and statistics for token cleanup operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupMonitoringService {

    private final TokenForUserPasswordResetRepository tokenRepository;

    /**
     * Get statistics about tokens in the system.
     */
    @Transactional(value = "secondTransactionManager", readOnly = true)
    public TokenStatistics getTokenStatistics() {
        try {
            long totalTokens = tokenRepository.count();
            long activeTokens = tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE).size();
            long expiredTokens = tokenRepository.findAllByTokenStatus(TokenStatus.EXPIRED).size();
            
            LocalDateTime currentTime = LocalDateTime.now();
            long expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            
            TokenStatistics stats = new TokenStatistics(totalTokens, activeTokens, expiredTokens, expiredActiveTokens);
            
            log.debug("Token statistics: Total={}, Active={}, Expired={}, ExpiredActive={}", 
                     totalTokens, activeTokens, expiredTokens, expiredActiveTokens);
            
            return stats;
            
        } catch (Exception e) {
            log.error("Error retrieving token statistics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve token statistics", e);
        }
    }

    /**
     * Check if there are tokens that need cleanup.
     */
    @Transactional(value = "secondTransactionManager", readOnly = true)
    public boolean hasTokensToCleanup() {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            long expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            return expiredActiveTokens > 0;
        } catch (Exception e) {
            log.error("Error checking for tokens to cleanup: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Data class representing token statistics.
     */
    public static class TokenStatistics {
        private final long totalTokens;
        private final long activeTokens;
        private final long expiredTokens;
        private final long expiredActiveTokens;

        public TokenStatistics(long totalTokens, long activeTokens, long expiredTokens, long expiredActiveTokens) {
            this.totalTokens = totalTokens;
            this.activeTokens = activeTokens;
            this.expiredTokens = expiredTokens;
            this.expiredActiveTokens = expiredActiveTokens;
        }

        public long getTotalTokens() {
            return totalTokens;
        }

        public long getActiveTokens() {
            return activeTokens;
        }

        public long getExpiredTokens() {
            return expiredTokens;
        }

        public long getExpiredActiveTokens() {
            return expiredActiveTokens;
        }

        @Override
        public String toString() {
            return String.format("TokenStatistics{total=%d, active=%d, expired=%d, expiredActive=%d}", 
                               totalTokens, activeTokens, expiredTokens, expiredActiveTokens);
        }
    }
}
