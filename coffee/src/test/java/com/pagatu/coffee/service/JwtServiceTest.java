package com.pagatu.coffee.service;

import com.pagatu.coffee.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtService jwtService;

    @Test
    void extractUserIdFromAuthHeader_WhenHeaderValid_ShouldReturnUserId() {
        // Given
        when(jwtUtil.getUserIdFromToken("token-value")).thenReturn(123L);

        // When
        Long userId = jwtService.extractUserIdFromAuthHeader("Bearer token-value");

        // Then
        assertEquals(123L, userId);
        verify(jwtUtil).getUserIdFromToken("token-value");
    }

    @Test
    void extractUserIdFromAuthHeader_WhenHeaderInvalid_ShouldThrowIllegalArgumentException() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> jwtService.extractUserIdFromAuthHeader("Invalid token"));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void extractUsernameFromAuthHeader_WhenHeaderValid_ShouldReturnUsername() {
        // Given
        when(jwtUtil.getUsernameFromToken("token-value")).thenReturn("testuser");

        // When
        String username = jwtService.extractUsernameFromAuthHeader("Bearer token-value");

        // Then
        assertEquals("testuser", username);
        verify(jwtUtil).getUsernameFromToken("token-value");
    }

    @Test
    void extractUsernameFromAuthHeader_WhenHeaderNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> jwtService.extractUsernameFromAuthHeader(null));
        verifyNoInteractions(jwtUtil);
    }
}