package com.pagatu.auth.service;

import com.pagatu.auth.batch.TokenStatistics;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.repository.DevTokenForUserPasswordResetRepository;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileAwareTokenCleanupMonitoringService {
    private final Environment environment;
    private final TokenForUserPasswordResetRepository tokenRepository;
    private final DevTokenForUserPasswordResetRepository devTokenRepository;

    /**
     * Get statistics about tokens in the system using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public TokenStatistics getTokenStatistics() {
        try {
            String profile = getActiveProfile();
            log.debug("Getting token statistics for profile: {}", profile);

            long totalTokens;
            long activeTokens;
            long expiredTokens;
            long expiredActiveTokens;

            LocalDateTime currentTime = LocalDateTime.now();

            if (isDevProfile()) {
                // Use dev repository
                totalTokens = devTokenRepository.count();
                activeTokens = devTokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE).size();
                expiredTokens = devTokenRepository.findAllByTokenStatus(TokenStatus.EXPIRED).size();
                expiredActiveTokens = devTokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            } else {
                // Use production repository (second datasource)
                totalTokens = tokenRepository.count();
                activeTokens = tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE).size();
                expiredTokens = tokenRepository.findAllByTokenStatus(TokenStatus.EXPIRED).size();
                expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            }

            TokenStatistics stats = new TokenStatistics(totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            log.debug("Token statistics for profile {}: Total={}, Active={}, Expired={}, ExpiredActive={}",
                    profile, totalTokens, activeTokens, expiredTokens, expiredActiveTokens);

            return stats;

        } catch (Exception e) {
            log.error("Error retrieving token statistics for profile {}: {}", getActiveProfile(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve token statistics", e);
        }
    }

    /**
     * Check if there are tokens that need cleanup using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public boolean hasTokensToCleanup() {
        try {
            String profile = getActiveProfile();
            log.debug("Checking for tokens to cleanup for profile: {}", profile);

            LocalDateTime currentTime = LocalDateTime.now();
            long expiredActiveTokens;

            if (isDevProfile()) {
                expiredActiveTokens = devTokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            } else {
                expiredActiveTokens = tokenRepository.findExpiredActiveTokens(TokenStatus.ACTIVE, currentTime).size();
            }

            boolean hasTokensToCleanup = expiredActiveTokens > 0;
            log.debug("Profile {} has {} expired active tokens to cleanup", profile, expiredActiveTokens);

            return hasTokensToCleanup;
        } catch (Exception e) {
            log.error("Error checking for tokens to cleanup for profile {}: {}", getActiveProfile(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Perform batch cleanup of expired tokens using profile-specific repository.
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            String profile = getActiveProfile();
            log.debug("Starting token cleanup for profile: {}", profile);

            LocalDateTime currentTime = LocalDateTime.now();
            int updatedCount;

            if (isDevProfile()) {
                updatedCount = devTokenRepository.updateExpiredTokensStatus(
                        TokenStatus.EXPIRED, currentTime, TokenStatus.ACTIVE);
            } else {
                updatedCount = tokenRepository.updateExpiredTokensStatus(
                        TokenStatus.EXPIRED, currentTime, TokenStatus.ACTIVE);
            }

            log.info("Token cleanup completed for profile {}: {} tokens updated", profile, updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("Error during token cleanup for profile {}: {}", getActiveProfile(), e.getMessage(), e);
            throw new RuntimeException("Failed to cleanup expired tokens", e);
        }
    }

    /**
     * Get count of tokens by status using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public long getTokenCountByStatus(TokenStatus status) {
        try {
            String profile = getActiveProfile();
            log.debug("Getting token count by status {} for profile: {}", status, profile);

            long count = isDevProfile()
                    ? devTokenRepository.findAllByTokenStatus(status).size()
                    : tokenRepository.findAllByTokenStatus(status).size();

            log.debug("Profile {} has {} tokens with status {}", profile, count, status);
            return count;

        } catch (Exception e) {
            log.error("Error getting token count by status {} for profile {}: {}",
                    status, getActiveProfile(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get count of recent tokens for a specific email using profile-specific repository.
     */
    @Transactional(readOnly = true)
    public long getRecentTokenCount(String email, LocalDateTime since) {
        try {
            String profile = getActiveProfile();
            log.debug("Getting recent token count for email {} since {} for profile: {}", email, since, profile);

            long count = isDevProfile()
                    ? devTokenRepository.countRecnetTokensByEmail(email, since)
                    : tokenRepository.countRecnetTokensByEmail(email, since);

            log.debug("Profile {} has {} recent tokens for email {} since {}", profile, count, email, since);
            return count;

        } catch (Exception e) {
            log.error("Error getting recent token count for email {} for profile {}: {}",
                    email, getActiveProfile(), e.getMessage(), e);
            return 0;
        }
    }

    // Profile detection methods
    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(",", profiles) : "default";
    }
}
