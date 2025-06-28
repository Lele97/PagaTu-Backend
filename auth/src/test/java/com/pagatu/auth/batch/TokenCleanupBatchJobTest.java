package com.pagatu.auth.batch;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import com.pagatu.auth.service.TokenCleanupMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenCleanupBatchJob.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class TokenCleanupBatchJobTest {

    @Mock
    private TokenForUserPasswordResetRepository tokenRepository;

    @Mock
    private TokenCleanupMonitoringService monitoringService;

    @InjectMocks
    private TokenCleanupBatchJob batchJob;

    private TokenForUserPasswordReset expiredToken;
    private TokenForUserPasswordReset activeToken;

    @BeforeEach
    void setUp() {
        // Create test tokens
        expiredToken = new TokenForUserPasswordReset();
        expiredToken.setId(1L);
        expiredToken.setToken("expired-token");
        expiredToken.setEmail("test@example.com");
        expiredToken.setTokenStatus(TokenStatus.ACTIVE);
        expiredToken.setExpiredDate(LocalDateTime.now().minusHours(1));
        expiredToken.setCreatedAt(LocalDateTime.now().minusHours(2));

        activeToken = new TokenForUserPasswordReset();
        activeToken.setId(2L);
        activeToken.setToken("active-token");
        activeToken.setEmail("test2@example.com");
        activeToken.setTokenStatus(TokenStatus.ACTIVE);
        activeToken.setExpiredDate(LocalDateTime.now().plusHours(1));
        activeToken.setCreatedAt(LocalDateTime.now().minusMinutes(30));
    }

    @Test
    void testCleanupExpiredTokens_BatchUpdate_Success() {
        // Given
        TokenStatistics stats =
                new TokenStatistics(2, 2, 0, 1);

        when(monitoringService.getTokenStatistics()).thenReturn(stats);
        when(monitoringService.hasTokensToCleanup()).thenReturn(true);
        when(tokenRepository.updateExpiredTokensStatus(
                eq(TokenStatus.EXPIRED),
                any(LocalDateTime.class),
                eq(TokenStatus.ACTIVE)
        )).thenReturn(1);

        // When
        batchJob.cleanupExpiredTokens();

        // Then
        verify(monitoringService, times(2)).getTokenStatistics();
        verify(monitoringService).hasTokensToCleanup();
        verify(tokenRepository).updateExpiredTokensStatus(
                eq(TokenStatus.EXPIRED),
                any(LocalDateTime.class),
                eq(TokenStatus.ACTIVE)
        );
        verify(tokenRepository, never()).findAllByTokenStatus(any());
    }

    @Test
    void testCleanupExpiredTokens_NoTokensToCleanup() {
        // Given
        TokenStatistics stats =
                new TokenStatistics(2, 2, 0, 0);

        when(monitoringService.getTokenStatistics()).thenReturn(stats);
        when(monitoringService.hasTokensToCleanup()).thenReturn(false);

        // When
        batchJob.cleanupExpiredTokens();

        // Then
        verify(monitoringService).getTokenStatistics();
        verify(monitoringService).hasTokensToCleanup();
        verify(tokenRepository, never()).updateExpiredTokensStatus(any(), any(), any());
        verify(tokenRepository, never()).findAllByTokenStatus(any());
    }

    @Test
    void testCleanupExpiredTokens_FallbackToIndividualProcessing() {
        // Given
        TokenStatistics stats =
                new TokenStatistics(2, 2, 0, 1);
        List<TokenForUserPasswordReset> activeTokens = Arrays.asList(expiredToken, activeToken);

        when(monitoringService.getTokenStatistics()).thenReturn(stats);
        when(monitoringService.hasTokensToCleanup()).thenReturn(true);
        when(tokenRepository.updateExpiredTokensStatus(
                eq(TokenStatus.EXPIRED),
                any(LocalDateTime.class),
                eq(TokenStatus.ACTIVE)
        )).thenReturn(0);
        when(tokenRepository.findAllByTokenStatus(TokenStatus.ACTIVE)).thenReturn(activeTokens);
        when(tokenRepository.save(any(TokenForUserPasswordReset.class))).thenReturn(expiredToken);

        // When
        batchJob.cleanupExpiredTokens();

        // Then
        verify(tokenRepository).updateExpiredTokensStatus(
                eq(TokenStatus.EXPIRED),
                any(LocalDateTime.class),
                eq(TokenStatus.ACTIVE)
        );
        verify(tokenRepository).findAllByTokenStatus(TokenStatus.ACTIVE);
        verify(tokenRepository).save(expiredToken);
        verify(tokenRepository, never()).save(activeToken);
    }

    @Test
    void testManualTrigger() {
        // Given
        TokenStatistics stats =
                new TokenStatistics(0, 0, 0, 0);

        when(monitoringService.getTokenStatistics()).thenReturn(stats);
        when(monitoringService.hasTokensToCleanup()).thenReturn(false);

        // When
        batchJob.manualTrigger();

        // Then
        verify(monitoringService).hasTokensToCleanup();
    }
}
