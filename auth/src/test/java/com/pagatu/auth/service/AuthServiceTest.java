package com.pagatu.auth.service;

import com.pagatu.auth.dto.LoginRequest;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.exception.*;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import com.pagatu.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-jwt-testing-123456".getBytes());

    @Mock
    private OutboxService outboxService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private WebClient webClient;

    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        authService = new AuthService(
                tokenForUserPasswordResetRepository,
                userRepository,
                passwordEncoder,
                webClientBuilder,
                "http://localhost:8082",
                outboxService,
                emailVerificationService
        );

        ReflectionTestUtils.setField(authService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "natsSubject", "reset-password-mail");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
        testUser.setEmailVerified(true);
        testUser.setAuthProvider(AuthProvider.LOCAL);
    }

    @Test
    void login_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> authService.login(new LoginRequest("unknown", "password")));
    }

    @Test
    void login_WhenPasswordInvalid_ShouldThrowAuthenticationException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class,
                () -> authService.login(new LoginRequest("testuser", "wrong")));
    }

    @Test
    void login_WhenCredentialsValid_ShouldReturnToken() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        // When
        LoginResponse response = authService.login(new LoginRequest("testuser", "password"));

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.username());
        assertEquals("test@example.com", response.email());
        assertNotNull(response.token());
        assertFalse(response.token().isBlank());
    }

    @Test
    void register_WhenUsernameExists_ShouldThrowUserAlreadyExistsException() {
        // Given
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WhenEmailExists_ShouldThrowUserAlreadyExistsException() {
        // Given
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WhenDataValid_ShouldSaveUser() {
        // Given
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newuser", userCaptor.getValue().getUsername());
        assertEquals("new@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void sendEmailForResetPassword_WhenEmailNotFound_ShouldThrowUserNotFoundException() {
        // Given
        when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> authService.sendEmailForResetPassword("missing@example.com"));
    }

    @Test
    void sendEmailForResetPassword_WhenRateLimitExceeded_ShouldThrowRateLimiterException() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(tokenForUserPasswordResetRepository.countRecnetTokensByEmail(eq("test@example.com"), any()))
                .thenReturn(10L);

        // When & Then
        assertThrows(RateLimiterException.class,
                () -> authService.sendEmailForResetPassword("test@example.com"));
    }

    @Test
    void sendEmailForResetPassword_WhenValid_ShouldCreateTokenAndPublishEvent() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(tokenForUserPasswordResetRepository.countRecnetTokensByEmail(eq("test@example.com"), any()))
                .thenReturn(0L);
        when(tokenForUserPasswordResetRepository.save(any(TokenForUserPasswordReset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.sendEmailForResetPassword("test@example.com");

        // Then
        verify(tokenForUserPasswordResetRepository).save(any(TokenForUserPasswordReset.class));
        verify(outboxService).saveEvent(eq("reset-password-mail"), any(ResetPasswordMailEvent.class));
    }

    @Test
    void validateResetTokenAndGetEmail_WhenTokenIsNull_ShouldThrowInvalidTokenException() {
        // When & Then
        assertThrows(InvalidTokenException.class,
                () -> authService.validateResetTokenAndGetEmail(null));
    }

    @Test
    void validateResetTokenAndGetEmail_WhenTokenUsed_ShouldThrowInvalidTokenException() {
        // Given
        TokenForUserPasswordReset token = new TokenForUserPasswordReset();
        token.setToken("used-token");
        token.setEmail("test@example.com");
        token.setTokenStatus(TokenStatus.USED);
        token.setExpiredDate(LocalDateTime.now().plusHours(1));

        when(tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken("used-token"))
                .thenReturn(Optional.of(token));

        // When & Then
        assertThrows(InvalidTokenException.class,
                () -> authService.validateResetTokenAndGetEmail("used-token"));
    }

    @Test
    void validateResetTokenAndGetEmail_WhenTokenExpired_ShouldThrowTokenExpiredException() {
        // Given
        TokenForUserPasswordReset token = new TokenForUserPasswordReset();
        token.setToken("expired-token");
        token.setEmail("test@example.com");
        token.setTokenStatus(TokenStatus.ACTIVE);
        token.setExpiredDate(LocalDateTime.now().minusMinutes(1));

        when(tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken("expired-token"))
                .thenReturn(Optional.of(token));

        // When & Then
        assertThrows(TokenExpiredException.class,
                () -> authService.validateResetTokenAndGetEmail("expired-token"));
        verify(tokenForUserPasswordResetRepository).save(token);
        assertEquals(TokenStatus.EXPIRED, token.getTokenStatus());
    }

    @Test
    void validateResetTokenAndGetEmail_WhenTokenValid_ShouldReturnEmail() {
        // Given
        TokenForUserPasswordReset token = new TokenForUserPasswordReset();
        token.setToken("valid-token");
        token.setEmail("test@example.com");
        token.setTokenStatus(TokenStatus.ACTIVE);
        token.setExpiredDate(LocalDateTime.now().plusHours(1));

        when(tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken("valid-token"))
                .thenReturn(Optional.of(token));

        // When
        String email = authService.validateResetTokenAndGetEmail("valid-token");

        // Then
        assertEquals("test@example.com", email);
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.getByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        User result = authService.getUserByEmail("test@example.com");

        // Then
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByEmail_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Given
        when(userRepository.getByEmail("missing@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> authService.getUserByEmail("missing@example.com"));
    }

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Password1!");
        request.setEmail("new@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setFirstName("Mario");
        request.setLastName("Rossi");
        return request;
    }
}