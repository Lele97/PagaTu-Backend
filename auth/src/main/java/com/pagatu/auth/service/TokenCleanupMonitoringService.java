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
}
