package com.pagatu.auth.service;

import com.pagatu.auth.dto.*;
import com.pagatu.auth.entity.EventType;
import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.event.TokenForgotPswUserEvent;
import com.pagatu.auth.event.UserEvent;
import com.pagatu.auth.repository.FirstUserRepository;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {
    @Value("${spring.kafka.topics.user-service}")
    private String USER_TOPIC;

    @Value("${spring.kafka.topics.resetPasswordMail}")
    private String RESET_PASSWORD_TOPIC;

    @Value("${spring.kafka.topics.user-service-reset-token}")
    private String TOKEN_FORGOT_USER_PSW;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${caffe.service.url}")
    private String caffeServiceUrl;

    @Qualifier("userEventKafkaTemplate")
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Qualifier("resetPasswordMailKafkaTemplate")
    private final KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail;

    @Qualifier("tokenForgotUserPasswordTemplate")
    private final KafkaTemplate<String, TokenForgotPswUserEvent> kafkaTemplateTokenForUserPasswordReset;

    private static final int LIMITER = 10;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final FirstUserRepository firstUserRepository;
    private final TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;

    public AuthService(
            FirstUserRepository firstUserRepository,
            TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository,
            PasswordEncoder passwordEncoder,
            WebClient.Builder webClientBuilder,
            @Qualifier("userEventKafkaTemplate") KafkaTemplate<String, UserEvent> kafkaTemplate,
            @Qualifier("resetPasswordMailKafkaTemplate") KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail,
            @Qualifier("tokenForgotUserPasswordTemplate") KafkaTemplate<String, TokenForgotPswUserEvent> kafkaTemplateTokenForUserPasswordReset) {

        this.firstUserRepository = firstUserRepository;
        this.tokenForUserPasswordResetRepository = tokenForUserPasswordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateResetPasswordMail = kafkaTemplateResetPasswordMail;
        this.kafkaTemplateTokenForUserPasswordReset = kafkaTemplateTokenForUserPasswordReset;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Attempting login for user: {}", loginRequest.getUsername());

        Optional<User> userOpt = firstUserRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            log.warn("Failed login attempt for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        String token = generateToken(user);

        log.info("Successful login for user: {}", user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional("firstTransactionManager")
    public User register(RegisterRequest registerRequest) {
        log.debug("Starting registration for user: {}", registerRequest.getUsername());

        // Validate unique constraints
        if (firstUserRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (firstUserRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create user entity
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setGroups(registerRequest.getGroups());

        User savedUser = firstUserRepository.save(user);
        log.info("User saved to database: {}", savedUser.getUsername());

        // Publish Kafka event
        publishUserEvent(savedUser, EventType.CREATE);

        // Sync with coffee service
        syncWithCoffeeService(savedUser,  "/api/coffee/user");

        return savedUser;
    }

    /**
     * Improved token validation that returns the associated email
     */
    public String validateResetTokenAndGetEmail(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Empty or null token provided");
                return null;
            }

            Optional<TokenForUserPasswordReset> tokenOpt = tokenForUserPasswordResetRepository
                    .findTokenForUserPasswordResetByToken(token);

            if (tokenOpt.isEmpty()) {
                log.warn("Token not found: {}", token);
                return null;
            }

            TokenForUserPasswordReset resetToken = tokenOpt.get();

            // Check if token is already used
            if (resetToken.getTokenStatus() == TokenStatus.USED) {
                log.warn("Token already used: {}", token);
                return null;
            }

            // Check if token is expired
            if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
                log.warn("Token has expired: {}", token);
                // Automatically mark expired tokens
                resetToken.setTokenStatus(TokenStatus.EXPIRED);
                tokenForUserPasswordResetRepository.save(resetToken);
                return null;
            }

            // Token is valid, return the associated email
            log.info("Token validated successfully for email: {}", resetToken.getEmail());
            return resetToken.getEmail();

        } catch (Exception e) {
            log.error("Error validating reset token", e);
            return null;
        }
    }

    public void sendEmailForResetPassword(String email) {
        log.debug("Processing password reset request for email: {}", email);

        if (!firstUserRepository.existsByEmail(email)) {
            log.warn("Password reset requested for non-existent email: {}", email);
            throw new RuntimeException("Email not found");
        }

        // Check daily limit
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentTokenCount = tokenForUserPasswordResetRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo);

        if (recentTokenCount >= LIMITER) {
            log.warn("Daily password reset limit exceeded for user: {}", email);
            throw new RuntimeException("You have exceeded the daily limit for password reset requests.");
        }

        // Create and save token
        TokenForUserPasswordReset tokenForUserPasswordReset = createTokenForUserPasswordReset(email);
        TokenForUserPasswordReset savedToken = tokenForUserPasswordResetRepository.save(tokenForUserPasswordReset);

        // Publish Kafka events
        publishTokenEvent(savedToken, EventType.CREATE);
        publishResetPasswordMailEvent(email, savedToken.getToken());

        log.info("Password reset process initiated for email: {}", email);
    }

    @Transactional("firstTransactionManager")
    public void resetPassword(ResetPasswordRequest resetPasswordRequest, String token) throws Exception {
        log.debug("Processing password reset for email: {}", resetPasswordRequest.getEmail());

        // Validate inputs
        validateResetPasswordRequest(resetPasswordRequest, token);

        // Get user and token
        User user = firstUserRepository.getByEmail(resetPasswordRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TokenForUserPasswordReset resetToken = tokenForUserPasswordResetRepository
                .findTokenForUserPasswordResetByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // Validate token
        validateResetToken(resetToken, resetPasswordRequest.getEmail());

        // Update password
        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getPassword());
        user.setPassword(encodedPassword);
        User updatedUser = firstUserRepository.save(user);

        // Mark token as used
        resetToken.setTokenStatus(TokenStatus.USED);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenForUserPasswordResetRepository.save(resetToken);

        // Publish Kafka events
        publishUserEvent(updatedUser, EventType.UPDATE);
        publishTokenEvent(resetToken, EventType.UPDATE);

        // Sync with coffee service
        //syncWithCoffeeService(updatedUser, "PUT", "/api/coffee/user/" + updatedUser.getId());

        log.info("Password successfully reset for user: {}", resetPasswordRequest.getEmail());
    }

    // Helper methods

    private void validateResetPasswordRequest(ResetPasswordRequest request, String token) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Token cannot be empty");
        }
    }

    private void validateResetToken(TokenForUserPasswordReset resetToken, String email) {
        // Verify token belongs to the email
        if (!resetToken.getEmail().equals(email)) {
            log.warn("Token email mismatch. Token email: {}, Request email: {}",
                    resetToken.getEmail(), email);
            throw new RuntimeException("Invalid token for this email");
        }

        // Check if token is already used
        if (resetToken.getTokenStatus() == TokenStatus.USED) {
            log.warn("Token already used: {}", resetToken.getToken());
            throw new RuntimeException("Token has already been used");
        }

        // Check if token is expired
        if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
            log.warn("Token has expired for email: {}", email);
            resetToken.setTokenStatus(TokenStatus.EXPIRED);
            tokenForUserPasswordResetRepository.save(resetToken);
            throw new RuntimeException("Token has expired");
        }
    }

    private TokenForUserPasswordReset createTokenForUserPasswordReset(String email) {
        String tokenHeader = "Paga_Tu_";
        String tokenTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_";
        String tokenBody = UUID.randomUUID().toString();
        String tokenFinal = "_Reset_Token_";
        String token = tokenHeader + tokenTimestamp + tokenBody + tokenFinal;
        TokenForUserPasswordReset tokenForUserPasswordReset = new TokenForUserPasswordReset();
        tokenForUserPasswordReset.setToken(token);
        tokenForUserPasswordReset.setEmail(email);
        tokenForUserPasswordReset.setCreatedAt(LocalDateTime.now());
        tokenForUserPasswordReset.setExpiredDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        tokenForUserPasswordReset.setTokenStatus(TokenStatus.ACTIVE);
        log.info("Created token active until: {}", tokenForUserPasswordReset.getExpiredDate());
        return tokenForUserPasswordReset;
    }

    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("id", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    private void publishUserEvent(User user, EventType eventType) {
        try {
            UserEvent userEvent = UserEvent.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .password(user.getPassword())
                    .groups(user.getGroups())
                    .eventType(eventType)
                    .build();

            kafkaTemplate.send(USER_TOPIC, String.valueOf(user.getId()), userEvent);
            log.debug("Published user event: {} for user: {}", eventType, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish user event", e);
        }
    }

    private void publishTokenEvent(TokenForUserPasswordReset token, EventType eventType) {
        try {
            TokenForgotPswUserEvent tokenEvent = new TokenForgotPswUserEvent();
            tokenEvent.setId(token.getId());
            tokenEvent.setToken(token.getToken());
            tokenEvent.setEmail(token.getEmail());
            tokenEvent.setCreatedAt(token.getCreatedAt());
            tokenEvent.setTokenStatus(token.getTokenStatus());
            tokenEvent.setExpiredDate(token.getExpiredDate());
            tokenEvent.setUsedAt(token.getUsedAt());
            tokenEvent.setEventType(eventType);

            kafkaTemplateTokenForUserPasswordReset.send(TOKEN_FORGOT_USER_PSW, tokenEvent);
            log.debug("Published token event: {} for email: {}", eventType, token.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish token event", e);
        }
    }

    private void publishResetPasswordMailEvent(String email, String token) {
        try {
            ResetPasswordMailEvent event = new ResetPasswordMailEvent();
            event.setEmail(email);
            event.setToken(token);

            kafkaTemplateResetPasswordMail.send(RESET_PASSWORD_TOPIC, event);
            log.debug("Published reset password mail event for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish reset password mail event", e);
        }
    }

    private void syncWithCoffeeService(User user, String endpoint) {

        UtenteDto utenteDto = new UtenteDto();
        utenteDto.setId(user.getId());
        utenteDto.setAuthId(user.getId());
        utenteDto.setUsername(user.getUsername());
        utenteDto.setEmail(user.getEmail());
        utenteDto.setName(user.getFirstName());
        utenteDto.setLastname(user.getLastName());
        utenteDto.setGroups(user.getGroups());

        WebClient.RequestBodySpec requestSpec;

        requestSpec = webClient.post().uri(endpoint);

//        if ("POST".equals(method)) {
//            requestSpec = webClient.post().uri(endpoint);
//        } else if ("PUT".equals(method)) {
//            requestSpec = webClient.put().uri(endpoint);
//        } else {
//            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
//        }

        requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(utenteDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    String errorMsg = String.format(
                                            "Coffee service error - Status: %s, Body: %s",
                                            response.statusCode(), errorBody);
                                    log.error(errorMsg);
                                    return Mono.error(new RuntimeException(errorMsg));
                                }))
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(x -> log.info("User successfully synchronized with coffee service"))
                .doOnError(WebClientResponseException.class,
                        error -> log.error("Coffee service HTTP error: {}", error.getMessage()))
                .doOnError(Exception.class,
                        error -> log.error("Error synchronizing with coffee service", error))
                .onErrorResume(Exception.class, error -> {
                    log.warn("Coffee service sync failed, continuing with main operation");
                    return Mono.empty();
                })
                .subscribe();
    }
}